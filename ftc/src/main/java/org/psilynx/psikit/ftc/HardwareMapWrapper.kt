package org.psilynx.psikit.ftc

import com.qualcomm.hardware.sparkfun.SparkFunOTOS
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.hardware.limelightvision.Limelight3A
import com.qualcomm.robotcore.hardware.AccelerationSensor
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.ColorSensor
import com.qualcomm.robotcore.hardware.CompassSensor
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorController
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.DistanceSensor
import com.qualcomm.robotcore.hardware.GyroSensor
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.I2cDevice
import com.qualcomm.robotcore.hardware.I2cDeviceSynch
import com.qualcomm.robotcore.hardware.IrSeekerSensor
import com.qualcomm.robotcore.hardware.LED
import com.qualcomm.robotcore.hardware.LightSensor
import com.qualcomm.robotcore.hardware.IMU
import com.qualcomm.robotcore.hardware.NormalizedRGBA
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor
import com.qualcomm.robotcore.hardware.PWMOutput
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoController
import com.qualcomm.robotcore.hardware.ServoImplEx
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
import org.psilynx.psikit.ftc.wrappers.ImuWrapper
import org.psilynx.psikit.ftc.wrappers.Limelight3AWrapper
import org.psilynx.psikit.ftc.wrappers.MotorWrapper
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxUsbDevice
import org.psilynx.psikit.ftc.wrappers.PinpointWrapper
import org.psilynx.psikit.ftc.wrappers.ServoWrapper
import org.psilynx.psikit.ftc.wrappers.SparkFunOTOSWrapper
import org.psilynx.psikit.ftc.wrappers.VoltageSensorWrapper
import org.psilynx.psikit.ftc.wrappers.ColorDistanceSensorWrapper
import java.lang.reflect.Proxy
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
        mutableMapOf<Class<out HardwareDevice>, HardwareInput<out HardwareDevice>>(
            // FTC SDK's Pinpoint driver (SDK 11+)
            GoBildaPinpointDriver::class.java to PinpointWrapper(null),

            Limelight3A::class.java            to Limelight3AWrapper(null),

            DigitalChannel::class.java        to DigitalChannelWrapper(null),
            VoltageSensor::class.java         to VoltageSensorWrapper(null),
            SparkFunOTOS::class.java          to SparkFunOTOSWrapper(null),
            AnalogInput::class.java           to AnalogInputWrapper(null),

            IMU::class.java                   to ImuWrapper(null),

            // Sensors commonly retrieved via interfaces (e.g. RevColorSensorV3).
            ColorSensor::class.java           to ColorDistanceSensorWrapper(null),
            NormalizedColorSensor::class.java to ColorDistanceSensorWrapper(null),
            DistanceSensor::class.java        to ColorDistanceSensorWrapper(null),

            // Support concrete retrieval too (some teams prefer to lock sensor type).
            RevColorSensorV3::class.java      to ColorDistanceSensorWrapper(null),

            CRServo::class.java               to CrServoWrapper(null),
            DcMotor::class.java               to MotorWrapper(null),
            DcMotorEx::class.java             to MotorWrapper(null),
            Servo::class.java                 to ServoWrapper(null),
        )

    init {
        // Some user code accesses DeviceMappings directly (e.g. hardwareMap.voltageSensor.iterator().next()).
        // When we wrap the HardwareMap, those mappings start empty unless populated. Copy over the
        // backing voltage sensors so common patterns keep working on-robot.
        try {
            val map = hardwareMap
            if (map != null) {
                for (entry in map.voltageSensor.entrySet()) {
                    if (entry.key != null && entry.value != null) {
                        this.voltageSensor.put(entry.key, entry.value)
                    }
                }

                // Some FTC environments (Robolectric, unit tests) provide a HardwareMap but no
                // voltage sensors. In replay, keep common access patterns from crashing.
                if (Logger.isReplay() && !this.voltageSensor.iterator().hasNext()) {
                    ensureReplayVoltageSensor()
                }
            } else if (Logger.isReplay()) {
                // Replay often runs without a real FTC HardwareMap. Provide a stable, non-empty
                // voltage sensor mapping so patterns like `hardwareMap.voltageSensor.iterator().next()`
                // don't crash during replay.
                ensureReplayVoltageSensor()
            }
        } catch (_: Throwable) {
            // Best-effort: if SDK internals change, don't crash during init.
        }
    }

    private fun ensureReplayVoltageSensor() {
        val replayName = "__psikit_replay_voltage"
        if (!this.voltageSensor.entrySet().any { it.key == replayName }) {
            val vs = VoltageSensorWrapper(null)
            this.voltageSensor.put(replayName, vs)
        }
    }

    private val replayConcreteDeviceCache = mutableMapOf<String, HardwareDevice>()

    private fun getOrCreateReplayLynxModule(name: String): LynxModule {
        val cached = replayConcreteDeviceCache[name]
        if (cached is LynxModule) return cached

        val fakeUsb = Proxy.newProxyInstance(
            LynxUsbDevice::class.java.classLoader,
            arrayOf(LynxUsbDevice::class.java)
        ) { proxy, method, _ ->
            when (method.name) {
                "toString" -> "ReplayProxy(${LynxUsbDevice::class.java.name}:$name)"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> false
                "getSerialNumber" -> SerialNumber.createFake()
                "getDeviceName" -> "ReplayLynxUsbDevice"
                "getConnectionInfo" -> "(replay)"
                "getVersion" -> 1
                "getManufacturer" -> HardwareDevice.Manufacturer.Other
                else -> {
                    val rt = method.returnType
                    when {
                        rt == java.lang.Boolean.TYPE -> false
                        rt == java.lang.Byte.TYPE -> 0.toByte()
                        rt == java.lang.Short.TYPE -> 0.toShort()
                        rt == java.lang.Integer.TYPE -> 0
                        rt == java.lang.Long.TYPE -> 0L
                        rt == java.lang.Float.TYPE -> 0.0f
                        rt == java.lang.Double.TYPE -> 0.0
                        rt == java.lang.Character.TYPE -> 0.toChar()
                        rt == String::class.java -> ""
                        rt.isEnum -> rt.enumConstants?.firstOrNull()
                        rt.isArray -> java.lang.reflect.Array.newInstance(rt.componentType, 0)
                        else -> null
                    }
                }
            }
        } as LynxUsbDevice

        val module = LynxModule(
            fakeUsb,
            /* moduleAddress */ 1,
            /* isParent */ true,
            /* isUserModule */ true,
        )
        replayConcreteDeviceCache[name] = module
        return module
    }
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
                // Avoid spamming warnings in replay: many requested devices are absent and come
                // through as null. Only warn (once per type) for real, unknown device types.
                if (device != null) {
                    val typeName = try {
                        device::class.qualifiedName
                    } catch (_: Throwable) {
                        null
                    } ?: device.javaClass.name

                    if (warnedUnknownDeviceTypes.add(typeName)) {
                        Logger.logWarning(
                            "device type $typeName not in all device mappings"
                        )
                    }
                }
            }
        }
        device as HardwareDevice?
        val wrapperTemplate = (
            deviceWrappers[classOrInterface as Class<HardwareDevice>]
            as? HardwareInput<HardwareDevice>
        )

        val wrapper = when (wrapperTemplate) {
            is MotorWrapper -> {
                // DcMotorEx is usually DcMotorImplEx in the FTC SDK, but tests/fakes may not be.
                // Avoid ClassCastException by only wrapping when the implementation matches.
                if (device == null || device is DcMotorImplEx) wrapperTemplate.new(device as? DcMotorImplEx)
                else null
            }
            is ServoWrapper -> {
                if (device == null || device is ServoImplEx) wrapperTemplate.new(device as? ServoImplEx)
                else null
            }
            is CrServoWrapper -> {
                if (device == null || device is CRServoImplEx) wrapperTemplate.new(device as? CRServoImplEx)
                else null
            }
            else -> wrapperTemplate?.new(device)
        }


        Logger.logInfo("hardwaremap call on $classOrInterface, got " +
                "wrapper ${wrapper?.javaClass?.canonicalName}")
        if (wrapper != null) {
            if (wrapper is MotorWrapper) {
                wrapper.psikitName = name
            }
            if (wrapper is PinpointWrapper) {
                wrapper.psikitName = name
            }
            devicesToProcess.put(name, wrapper)

            // Important: if the user asked for a concrete class (not an interface), we cannot
            // safely return an arbitrary wrapper unless it is actually an instance of that class.
            // In that case, return the underlying device to preserve type correctness, while
            // still logging via the wrapper stored in devicesToProcess.
            if (classOrInterface != null) {
                if (classOrInterface.isInstance(wrapper)) {
                    @Suppress("UNCHECKED_CAST")
                    return wrapper as T
                }

                if (!classOrInterface.isInterface && device != null && classOrInterface.isInstance(device)) {
                    @Suppress("UNCHECKED_CAST")
                    return device as T
                }
            }

            @Suppress("UNCHECKED_CAST")
            return wrapper as T
        }
        if (device != null) return device
        else {
            // Special-case: some teams use LynxModule directly for bulk caching / module info.
            // In replay, we may not have a real REV hub object. Provide a safe synthesized
            // LynxModule instance so code can keep running.
            if (Logger.isReplay() && classOrInterface == LynxModule::class.java) {
                @Suppress("UNCHECKED_CAST")
                return getOrCreateReplayLynxModule(name) as T
            }

            // In replay, we often don't have physical hardware. If the requested type is an
            // interface (common for sensors), return a proxy with safe defaults so robot logic can
            // continue running.
            if (Logger.isReplay() && classOrInterface != null && classOrInterface.isInterface) {
                @Suppress("UNCHECKED_CAST")
                return Proxy.newProxyInstance(
                    classOrInterface.classLoader,
                    arrayOf(classOrInterface)
                ) { proxy, method, _ ->
                    when (method.name) {
                        "toString" -> "ReplayProxy(${classOrInterface.name}:$name)"
                        "hashCode" -> System.identityHashCode(proxy)
                        "equals" -> false
                        "getDeviceName" -> name
                        // Common FTC pattern: NormalizedColorSensor.getNormalizedColors() must never be null.
                        "getNormalizedColors" -> NormalizedRGBA()
                        else -> {
                            val rt = method.returnType
                            when {
                                rt == java.lang.Boolean.TYPE -> false
                                rt == java.lang.Byte.TYPE -> 0.toByte()
                                rt == java.lang.Short.TYPE -> 0.toShort()
                                rt == java.lang.Integer.TYPE -> 0
                                rt == java.lang.Long.TYPE -> 0L
                                rt == java.lang.Float.TYPE -> 0.0f
                                rt == java.lang.Double.TYPE -> 0.0
                                rt == java.lang.Character.TYPE -> 0.toChar()
                                rt == String::class.java -> ""
                                rt == NormalizedRGBA::class.java -> NormalizedRGBA()
                                rt.isEnum -> rt.enumConstants?.firstOrNull()
                                rt.isArray -> java.lang.reflect.Array.newInstance(rt.componentType, 0)
                                else -> null
                            }
                        }
                    }
                } as T
            }

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
        val map = hardwareMap

        // Replay often runs without a real FTC HardwareMap. Special-case common mappings
        // that teams rely on, even when no backing HardwareMap exists.
        if (map == null) {
            if (Logger.isReplay() && classOrInterface == VoltageSensor::class.java) {
                @Suppress("UNCHECKED_CAST")
                return listOf(get(classOrInterface, "__psikit_replay_voltage"))
            }
            return emptyList()
        }

        // Deterministic ordering: HardwareMap.getNamesOf() returns a Set, whose iteration order is
        // not guaranteed. Sort names and choose the first stable name for each device.
        val rawDevices = map.getAll(classOrInterface)

        val devicesWithNames = rawDevices.map { device ->
            val hw = device as? HardwareDevice
            val stableName = if (hw != null) {
                val names = map.getNamesOf(hw)
                    .filterNotNull()
                    .sorted()
                names.firstOrNull() ?: hw.deviceName ?: "None"
            } else {
                "None"
            }
            stableName to device
        }
            .sortedWith(
                compareBy<Pair<String, T>> { it.first }
                    .thenBy { (it.second as? HardwareDevice)?.deviceName ?: "" }
                    .thenBy { it.second::class.java.name }
            )

        return devicesWithNames.map { (name, device) ->
            wrap(classOrInterface, name, device)
        }
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

    companion object {
        internal val devicesToProcess = mutableMapOf<String, LoggableInputs>()
        private val warnedUnknownDeviceTypes = mutableSetOf<String>()
    }
}