package test;

import org.junit.Test;
import org.psilynx.psikit.LogSocketServer;
import org.psilynx.psikit.Logger;
import org.psilynx.psikit.ReceiverThread;
import org.psilynx.psikit.rlog.ByteEncoder;
import org.psilynx.psikit.rlog.ByteLogReceiver;
import org.psilynx.psikit.rlog.ByteLogReplay;
import org.psilynx.psikit.rlog.LogReplaySource;

import java.util.Random;

import static java.lang.Thread.sleep;

public class LogFileTest {

    @Test
    public void testReadFile() throws InterruptedException {
        ByteLogReplay replaySource = new ByteLogReplay(
                "logs/testLog.rlog"
        );
        TestInputs inputs = new TestInputs();
        Logger.getInstance().setReplaySource(replaySource);
        Logger.getInstance().start();
        Logger.getInstance().periodicAfterUser();

        for (int i = 0; i < 10; i++) {
            Logger.getInstance().periodicBeforeUser();
            Logger.getInstance().processInputs("TestInput", inputs);
            assert inputs.number == i;
            Logger.getInstance().periodicAfterUser();
        }
        Logger.getInstance().end();
    }
}