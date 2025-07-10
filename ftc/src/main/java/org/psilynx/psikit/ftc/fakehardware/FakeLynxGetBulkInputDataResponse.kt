package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.commands.core.LynxGetBulkInputDataResponse

public class FakeLynxGetBulkInputDataResponse(
    module: LynxModule,
    val quadraturePositions: Array<Int>,
    val quadratureVelocities: Array<Int>,
    val isBusy: Array<Boolean>,
    val overCurrent: Array<Boolean>,
    val analogVoltages: Array<Int>,
    val digitalInputs: Array<Boolean>,
): LynxGetBulkInputDataResponse(module) {
    override fun getDigitalInput(i: Int) = digitalInputs[i]
    override fun getAnalogInput(i: Int)  = analogVoltages[i]
    override fun isOverCurrent(i: Int)   = overCurrent[i]
    override fun getVelocity(i: Int)     = quadratureVelocities[i]
    override fun getEncoder(i: Int)      = quadraturePositions[i]
    override fun isAtTarget(i: Int)      = !isBusy[i]
}