package org.firstinspires.ftc.teamcode.fakehardware

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxModule.BulkData
import com.qualcomm.hardware.lynx.commands.LynxMessage
import com.qualcomm.hardware.lynx.commands.core.LynxGetBulkInputDataResponse
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

class FakeLynxModule(isParent: Boolean): LynxModule(
    FakeLynxUsbDevice(),
    0,
    isParent,
    false
){
    override fun sendCommand(command: LynxMessage?) {

    }

    override fun getBulkData(): LynxModule.BulkData? {
        val constructor =  LynxModule.BulkData::class.constructors.first()
        constructor.isAccessible = true
        return constructor.call(
            LynxGetBulkInputDataResponse(
                this
            ),
            false
        )
    }
}