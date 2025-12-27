package test;

import org.junit.Test;
import org.psilynx.psikit.core.LogTable;
import org.psilynx.psikit.core.LogTable.LogValue;
import org.psilynx.psikit.core.rlog.RLOGDecoder;
import org.psilynx.psikit.core.rlog.RLOGEncoder;
import org.psilynx.psikit.core.wpi.math.Pose2d;
import org.psilynx.psikit.core.wpi.math.Rotation2d;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RLOGRoundTripTest {

  private static byte[] encodeTables(List<LogTable> tables) {
    RLOGEncoder encoder = new RLOGEncoder();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (LogTable table : tables) {
      encoder.encodeTable(table, true);
      out.writeBytes(encoder.getOutput().array());
    }
    return out.toByteArray();
  }

  private static List<LogTable> decodeAll(byte[] bytes) throws Exception {
    RLOGDecoder decoder = new RLOGDecoder();
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

    List<LogTable> tables = new ArrayList<>();
    while (in.available() > 0) {
      LogTable table = decoder.decodeTable(in);
      assertNotNull("Decoder returned null table", table);
      tables.add(table);
    }
    return tables;
  }

  private static void assertTablesEquivalent(LogTable expected, LogTable actual) {
    assertEquals(expected.getTimestamp(), actual.getTimestamp(), 1e-12);

    Map<String, LogValue> expectedAll = expected.getAll(false);
    Map<String, LogValue> actualAll = actual.getAll(false);

    for (Map.Entry<String, LogValue> e : expectedAll.entrySet()) {
      assertTrue("Missing key: " + e.getKey(), actualAll.containsKey(e.getKey()));
      LogValue expectedValue = e.getValue();
      LogValue actualValue = actualAll.get(e.getKey());
      assertEquals(
          "Value mismatch for key: " + e.getKey() + "\nexpected=" + expectedValue + "\nactual=" + actualValue,
          expectedValue,
          actualValue);
      assertEquals(
          "Type string mismatch for key: " + e.getKey(),
          expectedValue.getWPILOGType(),
          actualValue.getWPILOGType());
    }
  }

  @Test
  public void testEncoderDecoderRoundTrip_AllSupportedTypes() throws Exception {
    LogTable t1 = new LogTable(1.0);
    t1.put("bool", true);
    t1.put("i64", 1234567890123L);
    t1.put("flt", 3.25f);
    t1.put("dbl", -42.5);
    t1.put("str", "hello π");

    t1.put("boolArr", new boolean[]{true, false, true});
    t1.put("i64Arr", new long[]{0L, -1L, 5L, 9223372036854775807L});
    t1.put("fltArr", new float[]{1.5f, -2.0f, 3.0f});
    t1.put("dblArr", new double[]{Math.PI, -0.0, 1.0e-9});
    t1.put("strArr", new String[]{"a", "b", "c"});

    t1.put("raw", new byte[]{0x01, 0x02, (byte) 0xFF});

    // Struct + schema entries
    Pose2d pose1 = new Pose2d(1.0, 2.0, Rotation2d.fromDegrees(30.0));
    t1.put("pose", pose1);

    LogTable t2 = new LogTable(2.0, t1);
    t2.put("bool", false);
    t2.put("i64", -999L);
    t2.put("fltArr", new float[]{9.0f});
    t2.put("strArr", new String[]{"x", "y"});
    Pose2d pose2 = new Pose2d(3.0, 4.0, Rotation2d.fromDegrees(-90.0));
    t2.put("pose", pose2);

    List<LogTable> expected = List.of(t1, t2);

    byte[] encoded = encodeTables(expected);
    List<LogTable> decoded = decodeAll(encoded);

    assertEquals(2, decoded.size());
    assertTablesEquivalent(t1, decoded.get(0));
    assertTablesEquivalent(t2, decoded.get(1));

    // Ensure struct value is usable
    Pose2d decodedPose1 = decoded.get(0).get("pose", Pose2d.struct, Pose2d.kZero);
    Pose2d decodedPose2 = decoded.get(1).get("pose", Pose2d.struct, Pose2d.kZero);
    assertEquals(pose1.getX(), decodedPose1.getX(), 1e-12);
    assertEquals(pose1.getY(), decodedPose1.getY(), 1e-12);
    assertEquals(pose2.getX(), decodedPose2.getX(), 1e-12);
    assertEquals(pose2.getY(), decodedPose2.getY(), 1e-12);

    // Ensure at least one struct schema key exists and is preserved as raw bytes
    boolean foundSchema = false;
    for (String key : decoded.get(0).getAll(false).keySet()) {
      if (key.startsWith("/.schema/")) {
        foundSchema = true;
        assertNotNull(decoded.get(0).get(key));
        assertEquals("structschema", decoded.get(0).get(key).getWPILOGType());
        assertTrue(decoded.get(0).get(key).getRaw().length > 0);
        break;
      }
    }
    assertTrue("Expected at least one /.schema/* entry", foundSchema);

    // Quick sanity on raw payload round-trip
    assertTrue(Arrays.equals(new byte[]{0x01, 0x02, (byte) 0xFF}, decoded.get(0).get("raw", new byte[0])));
  }
}
