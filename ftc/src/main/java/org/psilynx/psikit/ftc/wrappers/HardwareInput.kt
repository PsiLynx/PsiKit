package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.robotcore.hardware.HardwareDevice
import org.psilynx.psikit.core.LoggableInputs

interface HardwareInput<T: HardwareDevice>: LoggableInputs {
    fun new(wrapped: T?): HardwareInput<T>
}