package org.psilynx.psikit.ftc.test

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import org.junit.Test
import org.junit.runner.RunWith
import org.psilynx.psikit.ftc.HardwareMapWrapper
import org.psilynx.psikit.ftc.wrappers.PinpointWrapper
import org.psilynx.psikit.ftc.test.fakehardware.FakeHardwareMap
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.qualcomm.hardware.lynx.LynxVoltageSensor

@Config(shadows = [ShadowAppUtil::class])
@RunWith(RobolectricTestRunner::class)
class HardwareMapInputTest {
    @Test
    fun testGetI2cDevice(){
        val input = HardwareMapWrapper(FakeHardwareMap)
        val pinpoint = input.get(GoBildaPinpointDriver::class.java, "test")
        assert(pinpoint is PinpointWrapper)
    }
    @Test
    fun testCreateInput() {
        HardwareMapWrapper(FakeHardwareMap)
    }
}