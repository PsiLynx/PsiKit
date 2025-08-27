package org.psilynx.psikit.ftc.test

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.junit.Test
import org.junit.runner.RunWith
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.core.rlog.RLOGReplay
import org.psilynx.psikit.core.rlog.RLOGServer
import org.psilynx.psikit.ftc.PsiKitOpMode
import org.psilynx.psikit.ftc.Replay
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowAppUtil::class])
@RunWith(RobolectricTestRunner::class)
class PsiKitTest {

    @Test fun replayFromFile(){
        println("starting....")
        val replaySource = RLOGReplay("testLog.rlog")
        Replay(
            @TeleOp object : PsiKitOpMode() {
                override fun runOpMode() {
                    psiKitSetup()
                    println("setup!")

                    val server = RLOGServer()
                    Logger.addDataReceiver(server)

                    Logger.start() // Start logging! No more data receivers, replay sources, or metadata values may be added.
                    Logger.periodicAfterUser(0.0, 0.0)

                    waitForStart()
                    val device = this.hardwareMap.get(
                        GoBildaPinpointDriver::class.java,
                        "i1"
                    )

                    while(Logger.getTimestamp() < 295) {
                        Logger.periodicBeforeUser()
                        processHardwareInputs()

                        println(device.position)

                        Logger.periodicAfterUser(0.0, 0.0)
                    }
                }
            },
            replaySource
        ).run()
    }
}