package org.psilynx.psikit.ftc.test

import org.junit.Test
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.core.rlog.RLOGServer
import org.psilynx.psikit.ftc.wrappers.GamepadWrapper
import org.psilynx.psikit.ftc.test.fakehardware.FakeGamepad

class GamepadTest {
    @Test fun sequentiallyPressButtons(){
        println("starting....")

        Logger.reset()

        var i = 0
        Logger.setTimeSource { i.toDouble() / 4 }
        Logger.setReplaySource(null)
        Logger.addDataReceiver(RLOGServer())
        Logger.start()
        Logger.periodicAfterUser(0.0, 0.0)

        val fakeGamepad = FakeGamepad()
        val gamepad = GamepadWrapper(fakeGamepad)

        fun loop(action: () -> Unit = {}) {
            Thread.sleep(20)
            Logger.periodicBeforeUser()
            action()
            Logger.processInputs("/DriverStation/Joystick0", gamepad)
            Logger.periodicAfterUser(0.0, 0.0)
            i++
        }

        try {
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
        } finally {
            try {
                Logger.end()
            } catch (_: Throwable) {
                // ignore
            }
        }
    }
}