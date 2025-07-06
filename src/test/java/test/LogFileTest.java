package test;

import org.junit.Test;
import org.psilynx.psikit.RLOGReplay;
import org.psilynx.psikit.Logger;

public class LogFileTest {

    @Test
    public void testReadFile() throws InterruptedException {
        RLOGReplay replaySource = new RLOGReplay(
                "logs/testLog.rlog"
        );
        replaySource.start();
        TestInputs inputs = new TestInputs();
        Logger.setReplaySource(replaySource);
        Logger.start();
        Logger.periodicAfterUser(0, 0);

        for (int i = 0; i < 10; i++) {
            Logger.periodicBeforeUser();
            Logger.processInputs("TestInput", inputs);
            assert inputs.number == i;
            Logger.periodicAfterUser(0, 0);
        }
        Logger.end();
    }
}