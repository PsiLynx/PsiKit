// Copyright (c) 2021-2025 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit.core;

import static org.psilynx.psikit.core.Logger.LogLevel.CRITICAL;
import static org.psilynx.psikit.core.Logger.LogLevel.DEBUG;
import static org.psilynx.psikit.core.Logger.LogLevel.ERROR;
import static org.psilynx.psikit.core.Logger.LogLevel.INFO;
import static org.psilynx.psikit.core.Logger.LogLevel.WARNING;

import org.psilynx.psikit.core.mechanism.LoggedMechanism2d;
import org.psilynx.psikit.core.wpi.Struct;
import org.psilynx.psikit.core.wpi.StructSerializable;
import org.psilynx.psikit.core.wpi.WPISerializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/** Central class for recording and replaying log data. */
public class Logger {
  private static final int receiverQueueCapcity = 500; // 10s at 50Hz
  private static double startTime = 0.0;
  private static boolean running = false;
  private static long cycleCount = 0;
  private static LogTable entry = new LogTable(0);
  private static LogTable outputTable;
  private static Map<String, String> metadata = new HashMap<>();
  private static List<LoggedNetworkInput> dashboardInputs = new ArrayList<>();
  private static ConsoleSource console = null;
  private static LogReplaySource replaySource;
  private static final BlockingQueue<LogTable> receiverQueue =
      new ArrayBlockingQueue<>(receiverQueueCapcity);
  private static ReceiverThread receiverThread = new ReceiverThread(receiverQueue);
  private static boolean receiverQueueFault = false;
  private static DoubleSupplier timeSource =
          () -> System.nanoTime() / 1000000000.0 - startTime;
  private static boolean simulation = false;
  private static boolean replay = false;
  private static boolean enableConsole = true;
  private static LogLevel currentLogLevel = LogLevel.INFO;

  private Logger() {}

  public static void reset(){
    startTime = 0.0;
    running = false;
    cycleCount = 0;
    entry = new LogTable(0);
    outputTable = null;
    metadata = new HashMap<>();
    dashboardInputs = new ArrayList<>();
    console = null;
    replaySource = null;
    receiverQueue.clear();
    receiverThread = new ReceiverThread(receiverQueue);
    receiverQueueFault = false;
    DoubleSupplier timeSource =
            () -> System.nanoTime() / 1000000000.0 - startTime;
    simulation = false;
    replay = false;
    enableConsole = true;
  }

  /**
   * sets the source of real time on the robot. defualt is System.nanoTime()
   * @param newTimeSource a monotonic time source in seconds
   */
  public static void setTimeSource(DoubleSupplier newTimeSource){
    Logger.timeSource = newTimeSource;
  }
  /**
   * Sets the source to use for replaying data. Use null to disable replay. This method only works
   * during setup before starting to log.
   */
  public static void setReplaySource(LogReplaySource replaySource) {
    if (!running) {
      Logger.replaySource = replaySource;
    }
  }

  /**
   * Adds a new data receiver to process real or replayed data. This method only works during setup
   * before starting to log.
   */
  public static void addDataReceiver(LogDataReceiver dataReceiver) {
    if (!running) {
      receiverThread.addDataReceiver(dataReceiver);
    }
  }

  /**
   * Registers a new dashboard input to be included in the periodic loop. This function should not
   * be called by the user.
   */
  public static void registerDashboardInput(LoggedNetworkInput dashboardInput) {
    dashboardInputs.add(dashboardInput);
  }

  /**
   * Records a metadata value. This method only works during setup before starting to log, then data
   * will be recorded during the first cycle.
   *
   * @param key The name used to identify this metadata field.
   * @param value The value of the metadata field.
   */
  public static void recordMetadata(String key, String value) {
    if (!running) {
      metadata.put(key, value);
    }
  }

  /** Disables automatic console capture. */
  public static void disableConsoleCapture() {
    enableConsole = false;
  }

