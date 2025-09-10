package org.psilynx.psikit.ftc

import com.qualcomm.hardware.sparkfun.SparkFunOTOS
import com.qualcomm.robotcore.hardware.AccelerationSensor
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.ColorSensor
import com.qualcomm.robotcore.hardware.CompassSensor
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorController
import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.GyroSensor
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.I2cDevice
import com.qualcomm.robotcore.hardware.I2cDeviceSynch
import com.qualcomm.robotcore.hardware.IrSeekerSensor
import com.qualcomm.robotcore.hardware.LED
import com.qualcomm.robotcore.hardware.LightSensor
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor
import com.qualcomm.robotcore.hardware.PWMOutput
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoController
import com.qualcomm.robotcore.hardware.TouchSensor
import com.qualcomm.robotcore.hardware.TouchSensorMultiplexer
import com.qualcomm.robotcore.hardware.UltrasonicSensor
import com.qualcomm.robotcore.hardware.VoltageSensor
import com.qualcomm.robotcore.util.SerialNumber
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.wrappers.AnalogInputWrapper
import org.psilynx.psikit.ftc.wrappers.CrServoWrapper
import org.psilynx.psikit.ftc.wrappers.DigitalChannelWrapper
import org.psilynx.psikit.ftc.wrappers.HardwareInput
import org.psilynx.psikit.ftc.wrappers.MotorWrapper
import org.psilynx.psikit.ftc.wrappers.PinpointWrapper
import org.psilynx.psikit.ftc.wrappers.ServoWrapper
import org.psilynx.psikit.ftc.wrappers.SparkFunOTOSWrapper
import org.psilynx.psikit.ftc.wrappers.VoltageSensorWrapper
import java.util.SortedSet
import java.util.Spliterator
import java.util.function.Consumer

