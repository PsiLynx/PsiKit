// Copyright (c) 2021-2025 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ReceiverThread extends Thread {

  private final BlockingQueue<LogTable> queue;
  private List<LogDataReceiver> dataReceivers = new ArrayList<>();

  ReceiverThread(BlockingQueue<LogTable> queue) {
    super("PsiKit_LogReceiver");
    this.setDaemon(true);
    this.queue = queue;
  }

  void addDataReceiver(LogDataReceiver dataReceiver) {
    dataReceivers.add(dataReceiver);
  }

  List<LogDataReceiver> getReceivers(){
    return dataReceivers;
  }

  public void run() {
    // Start data receivers
    for (int i = 0; i < dataReceivers.size(); i++) {
      dataReceivers.get(i).start();
    }

    try {
      while (!isInterrupted()) {
        LogTable entry = queue.take(); // Wait for data

        // Send data to receivers
        for (int i = 0; i < dataReceivers.size(); i++) {
          dataReceivers.get(i).putTable(entry);
        }
      }
    } catch (InterruptedException ignored) {
      // Normal shutdown path.
    } finally {
      // Drain any remaining queued entries before ending receivers.
      LogTable entry;
      while ((entry = queue.poll()) != null) {
        for (int i = 0; i < dataReceivers.size(); i++) {
          try {
            dataReceivers.get(i).putTable(entry);
          } catch (InterruptedException ignored) {
            // Ignore; we're shutting down.
          }
        }
      }

      // End all data receivers
      for (int i = 0; i < dataReceivers.size(); i++) {
        dataReceivers.get(i).end();
      }
    }
  }
}
