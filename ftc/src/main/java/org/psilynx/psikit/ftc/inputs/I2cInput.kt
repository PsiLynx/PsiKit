package org.psilynx.psikit.ftc.inputs

import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice
import org.psilynx.psikit.core.LoggableInputs

interface I2cInput<T: I2cDeviceSynchDevice<*>>: LoggableInputs {
    fun new(wrapped: T): I2cInput<T>
}