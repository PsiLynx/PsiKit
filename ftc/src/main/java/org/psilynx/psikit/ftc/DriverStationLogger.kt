package org.psilynx.psikit.ftc

import com.qualcomm.robotcore.hardware.Gamepad
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.wrappers.GamepadWrapper

/**
 * Logs FTC Driver Station inputs in the AdvantageScope "Joysticks" schema.
 *
 * This intentionally logs only /DriverStation/Joystick0 and /DriverStation/Joystick1
 * (no redundant /DriverStation/GamepadN), while still capturing extra FTC Gamepad
 * fields inside the same joystick table.
 */
class DriverStationLogger {

    private companion object {
        const val JOYSTICK0_TOPIC = "/DriverStation/Joystick0"
        const val JOYSTICK1_TOPIC = "/DriverStation/Joystick1"
    }

    private var lastRawGamepad1: Gamepad? = null
    private var cachedGamepad1Inputs: LoggableInputs? = null

    private var lastRawGamepad2: Gamepad? = null
    private var cachedGamepad2Inputs: LoggableInputs? = null

    /**
     * Call once per loop, after [Logger.periodicBeforeUser].
     */
    fun log(gamepad1: Gamepad?, gamepad2: Gamepad?) {
        Logger.processInputs(JOYSTICK0_TOPIC, asLoggableGamepad1(gamepad1))
        Logger.processInputs(JOYSTICK1_TOPIC, asLoggableGamepad2(gamepad2))
    }

    private fun asLoggableGamepad1(gamepad: Gamepad?): LoggableInputs {
        if (gamepad is LoggableInputs) {
            return gamepad
        }
        if (gamepad != lastRawGamepad1 || cachedGamepad1Inputs == null) {
            lastRawGamepad1 = gamepad
            cachedGamepad1Inputs = GamepadWrapper(gamepad)
        }
        return cachedGamepad1Inputs!!
    }

    private fun asLoggableGamepad2(gamepad: Gamepad?): LoggableInputs {
        if (gamepad is LoggableInputs) {
            return gamepad
        }
        if (gamepad != lastRawGamepad2 || cachedGamepad2Inputs == null) {
            lastRawGamepad2 = gamepad
            cachedGamepad2Inputs = GamepadWrapper(gamepad)
        }
        return cachedGamepad2Inputs!!
    }
}
