package org.psilynx.psikit.core.rlog;

import org.psilynx.psikit.core.LogTable;
import org.psilynx.psikit.core.LogTable.LoggableType;
import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.core.Pair;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts byte array format to log tables. */
public class RLOGDecoder {
  /**
   * For the byte-level RLOG R2 on-disk format, see {@link RLOGEncoder}.
   */
  public static final String STRUCT_PREFIX = "struct:";
  public static final List<Byte> supportedLogRevisions = List.of((byte) 2);
  private int total = 0;

  private Byte logRevision = null;
  private LogTable table = new LogTable(0);
  private Map<Short, Pair<String, String>> keyIDs = new HashMap<>();
  private boolean eofReached = false;
  private Double bufferedNextTimestamp = null;

  public LogTable decodeTable(DataInputStream input) {
    try {
      if (eofReached) {
        return null;
      }
      if (logRevision == null) {
        this.total = input.available();
        logRevision = input.readByte();
        if (!supportedLogRevisions.contains(logRevision)) {
          Logger.logCritical(
            "Log revision "
            + (logRevision & 0xff)
            + " is not supported."
          );
          return null;
        }
      }

      // Each cycle begins with a timestamp record: [0][double timestamp]
      // Any subsequent [0][double] indicates the *next* cycle and terminates this one.
      double timestamp;
      if (bufferedNextTimestamp != null) {
        timestamp = bufferedNextTimestamp;
        bufferedNextTimestamp = null;
      } else {
        final byte tsType;
        try {
          tsType = input.readByte();
        } catch (EOFException e) {
          eofReached = true;
          return null;
        }
        if (tsType != 0) {
          Logger.logWarning(
            "Unexpected record type while reading timestamp: " + tsType + ". Ending replay."
          );
          eofReached = true;
          return null;
        }
        try {
          timestamp = input.readDouble();
        } catch (EOFException e) {
          eofReached = true;
          return null;
        }
      }

      Logger.logDebug("read bytes: " + (this.total - input.available()));
      table = new LogTable(timestamp, table);

      while (true) {
        final byte type;
        try {
          type = input.readByte();
        } catch (EOFException e) {
          Logger.logInfo("got EOF, ending read of input file");
          eofReached = true;
          return new LogTable(table.getTimestamp(), table);
        }

        Logger.logDebug("type: " + type);
        switch (type) {
          case 0: {
            // Start of next cycle.
            try {
              bufferedNextTimestamp = input.readDouble();
            } catch (EOFException e) {
              Logger.logInfo("got EOF while reading final timestamp");
              eofReached = true;
              bufferedNextTimestamp = null;
            }
            return new LogTable(table.getTimestamp(), table);
          }
          case 1:
            decodeKey(input);
            break;
          case 2:
            decodeValue(input);
            break;
          default:
            Logger.logWarning(
              "Unknown record type " + type + ". Ending replay to avoid desync."
            );
            eofReached = true;
            return null;
        }
      }

    } catch (IOException e) {
      Logger.logError(
        "problem reading file\n"
        + Arrays.toString(e.getStackTrace())
      );
      return null; // Problem decoding, might have been interrupted while writing this cycle
    }
  }

  private void decodeKey(DataInputStream input) throws IOException {
    short keyID = input.readShort();
    int keyLength = input.readUnsignedShort();
    String key = new String(
      input.readNBytes(keyLength),
      StandardCharsets.UTF_8
    );
    int typeLength = input.readUnsignedShort();
    String type = new String(
      input.readNBytes(typeLength),
      StandardCharsets.UTF_8
    );
    keyIDs.put(keyID, new Pair<>(key, type));
    Logger.logDebug("Key defined: ID=" + keyID + ", key=" + key + ", type=" + type);
  }

