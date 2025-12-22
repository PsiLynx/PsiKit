package org.psilynx.psikit.ftc

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.psilynx.psikit.core.Logger

/**
 * Logs goBILDA Pinpoint pose in AdvantageScope-friendly schemas for 2D/3D visualizations.
 *
 * Produces:
 * - `/Odometry/<deviceName>` with `Pose2d` and `Pose3d` struct fields
 * - `/Odometry` alias when there's exactly one Pinpoint configured
 */
class PinpointOdometryLogger {

    private data class NamedPinpoint(
        val name: String,
        val update: () -> Unit,
        val position: () -> Pose2D,
        val poses: StructPoseInputs,
    )

    private val cached = mutableListOf<NamedPinpoint>()
    private var cachedOnce = false

    private val robotAliases = StructPoseInputs("RobotPose", "RobotPose3d")

    fun logAll(hardwareMap: HardwareMap) {
        if (!cachedOnce) {
            cacheDevices(hardwareMap)
            cachedOnce = true
        }

        for (device in cached) {
            device.update()
            val pose = device.position()

            val xMeters = pose.getX(DistanceUnit.METER)
            val yMeters = pose.getY(DistanceUnit.METER)
            val headingRad = pose.getHeading(AngleUnit.RADIANS)

            device.poses.set(xMeters, yMeters, headingRad)
            Logger.processInputs("/Odometry/${device.name}", device.poses)

            if (cached.size == 1) {
                robotAliases.set(xMeters, yMeters, headingRad)
                Logger.processInputs("/Odometry", robotAliases)
            }
        }
    }

    private fun cacheDevices(hardwareMap: HardwareMap) {
        cached.clear()

        // 1) FTC SDK's goBILDA driver (2025+ SDKs): com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
        // Use reflection so PsiKit can still compile against older SDK variants.
        val sdkDevices = getAllByClassName(
            hardwareMap,
            "com.qualcomm.hardware.gobilda.GoBildaPinpointDriver",
        )
        for (device in sdkDevices) {
            val hw = device as? HardwareDevice
            val name = if (hw != null) firstNameOrFallback(hardwareMap, hw, "pinpoint") else "pinpoint"
            val updateFn = { invokeNoArg(device, "update") }
            val positionFn = { invokePosition(device) }
            cached.add(NamedPinpoint(name, updateFn, positionFn, StructPoseInputs("Pose2d", "Pose3d")))
        }

        // 2) PsiKit's embedded driver (still supported)
        val psikitDevices = hardwareMap.getAll(GoBildaPinpointDriver::class.java)
        for (device in psikitDevices) {
            val name = firstNameOrFallback(hardwareMap, device, "pinpoint")
            cached.add(
                NamedPinpoint(
                    name,
                    update = { device.update() },
                    position = { device.position },
                    poses = StructPoseInputs("Pose2d", "Pose3d"),
                )
            )
        }

        cached.sortBy { it.name }
    }

    private fun firstNameOrFallback(
        hardwareMap: HardwareMap,
        device: HardwareDevice,
        fallback: String,
    ): String {
        return try {
            val names = hardwareMap.getNamesOf(device)
            if (!names.isNullOrEmpty()) names.first() else fallback
        } catch (_: Throwable) {
            fallback
        }
    }

    private fun getAllByClassName(hardwareMap: HardwareMap, className: String): List<Any> {
        val clazz = try {
            Class.forName(className)
        } catch (_: Throwable) {
            return emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        return try {
            hardwareMap.getAll(clazz as Class<Any>) as? List<Any> ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun invokeNoArg(target: Any, methodName: String) {
        try {
            val m = target.javaClass.methods.firstOrNull { it.name == methodName && it.parameterTypes.isEmpty() }
            m?.invoke(target)
        } catch (_: Throwable) {
            // Ignore; logging should never crash the OpMode.
        }
    }

    private fun invokePosition(target: Any): Pose2D {
        // Prefer getPosition() (SDK driver) but accept Kotlin property getter name too.
        val candidates = listOf("getPosition", "get_position")
        for (name in candidates) {
            try {
                val m = target.javaClass.methods.firstOrNull { it.name == name && it.parameterTypes.isEmpty() }
                val value = m?.invoke(target)
                if (value is Pose2D) {
                    return value
                }
            } catch (_: Throwable) {
                // Try next.
            }
        }

        // Fallback: try Kotlin-style property getter "getPosition" via declaredMethods.
        try {
            val m = target.javaClass.declaredMethods.firstOrNull { it.name == "getPosition" && it.parameterTypes.isEmpty() }
            m?.isAccessible = true
            val value = m?.invoke(target)
            if (value is Pose2D) {
                return value
            }
        } catch (_: Throwable) {
        }

        return Pose2D(DistanceUnit.METER, 0.0, 0.0, AngleUnit.RADIANS, 0.0)
    }
}
