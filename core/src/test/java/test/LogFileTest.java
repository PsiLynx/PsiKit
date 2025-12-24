package test;

import org.junit.Ignore;
import org.junit.Test;
import org.psilynx.psikit.core.LogTable;
import org.psilynx.psikit.core.rlog.RLOGReplay;
import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.core.rlog.RLOGDecoder;
import org.psilynx.psikit.core.rlog.RLOGWriter;
import org.psilynx.psikit.core.wpi.math.Pose2d;
import org.psilynx.psikit.core.wpi.math.Rotation2d;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Random;

import static org.junit.Assert.*;
import static java.lang.Thread.sleep;

public class LogFileTest {

    @Test
    @Ignore("Integration test requires a specific replay log file with Pose2d data")
    public void testReadFile() throws InterruptedException {
        Logger.reset();
        RLOGReplay replaySource = new RLOGReplay(
                "logs/testLog.rlog"
        );
        replaySource.start();
        TestInput inputs = new TestInput();
        Logger.setReplaySource(replaySource);
        //RLOGServer server = new RLOGServer();
        //Logger.addDataReceiver(server);
        Logger.start();
        Logger.periodicAfterUser(0, 0);

        for (int i = 2; i < 400; i++) {
            Logger.periodicBeforeUser();
            Logger.processInputs("TestInput", inputs);
            LogTable table = Logger.getEntry();
            System.out.println(table.getTimestamp());
            System.out.println(i);
            System.out.println(inputs.pose.getX());
            System.out.println();
            assert inputs.number == i;
            assert Logger.getTimestamp() - ( i / 50000.0 ) < 1e-9;
            assert inputs.pose.getX() == i;
            sleep(20);
            Logger.periodicAfterUser(0, 0);
        }
        Logger.end();
    }
    private int i = 1;
    private double getFakeTime(){
        System.out.println(i);
        return i / 50000.0;
    }
    @Test
    public void testCreateFile() throws InterruptedException {
        Logger.reset();
        Logger.setTimeSource(this::getFakeTime);
        Logger.recordMetadata("alliance", "red");
        RLOGWriter writer = new RLOGWriter("logs/", "serverTestLog");
        TestInput inputs = new TestInput();
        Logger.disableConsoleCapture();
        Logger.addDataReceiver(writer);
        Logger.start();
        Logger.periodicAfterUser(0, 0);

        while(i < 500){
            i ++;
            inputs.number = i;
            inputs.pose = new Pose2d(i, 2, Rotation2d.kZero);
            Logger.periodicBeforeUser();
            Logger.processInputs("TestInput", inputs);
            Logger.recordOutput("Test/test", new Random().nextDouble());
            Logger.recordOutput("Test/i", i);
            //System.out.println(i);
            System.out.println(Logger.getTimestamp());
            sleep(20);
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
        // Timestamp record
        out.writeByte(0x00);
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