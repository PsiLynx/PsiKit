package test;

import org.junit.Test;
import org.psilynx.psikit.LogTable;
import org.psilynx.psikit.RLOGReplay;
import org.psilynx.psikit.Logger;
import org.psilynx.psikit.io.RLOGDecoder;
import org.psilynx.psikit.wpi.Pose2d;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Map;

import static org.junit.Assert.*;

public class LogFileTest {

    @Test
    public void testReadFile() throws InterruptedException {
        RLOGReplay replaySource = new RLOGReplay(
                "logs/testLog.rlog"
        );
        replaySource.start();
        TestInput inputs = new TestInput();
        Logger.setReplaySource(replaySource);
        Logger.start();
        Logger.periodicAfterUser(0, 0);

        for (int i = 0; i < 40; i++) {
            Logger.periodicBeforeUser();
            Logger.processInputs("TestInput", inputs);
            LogTable table = Logger.getEntry();
            // System.out.println(
                // "length: " +
                // table.getAll(false).size()
            // );
            for(Map.Entry<String, LogTable.LogValue> entry:
                table.getAll(false).entrySet()
            ){
                //System.out.println("key: " + entry.getKey());
                //System.out.println("value: " + entry.getValue());
                if(entry.getValue().type == LogTable.LoggableType.Integer) {
                    //System.out.println("int: " + entry.getValue().getInteger());
                }
            }
            // System.out.println(Logger.getTimestamp());
            System.out.println(i);
            System.out.println(inputs.pose.getX());
            System.out.println();
            assert inputs.number == i;
            assert inputs.pose.getX() == i;
            Logger.periodicAfterUser(0, 0);
        }
        Logger.end();
    }
    @Test
    public void testDecodeMinimalRlogR2() throws Exception {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);

        // Header
        out.writeByte(0x02); // Log revision R2
        out.writeByte(0x00); // Timestamp type (ignored)

        // Timestamp message
        out.writeDouble(1.23); // Timestamp

        // Key definition
        out.writeByte(0x01); // Type 1 = key
        out.writeShort(0); // Key ID = 0
        out.writeShort((short) "/Drivetrain/LeftPos".getBytes().length);
        out.write("/Drivetrain/LeftPos".getBytes("UTF-8")); // Key
        out.writeShort((short) "double".getBytes().length);
        out.write("double".getBytes("UTF-8")); // Type

        // Field value
        out.writeByte(0x02); // Type 2 = field
        out.writeShort(0); // Key ID
        out.writeShort(8); // Length of double
        out.writeDouble(42.0); // Value

        // Next cycle timestamp (to end the current cycle)
        out.writeByte(0x00);
        out.writeDouble(2.34);

        // Now decode
        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        DataInputStream dataIn = new DataInputStream(byteIn);

        RLOGDecoder decoder = new RLOGDecoder();
        LogTable decoded = decoder.decodeTable(dataIn);

        assertNotNull(decoded);
        assertEquals(1.23, decoded.getTimestamp(), 1e-6);
        assertEquals(42.0, decoded.get("/Drivetrain/LeftPos", 0.0), 1e-6);
    }
}