package org.firstinspires.ftc.teamcode.fakehardware

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxUsbDevice
import com.qualcomm.hardware.lynx.LynxUsbDeviceImpl
import com.qualcomm.hardware.lynx.commands.LynxMessage
import com.qualcomm.robotcore.eventloop.SyncdDevice
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.LynxModuleDescription
import com.qualcomm.robotcore.hardware.LynxModuleMetaList
import com.qualcomm.robotcore.hardware.usb.RobotArmingStateNotifier
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule
import com.qualcomm.robotcore.util.SerialNumber
import org.firstinspires.ftc.robotcore.external.Consumer
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList
import org.firstinspires.ftc.robotcore.internal.ui.ProgressParameters
import java.util.concurrent.TimeUnit

class FakeLynxUsbDevice: LynxUsbDevice {
    override fun getRobotUsbDevice(): RobotUsbDevice? {
        TODO("Not yet implemented")
    }

    override fun isSystemSynthetic(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setSystemSynthetic(systemSynthetic: Boolean) {
        TODO("Not yet implemented")
    }

    override fun failSafe() {
        TODO("Not yet implemented")
    }

    override fun changeModuleAddress(
        module: LynxModule?,
        oldAddress: Int,
        runnable: Runnable?
    ) {
        TODO("Not yet implemented")
    }

    override fun getOrAddModule(moduleDescription: LynxModuleDescription?): LynxModule? {
        TODO("Not yet implemented")
    }

    override fun removeConfiguredModule(module: LynxModule?) {
        TODO("Not yet implemented")
    }

    override fun noteMissingModule(
        moduleAddress: Int,
        moduleName: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun performSystemOperationOnParentModule(
        parentAddress: Int,
        operation: Consumer<LynxModule?>?,
        timeout: Int,
        timeoutUnit: TimeUnit?
    ) {
        TODO("Not yet implemented")
    }

    override fun performSystemOperationOnConnectedModule(
        moduleAddress: Int,
        parentAddress: Int,
        operation: Consumer<LynxModule?>?,
        timeout: Int,
        timeoutUnit: TimeUnit?
    ) {
        TODO("Not yet implemented")
    }

    override fun keepConnectedModuleAliveForSystemOperations(
        moduleAddress: Int,
        parentAddress: Int
    ): LynxUsbDevice.SystemOperationHandle? {
        TODO("Not yet implemented")
    }

    override fun discoverModules(checkForImus: Boolean): LynxModuleMetaList? {
        TODO("Not yet implemented")
    }

    override fun acquireNetworkTransmissionLock(message: LynxMessage) { }

    override fun releaseNetworkTransmissionLock(message: LynxMessage) { }

    override fun transmit(message: LynxMessage?) { }

    override fun setupControlHubEmbeddedModule(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getDelegationTarget(): LynxUsbDeviceImpl? {
        TODO("Not yet implemented")
    }

    override fun updateFirmware(
        image: RobotCoreCommandList.FWImage?,
        requestId: String?,
        progressConsumer: Consumer<ProgressParameters?>?
    ): RobotCoreCommandList.LynxFirmwareUpdateResp? {
        TODO("Not yet implemented")
    }

    override fun arm() { }
    override fun pretend() { }

    override fun armOrPretend() { }

    override fun disarm() { }

    override fun close() { }

    override fun getSerialNumber() = object : SerialNumber("1") { }

    override fun getArmingState(): RobotArmingStateNotifier.ARMINGSTATE? {
        TODO("Not yet implemented")
    }

    override fun registerCallback(
        callback: RobotArmingStateNotifier.Callback?,
        doInitialCallback: Boolean
    ) { }

    override fun unregisterCallback(callback: RobotArmingStateNotifier.Callback?) {
        TODO("Not yet implemented")
    }

    override fun getGlobalWarning(): String? {
        TODO("Not yet implemented")
    }

    override fun shouldTriggerWarningSound(): Boolean {
        TODO("Not yet implemented")
    }

    override fun suppressGlobalWarning(suppress: Boolean) { }

    override fun setGlobalWarning(warning: String?) { }

    override fun clearGlobalWarning() { }

    override fun lockNetworkLockAcquisitions() { }

    override fun setThrowOnNetworkLockAcquisition(shouldThrow: Boolean) { }

    override fun getManufacturer(): HardwareDevice.Manufacturer? {
        TODO("Not yet implemented")
    }

    override fun getDeviceName(): String? {
        TODO("Not yet implemented")
    }

    override fun getConnectionInfo(): String? {
        TODO("Not yet implemented")
    }

    override fun getVersion(): Int {
        TODO("Not yet implemented")
    }

    override fun resetDeviceConfigurationForOpMode() { }

    override fun getShutdownReason(): SyncdDevice.ShutdownReason? {
        TODO("Not yet implemented")
    }

    override fun setOwner(owner: RobotUsbModule?) { }

    override fun getOwner(): RobotUsbModule? {
        TODO("Not yet implemented")
    }

    override fun disengage() { }

    override fun engage() { }

    override fun isEngaged(): Boolean {
        TODO("Not yet implemented")
    }
}