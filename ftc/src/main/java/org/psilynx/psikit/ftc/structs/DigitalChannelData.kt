package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.DigitalChannelController
import com.qualcomm.robotcore.hardware.HardwareDevice.Manufacturer
import org.psilynx.psikit.core.wpi.Struct
import org.psilynx.psikit.core.wpi.StructSerializable
import java.nio.ByteBuffer

data class DigitalChannelData(
    val mode: DigitalChannel.Mode,
    val state: Boolean,
) : StructSerializable {
    val struct = DigitalChannelDataStruct()
    val device = object : DigitalChannel {
        override fun setMode(mode: DigitalChannelController.Mode) {}
        override fun setMode(mode: DigitalChannel.Mode) {}
        override fun setState(state: Boolean) {}

        override fun getManufacturer() = Manufacturer.Other
        override fun getDeviceName() = "MockDigitalChannel"
        override fun getState() = this@DigitalChannelData.state
        override fun getMode() = this@DigitalChannelData.mode
        override fun getConnectionInfo() = ""
        override fun getVersion() = 1

        override fun resetDeviceConfigurationForOpMode() {}
        override fun close() {}
    }

    constructor(channel: DigitalChannel) : this(
        mode = channel.mode,
        state = channel.state
    )

    class DigitalChannelDataStruct : Struct<DigitalChannelData> {
        override fun getTypeClass() = DigitalChannelData::class.java

        override fun getTypeName() = "digitalChannelData"

        override fun getSize() = (
            Struct.kSizeInt8 +      // mode (enum ordinal)
            Struct.kSizeBool        // state
        )

        override fun getSchema() = (
            "enum{input=0,output=1}int8 mode; " +
            "bool state"
        )

        override fun unpack(bb: ByteBuffer): DigitalChannelData {
            return DigitalChannelData(
                mode = DigitalChannel.Mode.entries[bb.get().toInt()],
                state = bb.get() == 1.toByte()
            )
        }

        override fun pack(bb: ByteBuffer, value: DigitalChannelData) {
            with(value) {
                bb.put(mode.ordinal.toByte())
                bb.put((if (state) 1 else 0).toByte())
            }
        }
    }
    companion object {
        val empty = DigitalChannelData(DigitalChannel.Mode.INPUT, false)
    }
}
