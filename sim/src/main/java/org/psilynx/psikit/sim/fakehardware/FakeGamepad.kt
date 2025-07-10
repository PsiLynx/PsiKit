package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.robotcore.hardware.Gamepad


class FakeGamepad: FakeHardware, Gamepad() {
    private fun setState(button: String, value: Boolean){
        when(button){
            "a"            -> a            = value
            "b"            -> b            = value
            "x"            -> x            = value
            "y"            -> y            = value
            "back"         -> back         = value
            "start"        -> start        = value
            "options"      -> options      = value
            "dpad_up"      -> dpad_up      = value
            "dpad_down"    -> dpad_down    = value
            "dpad_left"    -> dpad_left    = value
            "dpad_right"   -> dpad_right   = value
            "left_bumper"  -> left_bumper  = value
            "right_bumper" -> right_bumper = value

        }
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