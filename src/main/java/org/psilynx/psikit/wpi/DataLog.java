// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.psilynx.psikit.wpi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A data log for high-speed writing of data values.
 *
 * <p>The finish() function is needed only to indicate in the log that a particular entry is no
 * longer being used (it releases the name to ID mapping). The finish() function is not required to
 * be called for data to be flushed to disk; entries in the log are written as append() calls are
 * being made. In fact, finish() does not need to be called at all.
 *
 * <p>DataLog calls are thread safe. DataLog uses a typical multiple-supplier, single-consumer
 * setup. Writes to the log are atomic, but there is no guaranteed order in the log when multiple
 * threads are writing to it; whichever thread grabs the write mutex first will get written first.
 * For this reason (as well as the fact that timestamps can be set to arbitrary values), records in
 * the log are not guaranteed to be sorted by timestamp.
 */
public class DataLog implements AutoCloseable {
  /**
   * Constructs.
   *
   * @param impl implementation handle
   */
  protected DataLog(long impl) {
    m_impl = impl;
  }

  /** Explicitly flushes the log data to disk. */
  public void flush() {
    DataLogJNI.flush(m_impl);
  }

  /**
   * Start an entry. Duplicate names are allowed (with the same type), and result in the same index
   * being returned (start/finish are reference counted). A duplicate name with a different type
   * will result in an error message being printed to the console and 0 being returned (which will
   * be ignored by the append functions).
   *
   * @param name Name
   * @param type Data type
   * @param metadata Initial metadata (e.g. data properties)
   * @param timestamp Time stamp (0 to indicate now)
   * @return Entry index
   */
  public int start(String name, String type, String metadata, long timestamp) {
    return DataLogJNI.start(m_impl, name, type, metadata, timestamp);
  }

  /**
   * Start an entry. Duplicate names are allowed (with the same type), and result in the same index
   * being returned (start/finish are reference counted). A duplicate name with a different type
   * will result in an error message being printed to the console and 0 being returned (which will
   * be ignored by the append functions).
   *
   * @param name Name
   * @param type Data type
   * @param metadata Initial metadata (e.g. data properties)
   * @return Entry index
   */
  public int start(String name, String type, String metadata) {
    return start(name, type, metadata, 0);
  }

  /**
   * Start an entry. Duplicate names are allowed (with the same type), and result in the same index
   * being returned (start/finish are reference counted). A duplicate name with a different type
   * will result in an error message being printed to the console and 0 being returned (which will
   * be ignored by the append functions).
   *
   * @param name Name
   * @param type Data type
   * @return Entry index
   */
  public int start(String name, String type) {
    return start(name, type, "");
  }

  @Override
  public void close() {
    DataLogJNI.close(m_impl);
    m_impl = 0;
  }

  /**
   * Appends a raw record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param data Byte array to record; will send entire array contents
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendRaw(int entry, byte[] data, long timestamp) {
    appendRaw(entry, data, 0, data.length, timestamp);
  }

  /**
   * Appends a record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param data Byte array to record
   * @param start Start position of data (in byte array)
   * @param len Length of data (must be less than or equal to data.length - start)
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendRaw(int entry, byte[] data, int start, int len, long timestamp) {
    DataLogJNI.appendRaw(m_impl, entry, data, start, len, timestamp);
  }

  /**
   * Appends a boolean record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param value Boolean value to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendBoolean(int entry, boolean value, long timestamp) {
    DataLogJNI.appendBoolean(m_impl, entry, value, timestamp);
  }

  /**
   * Appends an integer record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param value Integer value to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendInteger(int entry, long value, long timestamp) {
    DataLogJNI.appendInteger(m_impl, entry, value, timestamp);
  }

  /**
   * Appends a float record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param value Float value to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendFloat(int entry, float value, long timestamp) {
    DataLogJNI.appendFloat(m_impl, entry, value, timestamp);
  }

  /**
   * Appends a double record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param value Double value to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendDouble(int entry, double value, long timestamp) {
    DataLogJNI.appendDouble(m_impl, entry, value, timestamp);
  }

  /**
   * Appends a string record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param value String value to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendString(int entry, String value, long timestamp) {
    DataLogJNI.appendString(m_impl, entry, value, timestamp);
  }

  /**
   * Appends a boolean array record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param arr Boolean array to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendBooleanArray(int entry, boolean[] arr, long timestamp) {
    DataLogJNI.appendBooleanArray(m_impl, entry, arr, timestamp);
  }

  /**
   * Appends an integer array record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param arr Integer array to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendIntegerArray(int entry, long[] arr, long timestamp) {
    DataLogJNI.appendIntegerArray(m_impl, entry, arr, timestamp);
  }

  /**
   * Appends a float array record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param arr Float array to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendFloatArray(int entry, float[] arr, long timestamp) {
    DataLogJNI.appendFloatArray(m_impl, entry, arr, timestamp);
  }

  /**
   * Appends a double array record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param arr Double array to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendDoubleArray(int entry, double[] arr, long timestamp) {
    DataLogJNI.appendDoubleArray(m_impl, entry, arr, timestamp);
  }

  /**
   * Appends a string array record to the log.
   *
   * @param entry Entry index, as returned by start()
   * @param arr String array to record
   * @param timestamp Time stamp (0 to indicate now)
   */
  public void appendStringArray(int entry, String[] arr, long timestamp) {
    DataLogJNI.appendStringArray(m_impl, entry, arr, timestamp);
  }

  /**
   * Gets the JNI implementation handle.
   *
   * @return data log handle.
   */
  public long getImpl() {
    return m_impl;
  }

  /** Implementation handle. */
  protected long m_impl;

  private final ConcurrentMap<String, Integer> m_schemaMap = new ConcurrentHashMap<>();
}
