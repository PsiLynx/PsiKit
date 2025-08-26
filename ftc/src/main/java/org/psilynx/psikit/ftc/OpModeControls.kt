package org.psilynx.psikit.ftc

import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs

object OpModeControls: LoggableInputs {
    var started = false
    var stopped = false
    override fun toLog(table: LogTable) {
        table.put("started", started)
        table.put("stopped", stopped)
    }

    override fun fromLog(table: LogTable) {
        started = table.get("started", false)
        stopped = table.get("stopped", false)
    }
}