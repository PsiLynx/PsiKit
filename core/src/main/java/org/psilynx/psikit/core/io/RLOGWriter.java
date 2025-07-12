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
  private RLOGEncoder encoder = new RLOGEncoder();
  private static final Object encoderLock = new Object();
  private final String filePath;
  private FileOutputStream fileOutputStream = null;

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
    if(!folder.endsWith("/")){
      folder = folder + "/";
    }
    if(!fileName.endsWith(".rlog")){
      fileName = fileName + ".rlog";
    }

    this.filePath = folder + fileName;
  }

  public void start() {
    System.out.println("[PsiKit] RLOG writer started");
    File file = new File(filePath);
    file.delete();
    try {
      file.createNewFile();
      fileOutputStream = new FileOutputStream(filePath, true);
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("[PsiKit] error creating log file");
    }
  }

  public void putTable(LogTable table) {
    byte[] data;
    synchronized (encoderLock) {
      encoder.encodeTable(table, true);
      data = encoder.getOutput().array();
    }
    appendData(data);
  }

  private void appendData(byte[] data) {
    try {
      if(fileOutputStream == null){
        System.out.println(
          "[PsiKit] must start RLOGWriter before using append data"
        );
      } else fileOutputStream.write(data);
    }
    catch (IOException e){
      e.printStackTrace();
      System.out.println(
        "[PsiKit] error opening file \""
        + filePath
        + "\" for writing in the RLOG writer thread"
      );
    }

  }
}
