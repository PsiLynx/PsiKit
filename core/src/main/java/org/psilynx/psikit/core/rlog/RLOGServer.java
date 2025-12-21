// Copyright (c) 2021-2025 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit.core.rlog;

import org.psilynx.psikit.core.LogDataReceiver;
import org.psilynx.psikit.core.LogTable;
import org.psilynx.psikit.core.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/** Sends log data over a socket connection using the RLOG format. */
public class RLOGServer implements LogDataReceiver {
  private final int port;
  private ServerThread thread;
  private final RLOGEncoder encoder = new RLOGEncoder();

  private static final byte[] KEEPALIVE_BYTES = new byte[4];

  private final Object encoderLock = new Object();
  private final Object socketsLock = new Object();

  public RLOGServer() {
    this(5800);
  }

  public RLOGServer(int port) {
    this.port = port;
  }

  public void start() {
    if (thread != null) {
      return;
    }

    final ServerThread t = new ServerThread(port);
    if (!t.isBound()) {
      Logger.logError("RLOG server failed to start (requested port " + port + ")");
      return;
    }

    thread = t;
    thread.start();
    Logger.logInfo("RLOG server started on port " + port);
  }

  public void end() {
    // Avoid TOCTOU races with putTable(): grab a local reference first.
    final ServerThread t = thread;
    if (t != null) {
      t.close();
      thread = null;
    }
  }

  public void putTable(LogTable table) throws InterruptedException {
    // Avoid TOCTOU races: Logger thread can call putTable() while another thread calls end().
    final ServerThread t = thread;
    if (t == null) {
      return;
    }

    // If broadcast is behind, drop this cycle and encode changes in the next cycle.
    // Use offer() (non-blocking) so shutdown can't hang waiting on the queue.
    byte[] data;
    synchronized (encoderLock) {
      encoder.encodeTable(table, false);
      data = encodeData(encoder.getOutput().array());
    }
    t.broadcastQueue.offer(data);
  }

  private byte[] encodeData(byte[] data) {
    byte[] lengthBytes = ByteBuffer.allocate(Integer.BYTES).putInt(data.length).array();
    byte[] fullData = new byte[lengthBytes.length + data.length];
    System.arraycopy(lengthBytes, 0, fullData, 0, lengthBytes.length);
    System.arraycopy(data, 0, fullData, lengthBytes.length, data.length);
    return fullData;
  }

  private class ServerThread extends Thread {
    private static final double heartbeatTimeoutSecs =
        3.0; // Close connection if heartbeat not received for this
    // length

    ServerSocket server;
    Thread broadcastThread;

    ArrayBlockingQueue<byte[]> broadcastQueue = new ArrayBlockingQueue<>(500);
    List<Socket> sockets = new ArrayList<>();
    List<Double> lastHeartbeats = new ArrayList<>();

    public ServerThread(int port) {
      super("PsiKit_RLOGServer");
      this.setDaemon(true);

      try {
        final ServerSocket s = new ServerSocket();
        s.setReuseAddress(true);
        s.bind(new InetSocketAddress(port));
        server = s;
      } catch (IOException e) {
        Logger.logError(
            "error while opening a socket in RLOG server on port "
                + port
                + ": "
                + e.getClass().getSimpleName()
                + ": "
                + e.getMessage());
        server = null;
      }
    }

    public boolean isBound() {
      return server != null;
    }

    public void run() {
      if (server == null) {
        return;
      }

      // Start broadcast thread
      broadcastThread = new Thread(this::runBroadcast);
      broadcastThread.setName("PsiKit_RLOGServerBroadcast");
      broadcastThread.setDaemon(true);
      broadcastThread.start();

      // Wait for clients
      while (!isInterrupted()) {
        try {
          // Avoid TOCTOU races with close(): read a local ServerSocket reference.
          final ServerSocket s = server;
          if (s == null || s.isClosed()) {
            return;
          }

          Socket socket = s.accept();
          byte[] data;
          synchronized (encoderLock) {
            data = encodeData(encoder.getNewcomerData().array());
          }
          OutputStream out = socket.getOutputStream();
          out.write(data);
          out.flush();
          synchronized (socketsLock) {
            sockets.add(socket);
            lastHeartbeats.add(System.nanoTime() / 1000000000.0);
          }
          Logger.logInfo(
            "Connected to RLOG client - "
            + socket.getInetAddress().getHostAddress()
          );
        } catch (IOException e) {
          // Normal shutdown path: accept() throws when ServerSocket is closed.
          final ServerSocket s = server;
          if (isInterrupted() || s == null || s.isClosed()) {
            return;
          }
          Logger.logError(
              "rlog server threw an exception: "
                  + e.getClass().getSimpleName()
                  + ": "
                  + e.getMessage());
        }
      }
    }

    public void runBroadcast() {
      while (!isInterrupted()) {
        try {
          Thread.sleep(20);
        } catch (InterruptedException e) {
          return;
        }

        // Get queue data
        List<byte[]> broadcastData = new ArrayList<>();
        broadcastQueue.drainTo(broadcastData);

        // Broadcast to each client
        synchronized (socketsLock) {
          for (int i = sockets.size() - 1; i >= 0; i--) {
            Socket socket = sockets.get(i);
            if (socket.isClosed()) {
              sockets.remove(i);
              lastHeartbeats.remove(i);
              continue;
            }

            try {
              // Read heartbeat
              InputStream inputStream = socket.getInputStream();
              if (inputStream.available() > 0) {
                inputStream.skip(inputStream.available());
                lastHeartbeats.set(i, System.nanoTime() / 1000000000.0);
              }

              // Close connection if socket timed out
              if (System.nanoTime() / 1000000000.0 - lastHeartbeats.get(i)
                  > heartbeatTimeoutSecs) {
                socket.close();
                printDisconnectMessage(socket, "timeout");
                sockets.remove(i);
                lastHeartbeats.remove(i);
                continue;
              }

              // Send message to stay alive
              OutputStream outputStream = socket.getOutputStream();
              outputStream.write(KEEPALIVE_BYTES);

              // Send broadcast data
              for (byte[] data : broadcastData) {
                outputStream.write(data);
              }

              outputStream.flush();
            } catch (IOException e) {
              try {
                socket.close();
                printDisconnectMessage(socket, "IOException");
              } catch (IOException ignored) {
              }

              sockets.remove(i);
              lastHeartbeats.remove(i);
            }
          }
        }
      }
    }

    private void printDisconnectMessage(Socket socket, String reason) {
      Logger.logInfo(
          "Disconnected from RLOG client ("
              + reason
              + ") - "
              + socket.getInetAddress().getHostAddress());
    }

    public void close() {
      if (server != null) {
        try {
          server.close();
          server = null;
        } catch (IOException e) {
          Logger.logError(
              "rlog server could not be closed while shutting down: "
                  + e.getClass().getSimpleName()
                  + ": "
                  + e.getMessage());
        }
      }

      synchronized (socketsLock) {
        for (Socket socket : sockets) {
          try {
            socket.close();
          } catch (IOException ignored) {
          }
        }
        sockets.clear();
        lastHeartbeats.clear();
      }

      if (broadcastThread != null) {
        broadcastThread.interrupt();
      }
      this.interrupt();
    }
  }
}
