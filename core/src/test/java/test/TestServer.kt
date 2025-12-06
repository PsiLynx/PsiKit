package test

import org.junit.Test
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.core.rlog.RLOGServer
import java.lang.Thread.sleep
import kotlin.random.Random

class TestServer {
    @Test fun testServer(){
        Logger.reset()
        Logger.addDataReceiver(RLOGServer())
        Logger.start()

        repeat(100) {
            Logger.periodicBeforeUser()
            Logger.recordOutput("test", Random.nextDouble())
            sleep(20)
            Logger.periodicAfterUser(0.0, 0.0)
        }
    }
}