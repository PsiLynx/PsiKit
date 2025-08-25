package org.psilynx.psikit.ftc.test.fakehardware

import com.qualcomm.robotcore.hardware.Gamepad


class FakeGamepad: FakeHardware, Gamepad() {
    val buttons = mapOf<String, (Boolean) -> Unit>(
        "a"                  to {value -> a                  = value},
        "b"                  to {value -> b                  = value},
        "x"                  to {value -> x                  = value},
        "y"                  to {value -> y                  = value},
        "left_bumper"        to {value -> left_bumper        = value},
        "right_bumper"       to {value -> right_bumper       = value},
        "back"               to {value -> back               = value},
        "start"              to {value -> start              = value},
        "left_stick_button"  to {value -> left_stick_button  = value},
        "right_stick_button" to {value -> right_stick_button = value},
        "dpad_up"            to {value -> dpad_up            = value},
        "dpad_down"          to {value -> dpad_down          = value},
        "dpad_left"          to {value -> dpad_left          = value},
        "dpad_right"         to {value -> dpad_right         = value},
        "touchpad_finger_1"  to {value -> touchpad_finger_1  = value},
        "touchpad_finger_2"  to {value -> touchpad_finger_2  = value},
    )
    val axes = mapOf<String, (Float) -> Unit>(
        "left_stick_x"        to { value -> left_stick_x         = value },
        "left_stick_y"        to { value -> left_stick_y         = value },
        "left_trigger"        to { value -> left_trigger         = value },
        "right_trigger"       to { value -> right_trigger        = value },
        "right_stick_x"       to { value -> right_stick_x        = value },
        "right_stick_y"       to { value -> right_stick_y        = value },
        "touchpad_finger_1_x" to { value -> touchpad_finger_1_x  = value },
        "touchpad_finger_1_y" to { value -> touchpad_finger_1_y  = value },
        "touchpad_finger_2_x" to { value -> touchpad_finger_2_x  = value },
        "touchpad_finger_2_y" to { value -> touchpad_finger_2_y  = value },
    )
    private fun setState(button: String, value: Boolean){
        buttons[button]?.invoke(value) ?: error("button not found: $button")
    }
    fun setAxisState(axis: String, value: Double){
        axes[axis]?.invoke(value.toFloat()) ?: error("axis not found: $axis")
    }

    fun press   (button: String) = setState(button, true)
    fun depress (button: String) = setState(button, false)

    override fun update(deltaTime: Double) { }
    override fun resetDeviceConfigurationForOpMode() {
        a            = false
        b            = false
        x            = false
        y            = false
        back         = false
        start        = false
        options      = false
        dpad_up      = false
        dpad_down    = false
        dpad_left    = false
        dpad_right   = false
        left_bumper  = false
        right_bumper = false
    }
}
