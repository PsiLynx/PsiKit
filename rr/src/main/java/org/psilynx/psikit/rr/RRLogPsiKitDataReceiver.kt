package org.psilynx.psikit.rr

import com.acmerobotics.roadrunner.ftc.FlightRecorder
import org.psilynx.psikit.core.LogDataReceiver
import org.psilynx.psikit.core.LogTable

class RRLogPsiKitDataReceiver: LogDataReceiver {
    override fun putTable(table: LogTable) {
        table.getAll(false).forEach { (key, data) ->
            FlightRecorder.write(key, data.getObject())
        }
    }
}
