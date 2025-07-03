package test;

import org.junit.Test;
import org.psilynx.psikit.LogSocketServer;
import org.psilynx.psikit.Logger;

import java.util.Random;

import static java.lang.Thread.sleep;

public class LoggerTest {

    @Test
    public void testLogger() throws InterruptedException {
        LogSocketServer server = new LogSocketServer(5800);
        Logger.getInstance().addDataReceiver(server);
        Logger.getInstance().start();
        Logger.getInstance().periodicAfterUser();

        while(true){
            Logger.getInstance().periodicBeforeUser();
            Logger.getInstance().recordOutput("test", new Random().nextDouble());
            Logger.getInstance().recordOutput("time", Logger.getInstance().getTimestamp());
            Thread.sleep(20);
            Logger.getInstance().periodicAfterUser();
        }
    }
}