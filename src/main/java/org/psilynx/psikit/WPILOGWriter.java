// Copyright (c) 2021-2025 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit;

import org.psilynx.psikit.wpi.DataLogWriter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Records log values to a WPILOG file. */
public class WPILOGWriter implements LogDataReceiver {
  private static final double timestampUpdateDelay =
      5.0; // Wait several seconds after DS attached to ensure
  // timestamp/timezone is updated
  private static final String defaultPathRio = "/U/logs";
  private static final String defaultPathSim = "logs";
  private static final SimpleDateFormat timeFormatter =
      new SimpleDateFormat("yy-MM-dd_HH-mm-ss");
  private static final String advantageScopeFileName = "ascope-log-path.txt";

  private String folder;
  private String filename;
  private final String randomIdentifier;
  private Double dsAttachedTime;

  private boolean autoRename;
  private String logDate;
  private String logMatchText;

  private DataLogWriter log;
  private boolean isOpen = false;
  private final AdvantageScopeOpenBehavior openBehavior;
  private LogTable lastTable;
  private int timestampID;
  private Map<String, Integer> entryIDs;
  private Map<String, LogTable.LoggableType> entryTypes;

  /**
   * Create a new WPILOGWriter for writing to a ".wpilog" file.
   *
   * @param path Path to log file or folder. If only a folder is provided, the filename will be
   *     generated based on the current time and match number (if applicable).
   * @param openBehavior Whether to automatically open the log file in AdvantageScope.
   */
  public WPILOGWriter(String path, AdvantageScopeOpenBehavior openBehavior) {
    this.openBehavior = openBehavior;

    // Create random identifier
    Random random = new Random();
    StringBuilder randomIdentifierBuilder = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      randomIdentifierBuilder.append(String.format("%04x", random.nextInt(0x10000)));
    }
    randomIdentifier = randomIdentifierBuilder.toString();