  private void decodeValue(DataInputStream input) throws IOException {
    Pair<String, String> keyID = keyIDs.get(input.readShort());
    int length = input.readUnsignedShort();
    Logger.logDebug("length of value: " + length);

    // Read exactly this record's payload to avoid desync across records.
    final byte[] payload = input.readNBytes(length);
    if (keyID == null) {
      // Unknown key ID, payload consumed.
      return;
    }
    String key = keyID.getFirst();
    String typeString = keyID.getSecond();
    LoggableType type = LoggableType.fromWPILOGType(typeString);

    final ByteBuffer buffer = ByteBuffer.wrap(payload);

    switch (type) {
      case Boolean:
        table.put(key, payload.length > 0 && payload[0] != 0);
        break;
      case Integer:
        try {
          table.put(key, buffer.getLong());
        } catch (BufferUnderflowException e) {
          Logger.logWarning("Truncated int64 payload for key \"" + key + "\"");
        }
        break;
      case Float:
        try {
          table.put(key, buffer.getFloat());
        } catch (BufferUnderflowException e) {
          Logger.logWarning("Truncated float payload for key \"" + key + "\"");
        }
        break;
      case Double:
        try {
          table.put(key, buffer.getDouble());
        } catch (BufferUnderflowException e) {
          Logger.logWarning("Truncated double payload for key \"" + key + "\"");
        }
        break;
      case String:
        table.put(key, new String(payload, StandardCharsets.UTF_8));
        break;
      case BooleanArray:
        boolean[] booleanArray = new boolean[payload.length];
        for (int i = 0; i < payload.length; i++) {
          booleanArray[i] = payload[i] != 0;
        }
        table.put(key, booleanArray);
        break;
      case IntegerArray:
        // WPILOG int64[] stores 8 bytes per element.
        long[] intArray = new long[payload.length / Long.BYTES];
        for (int i = 0; i < intArray.length; i++) {
          try {
            intArray[i] = buffer.getLong();
          } catch (BufferUnderflowException e) {
            Logger.logWarning("Truncated int64[] payload for key \"" + key + "\"");
            break;
          }
        }
        table.put(key, intArray);
        break;
      case FloatArray:
        // WPILOG float[] stores 4 bytes per element.
        float[] floatArray = new float[payload.length / Float.BYTES];
        for (int i = 0; i < floatArray.length; i++) {
          try {
            floatArray[i] = buffer.getFloat();
          } catch (BufferUnderflowException e) {
            Logger.logWarning("Truncated float[] payload for key \"" + key + "\"");
            break;
          }
        }
        table.put(key, floatArray);
        break;
      case DoubleArray:
        double[] doubleArray = new double[payload.length / Double.BYTES];
        for (int i = 0; i < doubleArray.length; i++) {
          try {
            doubleArray[i] = buffer.getDouble();
          } catch (BufferUnderflowException e) {
            Logger.logWarning("Truncated double[] payload for key \"" + key + "\"");
            break;
          }
        }
        table.put(key, doubleArray);
        break;
      case StringArray:
        try {
          int arrLength = buffer.getInt();
          String[] stringArray = new String[arrLength];
          for (int i = 0; i < arrLength; i++) {
            int stringLength = buffer.getInt();
            if (stringLength < 0 || stringLength > buffer.remaining()) {
              Logger.logWarning(
                "Invalid string length " + stringLength + " for key \"" + key + "\""
              );
              break;
            }
            byte[] strBytes = new byte[stringLength];
            buffer.get(strBytes);
            stringArray[i] = new String(strBytes, StandardCharsets.UTF_8);
          }
          table.put(key, stringArray);
        } catch (BufferUnderflowException e) {
          Logger.logWarning("Truncated string[] payload for key \"" + key + "\"");
        }
        break;
      default:
        // Raw / custom types (struct, structschema, etc) should be preserved.
        table.put(key, new LogTable.LogValue(payload, typeString));
        break;
    }
    try {
      Logger.logDebug("value: " + table.get(key).toString());
    } catch (Exception ignored){ }
  }
}