  /** Returns whether a replay source is currently being used. */
  public static boolean hasReplaySource() {
    return replaySource != null;
  }

  /** Starts running the logging system, including any data receivers or the replay source. */
  public static void start() {
    if (!running) {
      running = true;
      startTime = getTimestamp();

      // Start console capture
      if (enableConsole) {
          console = new ConsoleSourceImpl();
      }

      // Start replay source
      if (replaySource != null) {
        replaySource.start();
      }

      // Create output table
      if (replaySource == null) {
        outputTable = entry.getSubtable("RealOutputs");
      } else {
        outputTable = entry.getSubtable("ReplayOutputs");
      }

      // Record metadata
      LogTable metadataTable =
          entry.getSubtable(replaySource == null ? "RealMetadata" : "ReplayMetadata");
      for (Map.Entry<String, String> item : metadata.entrySet()) {
        metadataTable.put(item.getKey(), item.getValue());
      }

      // Start receiver thread
      receiverThread.start();

      //TODO: supposed to tell the robot to use this timestamp thing
      //RobotController.setTimeSource(Logger::getTimestamp);

      // Start first periodic cycle
      periodicBeforeUser();
    }
  }

  /** Ends the logging system, including any data receivers or the replay source. */
  public static void end() {
    if (running) {
      running = false;
      if (console != null) {
        try {
          console.close();
        } catch (Exception e) {
          Logger.logError("Failed to stop console capture.");
        }
      }
      if (replaySource != null) {
        replaySource.end();
      }

      // Stop the receiver thread and allow it to drain queued entries before ending receivers.
      receiverThread.interrupt();
      try {
        receiverThread.join();
      } catch (InterruptedException e) {
        Logger.logError(
          "error ending the receiver ("
          + receiverThread.getName() + ") thread\n"
          + Arrays.toString(e.getStackTrace())
        );
      }
      //TODO: supposed to tell the robot to use the normal time source
      //RobotController.setTimeSource(RobotController::getFPGATime);
    }
  }

  /**
   * Periodic method to be called during the constructor of Robot and each loop cycle. Updates
   * timestamp, replay entry, and dashboard inputs.
   */
  public static void periodicBeforeUser() {
    cycleCount++;
    if (running) {
      // Get next entry
      if (replaySource == null) {
        synchronized (entry) {
          entry.setTimestamp(timeSource.getAsDouble());
        }
      } else {
        if (!replaySource.updateTable(entry)) {
          Logger.logInfo(
            "logger received false from " +
            "replay source, ending"
          );
          end();
          return;
        }
      }

      // Update Driver Station
      double entryUpdateEnd = getTimestamp();

      // Record timing data
      //recordOutput(
              //"Logger/EntryUpdateMS",
              //(entryUpdateEnd - entryUpdateStart) * 1000.0
       //);
    }
  }

  /**
   * Periodic method to be called after the constructor of Robot and each loop cycle. Updates
   * default log values and sends data to data receivers. Running this after user code allows IO
   * operations to occur between cycles rather than interferring with the main thread.
   */
  public static void periodicAfterUser(double userCodeLength, double periodicBeforeLength) {
    if (running) {
      // Update automatic outputs from user code
      double autoLogStart = getTimestamp();
      AutoLogOutputManager.periodic();
      double autoLogEnd = getTimestamp();
      // Record timing data
      recordOutput("Logger/AutoLogMS", (autoLogEnd - autoLogStart) * 1000.0);
      recordOutput("LoggedRobot/UserCodeMS", userCodeLength * 1000.0);
      recordOutput(
          "LoggedRobot/LogPeriodicMS", (periodicBeforeLength) * 1000.0);
      recordOutput(
          "LoggedRobot/FullCycleMS",
          (periodicBeforeLength + userCodeLength) * 1000.0);
      recordOutput("Logger/QueuedCycles", receiverQueue.size());

      double consoleCaptureStart = getTimestamp();
      if (enableConsole) {
        String consoleData = console.getNewData();
        if (!consoleData.isEmpty()) {
          recordOutput("Console", consoleData.trim());
        }
      }
      double consoleCaptureEnd = getTimestamp();

      try {
        // Send a copy of the data to the receivers. The original object will be
        // kept and updated with the next timestamp (and new data if replaying).
        receiverQueue.add(LogTable.clone(entry));
        receiverQueueFault = false;
      } catch (IllegalStateException exception) {
        receiverQueueFault = true;
        Logger.logCritical(
            "[PsiKit] Capacity of receiver queue exceeded, data will NOT be logged."
        );
      }
    }
  }

