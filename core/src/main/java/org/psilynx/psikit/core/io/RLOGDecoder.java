package org.psilynx.psikit.core.io;

import org.psilynx.psikit.core.LogTable;
import org.psilynx.psikit.core.LogTable.LoggableType;
import org.psilynx.psikit.core.Pair;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.floor;

/** Converts byte array format to log tables. */
public class RLOGDecoder {
  public static final String STRUCT_PREFIX = "struct:";
  public static final List<Byte> supportedLogRevisions = List.of((byte) 2);
  private int total = 0;

  private Byte logRevision = null;
  private LogTable table = new LogTable(0);
  private Map<Short, Pair<String, String>> keyIDs = new HashMap<>();

  public LogTable decodeTable(DataInputStream input) {
    readTable: try {
      if (logRevision == null) {
        this.total = input.available();
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
//      if (input.available() == 0) {
//        System.out.println("[Psikit] end of file");
//        return null; // No more data, so we can't start a new table
//      }

      try {
        System.out.println("[PsiKit] read bytes: " + ( this.total - input.available() ));
        table = new LogTable(decodeTimestamp(input), table);
        boolean done = false;
        while (!done) {
          System.out.println("[PsiKit] read bytes: " + ( total - input.available() ));
//        if (input.available() == 0) {
//          break readTable; // This was the last cycle, return the data
//        }

          byte type = input.readByte();
          System.out.println("[Psikit] type: " + type);
          switch (type) {
            case 0: // Next timestamp
              done = true;
              break;
            case 1: // New key ID
              decodeKey(input);
              break;
            case 2: // Updated field
              decodeValue(input);
              break;
          }
        }
      } catch (EOFException ignored){
        System.out.println("[Psikit] got EOF, ignoring");
      }

    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("[Psikit] problem reading file");
      return null; // Problem decoding, might have been interrupted while writing this cycle
    }

    return LogTable.clone(table);
  }

  private double decodeTimestamp(DataInputStream input) throws IOException {
    double timestamp = input.readDouble();
    System.out.println("[Psikit] decoded timestamp: " + timestamp);
    return timestamp;
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
    System.out.println("[PsiKit] Key defined: ID=" + keyID + ", key=" + key + ", type=" + type);
  }

  private void decodeValue(DataInputStream input) throws IOException {
    Pair<String, String> keyID = keyIDs.get(input.readShort());
    short length = input.readShort();
    System.out.println("[PsiKit] length of value: " + length);
    String key = keyID.getFirst();
    String typeString = keyID.getSecond();
    LoggableType type = LoggableType.fromWPILOGType(typeString);

    switch (type) {
      case Boolean:
        table.put(key, input.readBoolean());
        break;
      case Integer:
        long val = input.readLong();
        System.out.println("[PsiKit] key: " + key);
        System.out.println("[PsiKit] value: " + val);
        table.put(key, val);
        break;
      case Double:
        table.put(key, input.readDouble());
        break;
      case String:
        table.put(key, new String(input.readNBytes(length), StandardCharsets.UTF_8));
        break;
      case BooleanArray:
        boolean[] booleanArray = new boolean[length];
        for (int i = 0; i < length; i++) {
          booleanArray[i] = input.readBoolean();
        }
        table.put(key, booleanArray);
        break;
      case IntegerArray:
        int[] intArray = new int[length / 4];
        for (int i = 0; i < length; i++) {
          intArray[i] = input.readInt();
        }
        table.put(key, intArray);
        break;
      case DoubleArray:
        double[] doubleArray = new double[length / 8];
        for (int i = 0; i < length; i++) {
          doubleArray[i] = input.readDouble();
        }
        table.put(key, doubleArray);
        break;
      case StringArray:
        int arrLength = input.readInt();
        String[] stringArray = new String[arrLength];
        for (int i = 0; i < arrLength; i++) {
          int stringLength = input.readInt();
          stringArray[i] = new String(input.readNBytes(stringLength), "UTF-8");
        }
        table.put(key, stringArray);
        break;
      default:

        if (typeString.startsWith(STRUCT_PREFIX)) {
          String schemaType = typeString.substring(STRUCT_PREFIX.length());
          byte[] value = input.readNBytes(length);
          if (schemaType.endsWith("[]")) {
            String actualType = schemaType.substring(0, schemaType.length() - 2);
            table.put(key, new LogTable.LogValue(value, actualType));
          } else {
            table.put(key, new LogTable.LogValue(value, typeString));
          }
        }
        else {
          System.out.println(
            "[PsiKit] unsupported raw value: " + typeString
          );
          input.skipBytes(length);
        }
        break;
    }
  }
}
