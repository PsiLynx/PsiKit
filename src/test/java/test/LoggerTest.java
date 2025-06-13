package test;

import org.junit.Test;
import org.psilynx.psikit.Logger;
import org.psilynx.psikit.RLOGServer;

import java.util.Random;

import static java.lang.Thread.sleep;
import static org.junit.Assert.*;

public class LoggerTest {

    @Test
    public void testLogger() throws InterruptedException {
        RLOGServer server = new RLOGServer();
        Logger.addDataReceiver(server);
        Logger.start();
        Logger.periodicAfterUser(0, 0);

        while(true){
            Logger.periodicBeforeUser();
            System.out.println("test");
            System.out.println(new Random().nextDouble());
            Thread.sleep(20);
            Logger.periodicAfterUser(0, 0);
        }
    }
}