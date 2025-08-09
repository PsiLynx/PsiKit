package org.psilynx.psikit.ftc.test

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import org.junit.Test
import org.psilynx.psikit.ftc.inputs.HardwareMapInput
import org.psilynx.psikit.ftc.inputs.PinpointInput
import org.psilynx.psikit.ftc.test.fakehardware.FakeHardwareMap

class HardwareMapInputTest {
    @Test
    fun testGetI2cDevice(){
        val input = HardwareMapInput(FakeHardwareMap)
        val pinpoint = input.get(GoBildaPinpointDriver::class.java, "test")
        assert(pinpoint is PinpointInput)
    }
    @Test
    fun testCreateInput() {
        HardwareMapInput(FakeHardwareMap)
    }
}