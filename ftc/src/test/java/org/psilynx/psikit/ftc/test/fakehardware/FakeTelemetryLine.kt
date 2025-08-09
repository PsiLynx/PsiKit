package org.psilynx.psikit.ftc.test.fakehardware

import org.firstinspires.ftc.robotcore.external.Func
import org.firstinspires.ftc.robotcore.external.Telemetry

class FakeTelemetryLine:Telemetry.Line {
    override fun addData(p0: String?, p1: String?, vararg p2: Any?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun addData(p0: String?, p1: Any?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> addData(p0: String?, p1: Func<T>?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> addData(p0: String?, p1: String?, p2: Func<T>?): Telemetry.Item {
        TODO("Not yet implemented")
    }
}