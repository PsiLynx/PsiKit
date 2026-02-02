package test;

import org.junit.Test;
import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.core.rlog.RLOGReplay;
import org.psilynx.psikit.core.rlog.RLOGWriter;
import org.psilynx.psikit.core.wpi.math.Pose2d;
import org.psilynx.psikit.core.wpi.math.Rotation2d;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Self-contained replay test.
 *
 * This avoids depending on a checked-in binary fixture (logs/testLog.rlog), which can sometimes be
 * missing/empty or replaced with an unrelated log.
 */
public class LogFileGeneratedReplayTest {

    @Test
    public void testReadFile_generatedRlogThenReplay() throws Exception {
        final Path tempDir = Files.createTempDirectory("psikit-core-");
        final Path logFile = tempDir.resolve("testLog.rlog");

        // 1) Generate a deterministic log
        Logger.reset();
        Logger.disableConsoleCapture();

        final int[] tick = new int[]{0};

        // RLOGWriter only writes when (timestamp - lastTimestamp) > 0.0001, so use a step > 0.0001.
        final double timeDivisor = 5000.0; // step = 0.0002s
        Logger.setTimeSource(() -> tick[0] / timeDivisor);

        TestInput writeInputs = new TestInput();
        RLOGWriter writer = new RLOGWriter(tempDir.toString().replace('\\', '/') + "/", "testLog.rlog");
        Logger.addDataReceiver(writer);

        Logger.start();
        Logger.periodicAfterUser(0, 0);

        // Include i=1 as the first cycle because Logger.start() consumes one cycle immediately.
        for (int i = 1; i < 400; i++) {
            tick[0] = i;
            writeInputs.number = i;
            writeInputs.pose = new Pose2d(i, 2, Rotation2d.kZero);

            Logger.periodicBeforeUser();
            Logger.processInputs("TestInput", writeInputs);
            Logger.periodicAfterUser(0, 0);
        }
        Logger.end();

        assertTrue("Expected log file to exist: " + logFile, Files.exists(logFile));
        assertTrue("Expected log file to be non-empty: " + logFile, Files.size(logFile) > 0);

        // 2) Replay and validate
        Logger.reset();
        Logger.disableConsoleCapture();

        RLOGReplay replaySource = new RLOGReplay(logFile.toString());
        replaySource.start();
        Logger.setReplaySource(replaySource);

        TestInput inputs = new TestInput();

        Logger.start();
        Logger.periodicAfterUser(0, 0);

        for (int i = 1; i < 400; i++) {
            Logger.periodicBeforeUser();
            Logger.processInputs("TestInput", inputs);

            assertEquals(i, inputs.number);
            assertNotNull("Expected pose to be present in replay", inputs.pose);
            assertEquals(i, inputs.pose.getX(), 1e-12);
            assertEquals(i / timeDivisor, Logger.getTimestamp(), 1e-12);

            Logger.periodicAfterUser(0, 0);
        }

        Logger.end();
    }
}
