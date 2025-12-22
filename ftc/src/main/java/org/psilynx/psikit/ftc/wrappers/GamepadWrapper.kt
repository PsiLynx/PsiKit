package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.Gamepad
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs

class GamepadWrapper(val gamepad: Gamepad?): Gamepad(), LoggableInputs {
    override fun toLog(table: LogTable) {
        if (gamepad == null) {
            table.put("ButtonCount", NUM_BUTTONS)
            table.put("ButtonValues", 0)
            table.put("AxisValues", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f))
            table.put("POVs", intArrayOf(-1))
            table.put("TouchpadAxes", floatArrayOf(0f, 0f, 0f, 0f))
            table.put("TouchpadFinger1", false)
            table.put("TouchpadFinger2", false)
            table.put("Triangle", false)
            table.put("Circle", false)
            table.put("Cross", false)
            table.put("Square", false)
            table.put("Options", false)
            table.put("Share", false)
            table.put("DpadUp", false)
            table.put("DpadRight", false)
            table.put("DpadDown", false)
            table.put("DpadLeft", false)
            return
        }

        // AdvantageScope Joysticks schema (DriverStation/JoystickN):
        // - ButtonCount (int)
        // - ButtonValues (int, bit0 = button1)
        // - AxisValues (float[])
        // - POVs (int[], degrees or -1)
        //
        // Keep the required fields stable, but also log extra FTC Gamepad state as additional keys.
        table.put("ButtonCount", NUM_BUTTONS)
        table.put("ButtonValues", listOf(
            // msb
            gamepad.touchpad,
            gamepad.guide,
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
            // AdvantageScope Joysticks expects 6 axis values.
            // Put extra axes under a separate key to avoid breaking visualization.
        table.put("AxisValues", floatArrayOf(
            gamepad.left_stick_x,
            gamepad.left_stick_y,
            gamepad.left_trigger,
            gamepad.right_trigger,
            gamepad.right_stick_x,
            gamepad.right_stick_y,
        ))
        table.put("TouchpadAxes", floatArrayOf(
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

        // Extra fields (kept separate so the Joysticks tab stays happy even if it ignores them)
        table.put("TouchpadFinger1", gamepad.touchpad_finger_1)
        table.put("TouchpadFinger2", gamepad.touchpad_finger_2)
        table.put("Triangle", gamepad.triangle)
        table.put("Circle", gamepad.circle)
        table.put("Cross", gamepad.cross)
        table.put("Square", gamepad.square)
        table.put("Options", gamepad.options)
        table.put("Share", gamepad.share)
        table.put("DpadUp", gamepad.dpad_up)
        table.put("DpadRight", gamepad.dpad_right)
        table.put("DpadDown", gamepad.dpad_down)
        table.put("DpadLeft", gamepad.dpad_left)

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
        dpad_up             = gamepad.dpad_up
        triangle            = gamepad.triangle
        touchpad            = gamepad.touchpad
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
        touchpad           = valuesList[0]
        guide              = valuesList[1]
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

        // AxisValues: required 6-length float[] schema.
        val axisValues = table.get("AxisValues", floatArrayOf())

        left_stick_x        = axisValues.getOrElse(0) { 0f }
        left_stick_y        = axisValues.getOrElse(1) { 0f }
        left_trigger        = axisValues.getOrElse(2) { 0f }
        right_trigger       = axisValues.getOrElse(3) { 0f }
        right_stick_x       = axisValues.getOrElse(4) { 0f }
        right_stick_y       = axisValues.getOrElse(5) { 0f }

        // Touchpad axes (new key).
        val touchAxes = table.get("TouchpadAxes", floatArrayOf())
        if (touchAxes.isNotEmpty()) {
            touchpad_finger_1_x = touchAxes.getOrElse(0) { 0f }
            touchpad_finger_1_y = touchAxes.getOrElse(1) { 0f }
            touchpad_finger_2_x = touchAxes.getOrElse(2) { 0f }
            touchpad_finger_2_y = touchAxes.getOrElse(3) { 0f }
        }

        // Extra fields (may not exist in older logs)
        touchpad_finger_1 = table.get("TouchpadFinger1", false)
        touchpad_finger_2 = table.get("TouchpadFinger2", false)
        triangle = table.get("Triangle", false)
        circle = table.get("Circle", false)
        cross = table.get("Cross", false)
        square = table.get("Square", false)
        options = table.get("Options", false)
        share = table.get("Share", false)

        // POV (dpad)
        dpad_up = false
        dpad_right = false
        dpad_down = false
        dpad_left = false
        val povDir = table.get("POVs", intArrayOf(-1)).getOrElse(0) { -1 }
        if(povDir == 0)   dpad_up    = true
        if(povDir == 90)  dpad_right = true
        if(povDir == 180) dpad_down  = true
        if(povDir == 270) dpad_left  = true

    }
    companion object {
        const val NUM_BUTTONS = 12
    }
}
