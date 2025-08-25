package org.psilynx.psikit.ftc.test

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.junit.Test
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.core.rlog.RLOGReplay
import org.psilynx.psikit.core.rlog.RLOGServer
import org.psilynx.psikit.ftc.PsiKitOpMode
import org.psilynx.psikit.ftc.Replay
import org.psilynx.psikit.ftc.wrappers.GamepadWrapper
import org.psilynx.psikit.ftc.test.fakehardware.FakeGamepad

class GamepadTest {
    @Test fun sequentiallyPressButtons(){
        println("starting....")
        val replaySource = RLOGReplay("testLog.rlog")
        Replay(
            @TeleOp object : PsiKitOpMode() {
                override fun runOpMode() {
                    var i = 0
                    Logger.setTimeSource { i.toDouble() / 4 }
                    Logger.setReplaySource(null)
                    val server = RLOGServer()
                    Logger.addDataReceiver(server)

                    Logger.start() // Start logging! No more data receivers, replay sources, or metadata values may be added.
                    Logger.periodicAfterUser(0.0, 0.0)

                    val fakeGamepad = FakeGamepad()
                    val gamepad = GamepadWrapper(fakeGamepad)

                    fun loop(
                        action: () -> Unit = {}
                    ){
                        sleep(20)
                        Logger.periodicBeforeUser()
                        action()
                        Logger.processInputs(
                            "/DriverStation/Joystick0",
                            gamepad
                        )
                        Logger.periodicAfterUser(0.0, 0.0)
                        i ++
                    }

                    fakeGamepad.buttons.keys.forEach { name ->
                        loop { fakeGamepad.press(name) }
                        loop { fakeGamepad.depress(name) }
                    }
                    fakeGamepad.axes.keys.forEach { name ->
                        loop { fakeGamepad.setAxisState(name, -1.0) }
                        loop { fakeGamepad.setAxisState(name, -0.5) }
                        loop { fakeGamepad.setAxisState(name,  0.0) }
                        loop { fakeGamepad.setAxisState(name,  0.5) }
                        loop { fakeGamepad.setAxisState(name,  1.0) }
                        loop { fakeGamepad.setAxisState(name,  0.0) }
                    }
                    loop { }
                    loop { }
                }
            },
            replaySource
        ).run()
    }
}