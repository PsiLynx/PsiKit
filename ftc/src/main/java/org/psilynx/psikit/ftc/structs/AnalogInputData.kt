package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.HardwareDevice.Manufacturer
import org.psilynx.psikit.core.wpi.Struct
import org.psilynx.psikit.core.wpi.StructSerializable
import java.nio.ByteBuffer

class AnalogInputData(
    val voltage: Double,
    val maxVoltage: Double,
) : StructSerializable {
    val struct = AnalogInputDataStruct()

    val device = object : AnalogInput(null, 0) {
        override fun getVoltage() = this@AnalogInputData.voltage
        override fun getMaxVoltage() = this@AnalogInputData.maxVoltage
        override fun getConnectionInfo() = ""
        override fun getDeviceName() = "MockAnalogInput"
        override fun getManufacturer() = Manufacturer.Other
        override fun getVersion() = 1
        override fun close() {}
        override fun resetDeviceConfigurationForOpMode() {}
    }
    constructor(input: AnalogInput) : this(
        voltage = input.voltage,
        maxVoltage = input.maxVoltage
    )

    class AnalogInputDataStruct : Struct<AnalogInputData> {
        override fun getTypeClass() = AnalogInputData::class.java

        override fun getTypeName() = "analogInputData"

        override fun getSize() = (
            Struct.kSizeDouble +     // voltage
            Struct.kSizeDouble       // maxVoltage
        )

        override fun getSchema() = (
            "double voltage; double maxVoltage"
        )

        override fun unpack(bb: ByteBuffer): AnalogInputData {
            return AnalogInputData(
                voltage = bb.getDouble(),
                maxVoltage = bb.getDouble()
            )
        }

        override fun pack(bb: ByteBuffer, value: AnalogInputData) {
            with(value) {
                bb.putDouble(voltage)
                bb.putDouble(maxVoltage)
            }
        }
    }
    companion object {
        val empty = AnalogInputData(0.0, 0.0)
    }
}