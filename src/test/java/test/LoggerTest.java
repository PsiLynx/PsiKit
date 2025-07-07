package test;

import org.junit.Test;
import org.psilynx.psikit.Logger;
import org.psilynx.psikit.io.RLOGServer;
import org.psilynx.psikit.io.RLOGWriter;
import org.psilynx.psikit.wpi.Pose2d;
import org.psilynx.psikit.wpi.Rotation2d;

import java.util.Random;

import static java.lang.Thread.sleep;

public class LoggerTest {

    @Test
    public void testServer() throws InterruptedException {
        RLOGServer server = new RLOGServer();
        Logger.recordMetadata("alliance", "red");
        RLOGWriter writer = new RLOGWriter("logs/", "serverTestLog");
        TestInput inputs = new TestInput();
        Logger.disableConsoleCapture();
        Logger.addDataReceiver(server);
        Logger.addDataReceiver(writer);
        Logger.start();
        Logger.periodicAfterUser(0, 0);

        int i = 0;
        while(i < 50){
            inputs.number = i;
            inputs.pose = new Pose2d(i, 2, Rotation2d.kZero);
            Logger.periodicBeforeUser();
            Logger.processInputs("TestInput", inputs);
            Logger.recordOutput("Test/test", new Random().nextDouble());
            Logger.recordOutput("Test/i", i);
            Thread.sleep(20);
            Logger.periodicAfterUser(0, 0);
            i ++;
        }
    }
}