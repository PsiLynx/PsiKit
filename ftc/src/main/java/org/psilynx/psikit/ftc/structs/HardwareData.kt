package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.HardwareDevice
import org.psilynx.psikit.core.wpi.StructSerializable

interface HardwareData: StructSerializable {
    val device: HardwareDevice

}