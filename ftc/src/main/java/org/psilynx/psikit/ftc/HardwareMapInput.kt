package org.psilynx.psikit.ftc

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.HardwareMap
import org.psilynx.psikit.ftc.fakehardware.FakeLynxGetBulkInputDataResponse
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs
import kotlin.reflect.jvm.isAccessible

class HardwareMapInput(
    val hardwareMap: HardwareMap?,
    val readControlHub: Boolean,
    val readExpansionHub: Boolean
):
    LoggableInputs {
    val lynxFields = LynxModule::class.java.fields

    val bulkCachingLock = lynxFields.firstOrNull {
        it.name == "bulkCachingLock"
    }!!

    val lastBulkData = lynxFields.firstOrNull {
        it.name == "lastBulkData"
    }!!

    val bulkDataConstructor
        = LynxModule.BulkData::class.constructors.first()

    init {
        bulkCachingLock.isAccessible = true
        lastBulkData.isAccessible = true
        bulkDataConstructor.isAccessible = true;
    }

    override fun toLog(table: LogTable?) {
        val lynxModules = hardwareMap?.getAll(LynxModule::class.java)
        for(module in lynxModules!!){
            val isControl = module.isParent
            if(!(
                   ( module.isParent && readControlHub )
                || (!module.isParent && readExpansionHub)
            )) continue;
            table!!.apply { synchronized(bulkCachingLock.get(module)!!) {
                if(
                    module.bulkCachingMode
                    != LynxModule.BulkCachingMode.MANUAL
                ) module.bulkCachingMode =
                    LynxModule.BulkCachingMode.MANUAL

                val data = module.bulkData

                //put(isControl, "revProductNumer", module.revProductNumber)
                //put(isControl, "moduleAddresss", module.moduleAddress)
                //put(isControl, "serialNumber", module.serialNumber)
                //put(isControl, "isUserModule", module.isUserModule)
                //put(isControl, "deviceName", module.deviceName)
                //put(isControl, "isUserModule", module.imuType)
                //put(isControl, "isParent", module.isParent)
                //put(isControl, "version", module.version)
                put(isControl, "isFake", data.isFake)
                put(
                    isControl,
                    "firmwareVersionString",
                    module.firmwareVersionString
                )
                put(
                    isControl,
                    "isPhoneChargingEnabled",
                    module.isPhoneChargingEnabled
                )
                put(
                    isControl,
                    "moduleSerialNumber",
                    module.moduleSerialNumber
                )
                put(
                    isControl,
                    "Quadrature",
                    (0..3).map { data.getMotorCurrentPosition(it) }.toTypedArray()
                )
                put(
                    isControl,
                    "QuadratureVelocity",
                    (0..3).map { data.getMotorVelocity(it) }.toTypedArray()
                )
                put(
                    isControl,
                    "IsBusy",
                    (0..3).map { data.isMotorBusy(it) }.toTypedArray()
                )
                put(
                    isControl,
                    "OverCurrent",
                    (0..3).map { data.isMotorOverCurrent(it) }.toTypedArray()
                )
                put(
                    isControl,
                    "Analog",
                    (0..3).map { data.getAnalogInputVoltage(it) }.toTypedArray()
                )
                put(
                    isControl,
                    "Digital",
                    (0..7).map { data.getDigitalChannelState(it) }.toTypedArray()
                )
            }}
        }
    }

    override fun fromLog(table: LogTable?) {
        val lynxModules = hardwareMap?.getAll(LynxModule::class.java)
        for(module in lynxModules!!){
            val isControl = module.isParent
            table!!.apply { synchronized(bulkCachingLock.get(module)!!) {
                val response = FakeLynxGetBulkInputDataResponse(
                    module,
                    get(isControl, "Quadrature", arrayOf<Int>()),
                    get(isControl, "QuadratureVelocity", arrayOf<Int>()),
                    get(isControl, "IsBusy", arrayOf<Boolean>()),
                    get(isControl, "OverCurrent", arrayOf<Boolean>()),
                    get(isControl, "Analog", arrayOf<Int>()),
                    get(isControl, "Digital", arrayOf<Boolean>()),
                )
                val bulkData = bulkDataConstructor.call(
                    get(isControl, "isFake", false)
                )
                lastBulkData.set(module, bulkData)
            }}
        }
    }
    private inline fun <reified T> LogTable.get(
        isControl: Boolean,
        name: String,
        defualtValue: T
    ): T{
        val key = (
                ( if (isControl) "Control" else "Expansion" )
                        + " Hub/" + name
                )
        return when(defualtValue){
            is Int -> this.get(key, defualtValue)
            is Double -> this.get(key, defualtValue)
            is String -> this.get(key, defualtValue)
            is Boolean -> this.get(key, defualtValue)
            else -> error("T must be Int, Double, Boolean, or String")
        } as T
    }
    private inline fun <reified T> LogTable.get(
        isControl: Boolean,
        name: String,
        defualtValue: Array<T>
    ): Array<T>{
        val key = (
                ( if (isControl) "Control" else "Expansion" )
                        + " Hub/" + name
                )
        return when(T::class){
            Int::class ->
                this.get(key, (defualtValue as Array<Int>).toIntArray())
            Double::class ->
                this.get(key, (defualtValue as Array<Double>).toDoubleArray())
            Boolean::class ->
                this.get(key, (defualtValue as Array<Boolean>).toBooleanArray())
            String::class ->
                this.get(key, (defualtValue as Array<String>))
            else -> error("T must be Int, Double, Boolean, or String")
        } as Array<T>
    }
    private inline fun <reified T> LogTable.put(
        isControl: Boolean,
        name: String,
        data: T
    ){
        val key = (
            ( if (isControl) "Control" else "Expansion" )
            + " Hub/" + name
        )
        when(data){
            is Int -> this.put(key, data)
            is Double -> this.put(key, data)
            is String -> this.put(key, data)
            is Boolean -> this.put(key, data)
            else -> error("T must be Int, Double, Boolean, or String")
        }
    }
    private inline fun <reified T> LogTable.put(
        isControl: Boolean,
        name: String,
        data: Array<T>
    ){
        val key = (
            ( if (isControl) "Control" else "Expansion" )
            + " Hub/" + name
        )
        when(T::class){
            Int::class ->
                this.put(key, (data as Array<Int>).toIntArray())
            Double::class ->
                this.put(key, (data as Array<Double>).toDoubleArray())
            Boolean::class ->
                this.put(key, (data as Array<Boolean>).toBooleanArray())
            String::class ->
                this.put(key, (data as Array<String>))
            else -> error("T must be Int, Double, Boolean, or String")
        } as T
    }
}