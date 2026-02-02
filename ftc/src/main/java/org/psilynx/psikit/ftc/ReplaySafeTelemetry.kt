package org.psilynx.psikit.ftc

import org.firstinspires.ftc.robotcore.external.Telemetry

/**
 * Wraps an existing FTC SDK Telemetry instance and guarantees that update() will not throw.
 *
 * This is primarily for replay (Robolectric/CLI) where internal FTC services may not be wired.
 */
class ReplaySafeTelemetry(
    private val delegate: Telemetry,
) : Telemetry by delegate {

    override fun update(): Boolean {
        return try {
            delegate.update()
        } catch (_: Throwable) {
            false
        }
    }
}
