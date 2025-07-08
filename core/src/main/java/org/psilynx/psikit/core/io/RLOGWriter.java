// Copyright (c) 2021-2025 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit.core.io;

import org.psilynx.psikit.core.LogTable;

import java.io.*;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

/** Sends log data over a socket connection using the RLOG format. */
public class RLOGWriter implements LogDataReceiver {
  private WriterThread thread;
  private RLOGEncoder encoder = new RLOGEncoder();

  private static Object encoderLock = new Object();

  public RLOGWriter(){
    this(
            "/sdcard/FIRST/userLogs/",
            Date.from(Instant.now()).toString()
    );
  }
  public RLOGWriter(String fileName){
    this("/sdcard/FIRST/userLogs/", fileName);
  }
  public RLOGWriter(String folder, String fileName){
    thread = new WriterThread(folder, fileName);
  }

  public void start() {
    thread.start();
    System.out.println("[PsiKit] RLOG writer started");
  }

  public void end() {
    if (thread != null) {
      thread.close();
      thread = null;
    }
  }

  public void putTable(LogTable table) throws InterruptedException {
    if (thread != null && thread.broadcastQueue.remainingCapacity() > 0) {
      // If broadcast is behind, drop this cycle and encode changes in the next cycle
      byte[] data;
      synchronized (encoderLock) {
        encoder.encodeTable(table, true);
        data = encoder.getOutput().array();
      }
      thread.broadcastQueue.put(data);
    }
  }

  private class WriterThread extends Thread {
    ArrayBlockingQueue<byte[]> broadcastQueue = new ArrayBlockingQueue<>(500);
    private FileOutputStream fileOutputStream;
    private String filePath;

    public WriterThread(String folder, String fileName) {
      super("PsiKit_RLOGWriter");
      this.setDaemon(true);

      if(!folder.endsWith("/")){
        folder = folder + "/";
      }
      if(!fileName.endsWith(".rlog")){
        fileName = fileName + ".rlog";
      }

      this.filePath = folder + fileName;

    }

    public void run() {
      File file = new File(filePath);
      file.delete();
      try {
        file.createNewFile();
        fileOutputStream = new FileOutputStream(filePath, true);
      }
      catch (IOException e){
        e.printStackTrace();
        System.out.println(
          "[PsiKit] error opening file \""
          + this.filePath
          + "\" for writing in the RLOG writer thread"
        );
        fileOutputStream = null;
      }

      while (true) {
        try {
          byte[] data;
          //if( !this.broadcastQueue.isEmpty() ) {
            data = this.broadcastQueue.take();
            this.fileOutputStream.write(data);
          //}
        } catch (IOException | InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    public void close() {
      this.interrupt();
    }
  }
}
