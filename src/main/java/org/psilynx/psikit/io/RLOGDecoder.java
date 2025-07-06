package org.psilynx.psikit.io;

import org.psilynx.psikit.LogTable;
import org.psilynx.psikit.LogTable.LoggableType;
import org.psilynx.psikit.Pair;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts byte array format to log tables. */
public class RLOGDecoder {
  public static final String STRUCT_PREFIX = "struct:";
  public static final List<Byte> supportedLogRevisions = List.of((byte) 2);

  private Byte logRevision = null;
  private LogTable table = new LogTable(0);
  private Map<Short, Pair<String, String>> keyIDs = new HashMap<>();

  public LogTable decodeTable(DataInputStream input) {
    readTable: try {
      if (logRevision == null) {
        logRevision = input.readByte();
        if (!supportedLogRevisions.contains(logRevision)) {
          System.out.println(
            "Log revision "
            + (logRevision & 0xff)
            + " is not supported."
          );
          return null;
        }
        input.skip(1); // Second byte specifies timestamp type, this will be assumed
      }
      if (input.available() == 0) {
        return null; // No more data, so we can't start a new table
      }
      table = new LogTable(decodeTimestamp(input), table);

      readLoop: while (true) {
        if (input.available() == 0) {
          break readTable; // This was the last cycle, return the data
        }

        byte type = input.readByte();
        switch (type) {
          case 0: // Next timestamp
            break readLoop;
          case 1: // New key ID
            decodeKey(input);
            break;
          case 2: // Updated field
            decodeValue(input);
            break;
        }
      }

    } catch (IOException e) {
      return null; // Problem decoding, might have been interrupted while writing this cycle
    }

    return new LogTable(table.getTimestamp(), table);
  }

  private double decodeTimestamp(DataInputStream input) throws IOException {
    return input.readDouble();
  }

  private void decodeKey(DataInputStream input) throws IOException {
    short keyID = input.readShort();
    short keyLength = input.readShort();
    String key = new String(
      input.readNBytes(keyLength),
      StandardCharsets.UTF_8
    );
    short typeLength = input.readShort();
    String type = new String(
      input.readNBytes(typeLength),
      StandardCharsets.UTF_8
    );
    keyIDs.put(keyID, new Pair<>(key, type));
  }

  private void decodeValue(DataInputStream input) throws IOException {
    Pair<String, String> keyID = keyIDs.get(input.readShort());
    short length = input.readShort();
    String key = keyID.getFirst();
    String typeString = keyID.getSecond();
    LoggableType type = LoggableType.fromWPILOGType(typeString);

    //LoggableType type = LoggableType.values()[typeInt - 1];

    switch (type) {
      case Boolean:
        table.put(key, input.readBoolean());
        break;
      case Integer:
        table.put(key, input.readInt());
        break;
      case Double:
        table.put(key, input.readDouble());
        break;
      case String:
        length = input.readShort();
        table.put(key, new String(input.readNBytes(length), "UTF-8"));
        break;
      case BooleanArray:
        length = input.readShort();
        boolean[] booleanArray = new boolean[length];
        for (int i = 0; i < length; i++) {
          booleanArray[i] = input.readBoolean();
        }
        table.put(key, booleanArray);
        break;
      case IntegerArray:
        length = input.readShort();
        int[] intArray = new int[length];
        for (int i = 0; i < length; i++) {
          intArray[i] = input.readInt();
        }
        table.put(key, intArray);
        break;
      case DoubleArray:
        length = input.readShort();
        double[] doubleArray = new double[length];
        for (int i = 0; i < length; i++) {
          doubleArray[i] = input.readDouble();
        }
        table.put(key, doubleArray);
        break;
      case StringArray:
        length = input.readShort();
        String[] stringArray = new String[length];
        for (int i = 0; i < length; i++) {
          short stringLength = input.readShort();
          stringArray[i] = new String(input.readNBytes(stringLength), "UTF-8");
        }
        table.put(key, stringArray);
        break;
      default:

        if (typeString.startsWith(STRUCT_PREFIX)) {
          String schemaType = typeString.substring(STRUCT_PREFIX.length());
          boolean isArray = schemaType.endsWith("[]");
          String baseType = isArray ? schemaType.substring(0, schemaType.length() - 2) : schemaType;
          byte[] value = input.readNBytes(length);
          table.put(key, value);
        }
        else if (typeString.startsWith("photon:")) {
          System.out.println("[Psikit] photon not supported in decoder");
        }
        else if (typeString.startsWith("proto:")) {
          System.out.println("[Psikit] proto not supported in decoder");
        }
        else {
          System.out.println(
            "[PsiKit] unsupported raw value: " + typeString
          );
        }
        break;
    }
  }
}
