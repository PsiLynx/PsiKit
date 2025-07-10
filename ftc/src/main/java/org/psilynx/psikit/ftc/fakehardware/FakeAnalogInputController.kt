package org.psilynx.psikit.ftc.fakehardware

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.AnalogInputController
import com.qualcomm.robotcore.util.SerialNumber
import java.lang.reflect.Field

class FakeAnalogInputController(
    val module: LynxModule
): AnalogInputController, FakeHardware {
    val lynxFields: Array<out Field> = LynxModule::class.java.fields

    val bulkCachingLock = lynxFields.firstOrNull {
        it.name == "bulkCachingLock"
    }!!

    val lastBulkData = lynxFields.firstOrNull {
        it.name == "lastBulkData"
    }!!
    init {
        bulkCachingLock.isAccessible = true
        lastBulkData.isAccessible = true
    }

    override fun getAnalogInputVoltage(channel: Int): Double {
        synchronized(bulkCachingLock.get(module)!!) {
            val bulkData = lastBulkData.get(module)!! as LynxModule.BulkData
            return bulkData.getAnalogInputVoltage(channel)
        }
    }
    override fun getMaxAnalogInputVoltage(): Double { TODO("Not yet implemented")}
    override fun getSerialNumber(): SerialNumber? { TODO("Not yet implemented") }
    override fun resetDeviceConfigurationForOpMode() { }

    override fun update(deltaTime: Double) { }

}