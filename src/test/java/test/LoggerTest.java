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
    public void testLogger() throws InterruptedException {
        RLOGServer server = new RLOGServer();
        RLOGWriter writer = new RLOGWriter("logs/", "test");
        Logger.addDataReceiver(server);
        Logger.addDataReceiver(writer);
        Logger.start();
        Logger.periodicAfterUser(0, 0);

        while(true){
            Logger.periodicBeforeUser();
            Logger.recordOutput("Test/test", new Random().nextDouble());
            Logger.recordOutput("Test/pose", new Pose2d(1, 2, Rotation2d.kZero));
            Thread.sleep(20);
            Logger.periodicAfterUser(0, 0);
        }
    }
}