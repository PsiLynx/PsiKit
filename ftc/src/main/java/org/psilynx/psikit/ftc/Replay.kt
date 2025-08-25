package org.psilynx.psikit.ftc

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.hardware.rev.RevTouchSensor
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.CRServoImpl
import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorImpl
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoImpl
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.TouchSensor
import org.psilynx.psikit.core.LogReplaySource
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.wrappers.AnalogInputWrapper
import org.psilynx.psikit.ftc.wrappers.CrServoWrapper
import org.psilynx.psikit.ftc.wrappers.DigitalChannelWrapper
import org.psilynx.psikit.ftc.wrappers.GamepadWrapper
import org.psilynx.psikit.ftc.wrappers.MotorWrapper
import org.psilynx.psikit.ftc.wrappers.PinpointWrapper
import org.psilynx.psikit.ftc.wrappers.ServoWrapper

class Replay(val opMode: PsiKitOpMode, val replaySource: LogReplaySource) {
    fun run(){
        opMode.hardwareMap = object : HardwareMap(null, null) {
            override fun <T : Any?> get(
                classOrInterface: Class<out T?>?,
                deviceName: String?
            ) = when(classOrInterface){
                GoBildaPinpointDriver::class.java -> PinpointWrapper(null)

                DigitalChannel::class.java -> DigitalChannelWrapper(null)
                RevTouchSensor::class.java -> DigitalChannelWrapper(null)
                TouchSensor::class.java    -> DigitalChannelWrapper(null)

                AnalogInput::class.java    -> AnalogInputWrapper(null)

                CRServoImplEx::class.java  -> CrServoWrapper(null)
                CRServoImpl::class.java    -> CrServoWrapper(null)
                CRServo::class.java        -> CrServoWrapper(null)

                ServoImplEx::class.java    -> ServoWrapper(null)
                ServoImpl::class.java      -> ServoWrapper(null)
                Servo::class.java          -> ServoWrapper(null)

                DcMotorImplEx::class.java  -> MotorWrapper(null)
                DcMotorSimple::class.java  -> MotorWrapper(null)
                DcMotorImpl::class.java    -> MotorWrapper(null)
                DcMotor::class.java        -> MotorWrapper(null)

                else                       -> error(
                    "while replaying the OpMode " +
                    "\"${opMode::class.simpleName}\", it requested a " +
                    "${classOrInterface?.simpleName}, which is a " +
                    "device type that PsiKit doesn't auto log"
                )
            } as T
        }
        opMode.gamepad1 = GamepadWrapper(null)
        opMode.gamepad2 = GamepadWrapper(null)
        Logger.setReplay(true)
        Logger.setReplaySource(replaySource)
        opMode.runOpMode()

    }
}