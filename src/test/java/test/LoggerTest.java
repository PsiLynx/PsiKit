package test;

import org.junit.Test;
import org.psilynx.psikit.LogSocketServer;
import org.psilynx.psikit.Logger;
import org.psilynx.psikit.rlog.ByteLogReceiver;

import java.util.Random;

import static java.lang.Thread.sleep;

public class LoggerTest {

    @Test
    public void testLogger() throws InterruptedException {
        LogSocketServer server = new LogSocketServer(5800);
        ByteLogReceiver receiver = new ByteLogReceiver(
                "logs"
        );
        TestInputs inputs = new TestInputs();
        Logger.getInstance().addDataReceiver(receiver);
        Logger.getInstance().addDataReceiver(server);
        Logger.getInstance().recordMetadata("alliance", "red");
        Logger.getInstance().start();
        Logger.getInstance().periodicAfterUser();

        int i = 0;
        while(true){
            inputs.number = i;
            Logger.getInstance().periodicBeforeUser();
            Logger.getInstance().recordOutput("Test/test", new Random().nextDouble());
            Logger.getInstance().recordOutput("Test/time", Logger.getInstance().getTimestamp());
            Logger.getInstance().processInputs("TestInput", inputs);
            Thread.sleep(20);
            Logger.getInstance().periodicAfterUser();
            i ++;
        }
    }
}