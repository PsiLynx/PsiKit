package org.psilynx.psikit.ftc

import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.hardware.HardwareMap
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.wpi.Struct
import org.psilynx.psikit.core.wpi.StructSerializable
import org.psilynx.psikit.core.wpi.WPISerializable
import java.nio.ByteBuffer
import kotlin.collections.flatten

class HardwareMapInput(
    val hardwareMap: HardwareMap?,
    val readControlHub: Boolean,
    val readExpansionHub: Boolean
): LoggableInputs, HardwareMap(null, OpModeManagerImpl(null, null)){
    override fun <T : Any?> getAll(classOrInterface: Class<out T?>?) =
        allDeviceMappings.first { it.deviceTypeClass
            .isAssignableFrom(classOrInterface!!)}.toList() as List<T>


    override fun toLog(table: LogTable?) {
        val motors = arrayListOf<String>()
        hardwareMap.dcMotorController.forEach { it. }

        table.put("allDeviceMappings", this.allDeviceMappings.map { mapping ->
            mapping.deviceTypeClass.toString() +
            mapping.map {
                it.
            }

        }.flatten().toTypedArray()
    }

    override fun fromLog(table: LogTable?) {

    }
    class DeviceMappings: StructSerializable{
        val struct = DeviceMappingsStruct()
    }
    class DeviceMappingsStruct: Struct<DeviceMappings>{
        override fun getTypeClass() = DeviceMappings::class.java

        override fun getTypeName() = "deviceMappings"

        override fun getSize(): Int {
            TODO("Not yet implemented")
        }

        override fun getSchema(): String? {
            TODO("Not yet implemented")
        }

        override fun unpack(bb: ByteBuffer?): DeviceMappings? {
            TODO("Not yet implemented")
        }

        override fun pack(
            bb: ByteBuffer?,
            value: DeviceMappings?
        ) {
            TODO("Not yet implemented")
        }

    }
}