    // Set up folder and filename
    if (path.endsWith(".wpilog")) {
      File pathFile = new File(path);
      folder = pathFile.getParent();
      filename = pathFile.getName();
      autoRename = false;
    } else {
      folder = path;
      filename = "akit_" + randomIdentifier + ".wpilog";
      autoRename = true;
    }
  }

  /**
   * Create a new WPILOGWriter for writing to a ".wpilog" file.
   *
   * @param path Path to log file or folder. If only a folder is provided, the filename will be
   *     generated based on the current time and match number (if applicable).
   */
  public WPILOGWriter(String path) {
    this(path, AdvantageScopeOpenBehavior.AUTO);
  }

  /**
   * Create a new WPILOGWriter for writing to a ".wpilog" file.
   *
   * <p>The logs will be saved to "/U/logs" on the RIO and "logs" in sim. The filename will be
   * generated based on the current time and match number (if applicable).
   *
   * @param openBehavior Whether to automatically open the log file in AdvantageScope.
   */
  public WPILOGWriter(AdvantageScopeOpenBehavior openBehavior) {
    this(Logger.isSimulation() ? defaultPathSim : defaultPathRio, openBehavior);
  }

  /**
   * Create a new WPILOGWriter for writing to a ".wpilog" file.
   *
   * <p>The logs will be saved to "/U/logs" on the RIO and "logs" in sim. The filename will be
   * generated based on the current time and match number (if applicable).
   */
  public WPILOGWriter() {
    this(
        Logger.isSimulation() ? defaultPathSim : defaultPathRio,
        AdvantageScopeOpenBehavior.AUTO);
  }

  public void start() {
    // Create folder if necessary
    File logFolder = new File(folder);
    if (!logFolder.exists()) {
      logFolder.mkdirs();
    }

    // Delete log if it already exists
    File logFile = new File(folder, filename);
    if (logFile.exists()) {
      logFile.delete();
    }

    // Create new log
      String logPath = null;
      try {
          logPath = new File(folder, filename).getCanonicalPath();
      } catch (IOException e) {
          System.out.println("[AdvantageKit] io error getting log path");
      }
      System.out.println("[AdvantageKit] Logging to \"" + logPath + "\"");
    try {
      log = new DataLogWriter(logPath, WPILOGConstants.extraHeader);
    } catch (IOException e) {
      System.out.println("[AdvantageKit] Failed to open output log file.");
      return;
    }
    isOpen = true;
    timestampID =
        log.start(
            timestampKey, LogTable.LoggableType.Integer.getWPILOGType(),
                WPILOGConstants.entryMetadata, 0);
    lastTable = new LogTable(0);

    // Reset data
    entryIDs = new HashMap<>();
    entryTypes = new HashMap<>();
    logDate = null;
    logMatchText = null;
  }

  public void end() {
    log.close();

    // Send log path to AdvantageScope
    boolean shouldOpen;
    switch (openBehavior) {
      case ALWAYS:
        shouldOpen = Logger.isSimulation();
        break;
      case AUTO:
        shouldOpen = Logger.isSimulation() && Logger.hasReplaySource();
        break;
      case NEVER:
      default:
        shouldOpen = false;
        break;
    }

    if (shouldOpen) {
      try {
        File logFile = new File(folder, filename);
        String fullLogPath = logFile.getCanonicalPath();

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File advantageScopeTempFile = new File(tempDir, advantageScopeFileName);

        PrintWriter writer = new PrintWriter(advantageScopeTempFile, "UTF-8");
        try {
          writer.println(fullLogPath);
          System.out.println("[AdvantageKit] Log sent to AdvantageScope.");
        } finally {
          writer.close();
        }

      } catch (Exception e) {
        System.out.println("[AdvantageKit] Failed to send log to AdvantageScope.");
      }
    }
  }

  public void putTable(LogTable table) {
    // Exit if log not open
    if (!isOpen) return;

    // Auto rename
    if (autoRename) {

      // Update timestamp
      if (logDate == null) {
        if ((table.get("DriverStation/DSAttached", false)
                && table.get("SystemStats/SystemTimeValid", false))
            || Logger.isSimulation()) {
          if (dsAttachedTime == null) {
            dsAttachedTime = Logger.getTimestamp();
          } else if (Logger.getTimestamp() - dsAttachedTime
                  > timestampUpdateDelay
              || Logger.isSimulation()) {
            logDate = timeFormatter.format(new Date());
          }
        } else {
          dsAttachedTime = null;
        }
      }

      // Update filename
      StringBuilder newFilenameBuilder = new StringBuilder();
      newFilenameBuilder.append("akit_");
      if (logDate == null) {
        newFilenameBuilder.append(randomIdentifier);
      } else {
        newFilenameBuilder.append(logDate);
      }
      String eventName = table.get("DriverStation/EventName", "").toLowerCase();
      if (eventName.length() > 0) {
        newFilenameBuilder.append("_");
        newFilenameBuilder.append(eventName);
      }
      if (logMatchText != null) {
        newFilenameBuilder.append("_");
        newFilenameBuilder.append(logMatchText);
      }
      newFilenameBuilder.append(".wpilog");
      String newFilename = newFilenameBuilder.toString();
      if (!newFilename.equals(filename)) {
          String logPath = null;
          try {
              logPath = new File(folder, newFilename).getCanonicalPath();
          } catch (IOException e) {
              System.out.println("[AdvantageKit] error getting file name");
          }
          System.out.println("[AdvantageKit] Renaming log to \"" + logPath + "\"");

        File fileA = new File(folder, filename);
        File fileB = new File(folder, newFilename);
        fileA.renameTo(fileB);
        filename = newFilename;
      }
    }

    // Save timestamp
    log.appendInteger(timestampID, table.getTimestamp(), table.getTimestamp());

    // Get new and old data
    Map<String, LogTable.LogValue> newMap = table.getAll(false);
    Map<String, LogTable.LogValue> oldMap = lastTable.getAll(false);

    // Encode fields
    for (Map.Entry<String, LogTable.LogValue> field : newMap.entrySet()) {

      // Check if field should be updated
      LogTable.LoggableType type = field.getValue().type;
      boolean appendData = false;
      if (!entryIDs.containsKey(field.getKey())) { // New field
        entryIDs.put(
            field.getKey(),
            log.start(
                field.getKey(),
                field.getValue().getWPILOGType(),
                WPILOGConstants.entryMetadata,
                table.getTimestamp()));
        entryTypes.put(field.getKey(), type);
        appendData = true;
      } else if (!field.getValue().equals(oldMap.get(field.getKey()))) { // Updated field
        appendData = true;
      }

      // Append data
      if (appendData) {
        int id = entryIDs.get(field.getKey());
        switch (field.getValue().type) {
          case Raw:
            log.appendRaw(id, field.getValue().getRaw(), table.getTimestamp());
            break;
          case Boolean:
            log.appendBoolean(id, field.getValue().getBoolean(), table.getTimestamp());
            break;
          case Integer:
            log.appendInteger(id, field.getValue().getInteger(), table.getTimestamp());
            break;
          case Float:
            log.appendFloat(id, field.getValue().getFloat(), table.getTimestamp());
            break;
          case Double:
            log.appendDouble(id, field.getValue().getDouble(), table.getTimestamp());
            break;
          case String:
            log.appendString(id, field.getValue().getString(), table.getTimestamp());
            break;
          case BooleanArray:
            log.appendBooleanArray(id, field.getValue().getBooleanArray(), table.getTimestamp());
            break;
          case IntegerArray:
            log.appendIntegerArray(id, field.getValue().getIntegerArray(), table.getTimestamp());
            break;
          case FloatArray:
            log.appendFloatArray(id, field.getValue().getFloatArray(), table.getTimestamp());
            break;
          case DoubleArray:
            log.appendDoubleArray(id, field.getValue().getDoubleArray(), table.getTimestamp());
            break;
          case StringArray:
            log.appendStringArray(id, field.getValue().getStringArray(), table.getTimestamp());
            break;
        }
      }
    }

    // Flush to disk
    log.flush();

    // Update last table
    lastTable = table;
  }

  public static enum AdvantageScopeOpenBehavior {
    /** Always open the log file in AdvantageScope when running in sim. */
    ALWAYS,

    /** Open the log file in AdvantageScope when running in replay. */
    AUTO,

    /** Never open the log file in AdvantageScope */
    NEVER
  }
}