  public static LogTable getEntry(){
    return entry;
  }

  /**
   * Returns the state of the receiver queue fault. This is tripped when the receiver queue fills
   * up, meaning that data is no longer being saved.
   */
  public static boolean getReceiverQueueFault() {
    return receiverQueueFault;
  }

  /**
   * Returns the current FPGA timestamp or replayed time based on the current log entry
   * (microseconds).
   */
  public static double getTimestamp() {
    synchronized (entry) {
      if (!running || entry == null) {
        return timeSource.getAsDouble();
      } else {
        return entry.getTimestamp();
      }
    }
  }

  /**
   * Returns the true timestamp in seconds, regardless of the timestamp
   * used for logging.
   * Useful for analyzing performance. DO NOT USE this method for any logic which might need to be
   * replayed.
   */
  public static double getRealTimestamp() {
    return getTimestamp();
  }

  /**
   * Runs the provided callback function every N loop cycles. This method can be used to update
   * inputs or log outputs at a lower rate than the standard loop cycle.
   *
   * <p><b>Note that this method must be called periodically to continue running the callback
   * function</b>.
   */
  public static void runEveryN(int n, Runnable function) {
    if (cycleCount % n == 0) {
      function.run();
    }
  }

  /**
   * Processes a set of inputs, logging them on the real robot or updating them in the simulator.
   * This should be called every loop cycle after updating the inputs from the hardware (if
   * applicable).
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name used to identify this set of inputs.
   * @param inputs The inputs to log or update.
   */
  public static void processInputs(String key, LoggableInputs inputs) {
    if (running) {
      if (replaySource == null) {
        inputs.toLog(entry.getSubtable(key));
      } else {
        inputs.fromLog(entry.getSubtable(key));
      }
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, byte[] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, byte[][] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, boolean value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, BooleanSupplier value) {
    if (running) {
      outputTable.put(key, value.getAsBoolean());
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, boolean[] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, boolean[][] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, int value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, IntSupplier value) {
    if (running) {
      outputTable.put(key, value.getAsInt());
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, int[] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, int[][] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, long value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, LongSupplier value) {
    if (running) {
      outputTable.put(key, value.getAsLong());
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, long[] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, long[][] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, float value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, float[] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, float[][] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, double value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, DoubleSupplier value) {
    if (running) {
      outputTable.put(key, value.getAsDouble());
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, double[] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, double[][] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, String value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, String[] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, String[][] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static <E extends Enum<E>> void recordOutput(String key, E value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static <E extends Enum<E>> void recordOutput(String key, E[] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static <E extends Enum<E>> void recordOutput(String key, E[][] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }
  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * <p>This method serializes a single object as a struct. Example usage: {@code
   * recordOutput("MyPose", Pose2d.struct, new Pose2d())}
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static <T> void recordOutput(String key, Struct<T> struct, T value) {
    if (running) {
      outputTable.put(key, struct, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method serializes an array of objects as a struct. Example usage: {@code
   * recordOutput("MyPoses", Pose2d.struct, new Pose2d(), new Pose2d()); recordOutput("MyPoses",
   * Pose2d.struct, new Pose2d[] {new Pose2d(), new Pose2d()}); }
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static <T> void recordOutput(String key, Struct<T> struct, T[] value) {
    if (running) {
      outputTable.put(key, struct, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static <T> void recordOutput(String key, Struct<T> struct, T[][] value) {
    if (running) {
      outputTable.put(key, struct, value);
    }
  }
  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method serializes a single object as a struct or protobuf automatically. Struct is
   * preferred if both methods are supported.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param <T> The type
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static <T extends WPISerializable> void recordOutput(String key, T value) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method serializes an array of objects as a struct automatically. Top-level protobuf
   * arrays are not supported.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param <T> The type
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  @SuppressWarnings("unchecked")
  public static <T extends StructSerializable> void recordOutput(
          String key,
          T[] value
  ) {
    if (running) {
      outputTable.put(key, value);
    }
  }

  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>This method serializes an array of objects as a struct automatically. Top-level protobuf
   * arrays are not supported.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param <T> The type
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static <T extends StructSerializable> void recordOutput(String key, T[][] value) {
    if (running) {
      outputTable.put(key, value);
    }
  }
  /**
   * Records a single output field for easy access when viewing the log. On the simulator, use this
   * method to record extra data based on the original inputs.
   *
   * <p>The current position of the Mechanism2d is logged once as a set of nested fields. If the
   * position is updated, this method must be called again.
   *
   * <p>This method is <b>not thread-safe</b> and should only be called from the main thread. See
   * the "Common Issues" page in the documentation for more details.
   *
   * @param key The name of the field to record. It will be stored under "/RealOutputs" or
   *     "/ReplayOutputs"
   * @param value The value of the field.
   */
  public static void recordOutput(String key, LoggedMechanism2d value) {
    if (running) {
      value.logOutput(outputTable.getSubtable(key));
    }
  }

  public static boolean isSimulation() {
      return simulation;
  }
  public static void setSimulation(boolean simulation) {
      Logger.simulation = simulation;
  }

  /** Returns whether the logging system has been started and is currently running. */
  public static boolean isRunning() {
    return running;
  }

  public static boolean isReplay() {
    return replay;
  }
  public static void setReplay(boolean replay) {
    Logger.replay = replay;
  }

  public static LogLevel getLogLevel() {
    return currentLogLevel;
  }

  public static void setLogLevel(LogLevel logLevel) {
    Logger.currentLogLevel = logLevel;
  }

  public static void logDebug(String message){
    if(currentLogLevel.ordinal() >= DEBUG.ordinal()){
      System.out.println("[PsiKit] DD: " + message);
    }
  }
  public static void logInfo(String message){
    if(currentLogLevel.ordinal() >= INFO.ordinal()){
      System.out.println("[PsiKit] II: " + message);
    }
  }
  public static void logWarning(String message){
    if(currentLogLevel.ordinal() >= WARNING.ordinal()){
      System.out.println("[PsiKit] WW: " + message);
    }
  }
  public static void logError(String message){
    if(currentLogLevel.ordinal() >= ERROR.ordinal()){
      System.out.println("[PsiKit] EE: " + message);
    }
  }
  public static void logCritical(String message){
    if(currentLogLevel.ordinal() >= CRITICAL.ordinal()){
      System.out.println("[PsiKit] CC: " + message);
    }
  }

  /**
   * log levels for debugging messages. <br>
   * - CRITICAL (CC) logs are exceptions that will most likely cause
   * significant portions of functionality to be unrecoverable <br>
   * - ERROR (EE) logs are exceptions that PsiKit can usually keep running after
   * encountering, although usually not perfectly <br>
   * - WARNING (WW) logs are events that are generally perfectly fine to have
   * occur, but can be symptoms of issues <br>
   * - INFO (II) logs let you know that something happened, like the
   * server connecting to a client. <br>
   * - DEBUG (DD) logs will produce a large amount of data meant for fixing
   * bugs. it will be much more than is necessary for the average user <br>
   */
  public enum LogLevel {
    CRITICAL, ERROR, WARNING, INFO, DEBUG
  }
}