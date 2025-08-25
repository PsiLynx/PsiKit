package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.Gamepad
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs

class GamepadWrapper(val gamepad: Gamepad?): Gamepad(), LoggableInputs {
    override fun toLog(table: LogTable) {
        gamepad!!
        table.put("ButtonCount", NUM_BUTTONS)
        table.put("ButtonValues", listOf(
            // msb
            gamepad.touchpad_finger_2,
            gamepad.touchpad_finger_1,
            gamepad.right_stick_button,
            gamepad.left_stick_button,
            gamepad.start,
            gamepad.back,
            gamepad.right_bumper,
            gamepad.left_bumper,
            gamepad.y,
            gamepad.x,
            gamepad.b,
            gamepad.a,
            // lsb
        ).fold(0) { acc, value -> (acc shl 1) + if(value) 1 else 0})
        table.put("AxisValues", floatArrayOf(
            gamepad.left_stick_x,
            gamepad.left_stick_y,
            gamepad.left_trigger,
            gamepad.right_trigger,
            gamepad.right_stick_x,
            gamepad.right_stick_y,
            gamepad.touchpad_finger_1_x,
            gamepad.touchpad_finger_1_y,
            gamepad.touchpad_finger_2_x,
            gamepad.touchpad_finger_2_y,
        ))
        table.put("POVs", intArrayOf(
            if(gamepad.dpad_up) 0
            else if(gamepad.dpad_right) 90
            else if(gamepad.dpad_down) 180
            else if(gamepad.dpad_left) 270
            else -1
        )) // only one POV (dpad)
        touchpad_finger_1_x = gamepad.touchpad_finger_1_x
        touchpad_finger_1_y = gamepad.touchpad_finger_1_y
        touchpad_finger_2_x = gamepad.touchpad_finger_2_x
        touchpad_finger_2_y = gamepad.touchpad_finger_2_y
        right_stick_button  = gamepad.right_stick_button
        left_stick_button   = gamepad.left_stick_button
        touchpad_finger_1   = gamepad.touchpad_finger_1
        touchpad_finger_2   = gamepad.touchpad_finger_2
        right_stick_x       = gamepad.right_stick_x
        right_stick_y       = gamepad.right_stick_y
        right_trigger       = gamepad.right_trigger
        left_stick_x        = gamepad.left_stick_x
        left_stick_y        = gamepad.left_stick_y
        left_trigger        = gamepad.left_trigger
        right_bumper        = gamepad.right_bumper
        left_bumper         = gamepad.left_bumper
        dpad_right          = gamepad.dpad_right
        dpad_left           = gamepad.dpad_left
        dpad_down           = gamepad.dpad_down
        triangle            = gamepad.triangle
        touchpad            = gamepad.touchpad
        dpad_up             = gamepad.dpad_up
        options             = gamepad.options
        circle              = gamepad.circle
        square              = gamepad.square
        guide               = gamepad.guide
        start               = gamepad.start
        cross               = gamepad.cross
        share               = gamepad.share
        back                = gamepad.back
        a                   = gamepad.a
        b                   = gamepad.b
        x                   = gamepad.x
        y                   = gamepad.y
    }

    override fun fromLog(table: LogTable) {
        var valuesInt = table.get("ButtonValues", 0)
        val valuesList = mutableListOf<Boolean>()
        repeat(NUM_BUTTONS) {
            valuesList.add(valuesInt % 2 == 1)
            valuesInt = valuesInt shr 1
        }

        valuesList.reverse() // decoding puts lsb in first, needs to be last
        touchpad_finger_2  = valuesList[0]
        touchpad_finger_1  = valuesList[1]
        right_stick_button = valuesList[2]
        left_stick_button  = valuesList[3]
        start              = valuesList[4]
        back               = valuesList[5]
        right_bumper       = valuesList[6]
        left_bumper        = valuesList[7]
        y                  = valuesList[8]
        x                  = valuesList[9]
        b                  = valuesList[10]
        a                  = valuesList[11]

        val axisValues = table.get("AxisValues", floatArrayOf())

        left_stick_x        = axisValues[0]
        left_stick_y        = axisValues[1]
        left_trigger        = axisValues[2]
        right_trigger       = axisValues[3]
        right_stick_x       = axisValues[4]
        right_stick_y       = axisValues[5]
        touchpad_finger_1_x = axisValues[6]
        touchpad_finger_1_y = axisValues[7]
        touchpad_finger_2_x = axisValues[8]
        touchpad_finger_2_y = axisValues[9]

        val povDir = table.get("POVs", intArrayOf())[0]

        if(povDir == 0)   dpad_up    = true
        if(povDir == 90)  dpad_right = true
        if(povDir == 180) dpad_down  = true
        if(povDir == 270) dpad_left  = true

    }
    companion object {
        const val NUM_BUTTONS = 12
    }
}
