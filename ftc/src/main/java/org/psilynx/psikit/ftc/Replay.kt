package org.psilynx.psikit.ftc

import org.psilynx.psikit.core.LogReplaySource
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.wrappers.GamepadWrapper

class Replay(val opMode: PsiKitLinearOpMode, val replaySource: LogReplaySource) {
    fun run(){
        opMode.hardwareMap = null
        opMode.gamepad1 = GamepadWrapper(null)
        opMode.gamepad2 = GamepadWrapper(null)
        Logger.setReplay(true)
        Logger.setReplaySource(replaySource)
        opMode.runOpMode()
    }
}