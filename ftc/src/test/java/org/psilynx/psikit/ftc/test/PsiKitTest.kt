package org.psilynx.psikit.ftc.test

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.junit.Test
import org.junit.runner.RunWith
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.core.rlog.RLOGReplay
import org.psilynx.psikit.core.rlog.RLOGServer
import org.psilynx.psikit.ftc.FtcLoggingSession
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.psilynx.psikit.ftc.wrappers.GamepadWrapper

@Config(shadows = [ShadowAppUtil::class])
@RunWith(RobolectricTestRunner::class)
class PsiKitTest {

    @Test fun replayFromFile(){
        println("starting....")

        // Allow replay to install a mock/wrapped HardwareMap for hardwareMap.get(...).
        System.setProperty("psikitReplayMockHardwareMap", "true")

        val replaySource = RLOGReplay("testLog.rlog")

        val opMode = @TeleOp object : OpMode() {
            override fun init() {}
            override fun loop() {}
        }

        // Ensure DriverStationLogger and other session components see non-null gamepads.
        opMode.gamepad1 = GamepadWrapper(null)
        opMode.gamepad2 = GamepadWrapper(null)

        val session = FtcLoggingSession()
        session.start(
            opMode = opMode,
            rlogPort = 0,
            filename = "",
            replaySource = replaySource,
            configure = {
                Logger.addDataReceiver(RLOGServer())
            }
        )

        Logger.periodicAfterUser(0.0, 0.0)

        val device = opMode.hardwareMap.get(GoBildaPinpointDriver::class.java, "i1")

        try {
            while (Logger.getTimestamp() < 295) {
                Logger.periodicBeforeUser()
                session.logOncePerLoop(opMode)

                println(device.position)

                Logger.periodicAfterUser(0.0, 0.0)
            }
        } finally {
            session.end()
        }
    }
}