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

import java.io.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

/** Sends log data over a socket connection using the RLOG format. */
public class RLOGWriter implements LogDataReceiver {
  private RLOGEncoder encoder = new RLOGEncoder();
  private static final Object encoderLock = new Object();
  private final String filePath;
  private FileOutputStream fileOutputStream = null;
  private double lastTimestamp = 0.0;

  public RLOGWriter(String fileName){
    this(
            "/sdcard/FIRST/",
            fileName
    );
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
    Logger.logInfo("RLOG writer started");
    File file = new File(filePath);
    file.delete();
    try {
      file.createNewFile();
      fileOutputStream = new FileOutputStream(filePath, true);
    } catch (IOException e) {
      Logger.logError(
        "error opening log file\n"
        + Arrays.toString(e.getStackTrace())
      );
    }
  }

  public void putTable(LogTable table) {
    if(table.getTimestamp() - lastTimestamp > 0.0001) {
      lastTimestamp = table.getTimestamp();
      byte[] data;
      synchronized (encoderLock) {
        encoder.encodeTable(table, true);
        data = encoder.getOutput().array();
      }
      appendData(data);
    }
  }

  private void appendData(byte[] data) {
    try {
      if(fileOutputStream == null){
        Logger.logError(
          "must start RLOGWriter before using append data"
        );
      } else fileOutputStream.write(data);
    }
    catch (IOException e){
      Logger.logError(
        "error opening file \""
        + filePath
        + "\" for writing in the RLOG writer thread\n"
        + Arrays.toString(e.getStackTrace())
      );
    }

  }
}