class HardwareMapWrapper(
    val hardwareMap: HardwareMap?
): HardwareMap(
    hardwareMap?.appContext,
    null
){
    /*
     * map of HardwareDevice classes to Inputs that wrap them. users should not
     * have to use this directly unless they are using an i2c device that
     * doesn't have support yet, in which case, they should look at the
     * PinpointInput as an example.
     */
    val deviceWrappers =
        mapOf<Class<out HardwareDevice>, HardwareInput<out HardwareDevice>>(
            GoBildaPinpointDriver::class.java to PinpointWrapper(null),

            DigitalChannel::class.java        to DigitalChannelWrapper(null),
            VoltageSensor::class.java         to VoltageSensorWrapper(null),
            SparkFunOTOS::class.java          to SparkFunOTOSWrapper(null),
            AnalogInput::class.java           to AnalogInputWrapper(null),
            CRServo::class.java               to CrServoWrapper(null),
            DcMotor::class.java               to MotorWrapper(null),
            Servo::class.java                 to ServoWrapper(null),
        )
    internal val devicesToProcess = mutableMapOf<String, LoggableInputs>()
    /*
    init {
        this.allDeviceMappings.forEach { mapping ->
            this.getAll(mapping.deviceTypeClass)
            // this makes all the wrappers get put into the device mappings,
            // because each get calls the wrap command. this means that users
            // can use hardwaremap.<mapping> to get devices if they want to,
            // and they will still be wrapped
        }

    }
     */

    private fun <T : Any> wrap(
        classOrInterface: Class<out T?>?,
        name: String,
        device: T?
    ): T {
        if(device is HardwareInput<*>) return device
        if(device !is HardwareDevice && device != null) Logger.logCritical(
            "tried to get something from the hardwaremap that doesn't extend"
            + " HardwareDevice"
        )

        // this puts the device into the device mappings
        when (device) {
            is TouchSensorMultiplexer -> this.touchSensorMultiplexer.put(
                name, device
            )
            is OpticalDistanceSensor -> this.opticalDistanceSensor.put(
                name, device
            )
            is AccelerationSensor -> this.accelerationSensor.put( name, device )
            is DcMotorController -> this.dcMotorController.put( name, device )
            is UltrasonicSensor -> this.ultrasonicSensor.put( name, device )
            is ServoController -> this.servoController.put( name, device )
            is DigitalChannel -> this.digitalChannel.put( name, device )
            is IrSeekerSensor -> this.irSeekerSensor.put( name, device )
            is I2cDeviceSynch -> this.i2cDeviceSynch.put( name, device )
            is VoltageSensor -> this.voltageSensor.put( name, device )
            is CompassSensor -> this.compassSensor.put( name, device )
            is AnalogInput -> this.analogInput.put( name, device )
            is TouchSensor -> this.touchSensor.put( name, device )
            is ColorSensor -> this.colorSensor.put( name, device )
            is LightSensor -> this.lightSensor.put( name, device )
            is GyroSensor -> this.gyroSensor.put( name, device )
            is PWMOutput -> this.pwmOutput.put( name, device )
            is I2cDevice -> this.i2cDevice.put( name, device )
            is DcMotor -> this.dcMotor.put( name, device )
            is CRServo -> this.crservo.put( name, device )
            is Servo -> this.servo.put( name, device )
            is LED -> this.led.put( name, device )
            else -> {
                Logger.logWarning(
                    "device type ${device?.apply { this::class.qualifiedName }}"
                    + " not in all device mappings"
                )
            }
        }
        device as HardwareDevice?
        val wrapper = (
            deviceWrappers[classOrInterface as Class<HardwareDevice>]
            as? HardwareInput<HardwareDevice>
        ) ?.new(device)


        Logger.logInfo("hardwaremap call on $classOrInterface, got " +
                "wrapper ${wrapper?.javaClass?.canonicalName}")
        if (wrapper != null) {
            devicesToProcess.put(name, wrapper)
            return wrapper as T
        }
        if (device != null) return device
        else {
            Logger.logCritical(
                "device to wrap is null, and no wrapper can be found." +
                " exiting with error"
            )
            error("")
        }
    }

    override fun <T : Any> get(
        classOrInterface: Class<out T?>?,
        deviceName: String
    ) = wrap(
        classOrInterface,
        deviceName,
        hardwareMap?.get<T>(classOrInterface, deviceName)
    )

    override fun <T : Any> getAll(classOrInterface: Class<out T>): List<T> {

        Logger.logError(
            "method getAll not wrapped correctly, it is very "
            + "likely that using this will break determinism"
        )
        return hardwareMap?.getAll(classOrInterface)?.map {
            val name = getNamesOf(it as HardwareDevice).first()
            if(name == null) {
                Logger.logError(
                    "couldn't get a name for ${it::class.qualifiedName}"
                )
            };
            wrap(
                classOrInterface,
                name ?: "None",
                it
            )
        } ?: listOf()
    }

    override fun get(deviceName: String): HardwareDevice? {
        Logger.logError(
            "method get (without a class) not wrapped correctly, it is very "
            + "likely that using this will break determinism"
        )

        val device = hardwareMap?.get(deviceName)
        if(device == null) return null

        return wrap(device::class.java, deviceName, device)
    }

    override fun forEach(action: Consumer<in HardwareDevice>) {
        hardwareMap?.forEach(action)
    }

    override fun spliterator(): Spliterator<HardwareDevice?> {
        Logger.logError(
            "method spliterator not wrapped correctly, it is very "
            + "likely that using this will break determinism"
            + " I'm gonna be real, I have no idea what a \"Spliterator\" is "
            + "or why I should waste my time implementing it"
        )
        if(hardwareMap == null) error(
            "okay you can't even get the spliterator in replay"
        )
        return hardwareMap.spliterator()
    }

    override fun getAllNames(classOrInterface: Class<out HardwareDevice?>?): SortedSet<String?>? {
        Logger.logError(
            "method getAllNames not wrapped correctly, it is very "
            + "likely that using this will break determinism"
        )
        return hardwareMap?.getAllNames(classOrInterface) ?: sortedSetOf()
    }

    override fun getNamesOf(device: HardwareDevice?): Set<String?> {
        Logger.logWarning(
            "you used hardwaremap.getNamesOf, this does not 100% guarantee "
            + "determinsism (also like what are you even using it for"
        )
        return hardwareMap?.getNamesOf(device) ?: setOf(device?.deviceName)
    }

    override fun <T : Any?> get(
        classOrInterface: Class<out T?>?,
        serialNumber: SerialNumber?
    ): T {

        val device = hardwareMap?.get(classOrInterface, serialNumber)
        val name = getNamesOf(device as? HardwareDevice).first()
        if(name == null) {
            Logger.logError(
                "couldn't get a name for ${
                    device?.apply { this::class .qualifiedName}
                }"
            )
        };
        return wrap(
            classOrInterface,
            name ?: "None",
            device
        )
    }

    override fun iterator(): MutableIterator<HardwareDevice?> {
        Logger.logError(
            "method iterator not wrapped correctly, it is very "
            + "likely that using this will break determinism"
        )
        if(hardwareMap == null) error(
            "okay you can't even get the iterator in replay"
        )
        return hardwareMap.iterator()
    }

    override fun toString(): String {
        return hardwareMap?.toString() ?: super.toString()
    }

    override fun <T : Any> tryGet(
        classOrInterface: Class<out T>,
        deviceName: String
    ): T? {
        val device = hardwareMap?.tryGet<T>(classOrInterface, deviceName)
        return (
            if(hardwareMap == null || device != null) wrap(
                classOrInterface,
                deviceName,
                device
            ) as T?

            else null
        )
    }

}