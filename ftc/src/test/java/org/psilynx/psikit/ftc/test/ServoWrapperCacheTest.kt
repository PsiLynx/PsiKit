package org.psilynx.psikit.ftc.test

import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.psilynx.psikit.ftc.wrappers.ServoWrapper

class ServoWrapperCacheTest {

    @Test
    fun servoWrapperCachesCommandedStateWithoutToLog() {
        val wrapper = ServoWrapper(null)

        wrapper.setDirection(Servo.Direction.REVERSE)
        wrapper.setPosition(0.42)
        wrapper.setPwmRange(PwmControl.PwmRange(600.0, 2400.0))
        wrapper.setPwmEnable()

        assertEquals(Servo.Direction.REVERSE, wrapper.direction)
        assertEquals(0.42, wrapper.position, 1e-9)
        assertEquals(600.0, wrapper.pwmRange.usPulseLower, 1e-9)
        assertEquals(2400.0, wrapper.pwmRange.usPulseUpper, 1e-9)
        assertTrue(wrapper.isPwmEnabled)

        wrapper.setPwmDisable()
        assertFalse(wrapper.isPwmEnabled)
    }
}
