package org.psilynx.psikit.ftc.structs

import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.DigitalChannelController
import com.qualcomm.robotcore.hardware.HardwareDevice
import org.psilynx.psikit.core.wpi.Struct
import java.nio.ByteBuffer

class DigitalChannelData(
    val mode: DigitalChannel.Mode,
    val state: Boolean,
) : HardwareData {

    override val device = Device(this)

    class Device(var thisRef: DigitalChannelData) : DigitalChannel {
        override fun getMode() = thisRef.mode
        override fun setMode(mode: DigitalChannel.Mode) {}
        override fun setMode(mode: DigitalChannelController.Mode) {}
        override fun getState() = thisRef.state
        override fun setState(state: Boolean) {}

        override fun getManufacturer() = HardwareDevice.Manufacturer.Other
        override fun getDeviceName() = "MockDigitalChannel"
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
            Struct.kSizeInt8        // state
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
        @JvmField
        val struct = DigitalChannelDataStruct()
    }
}
