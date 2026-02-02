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
        // Only overwrite when these keys exist in the replay log.
        // This avoids getting stuck in init when replaying older logs that never recorded
        // OpModeControls (LogTable.get(key, default) would otherwise return the default).
        if (table.get("started") != null) {
            started = table.get("started", started)
        }
        if (table.get("stopped") != null) {
            stopped = table.get("stopped", stopped)
        }
    }
}