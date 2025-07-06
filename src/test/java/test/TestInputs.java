package test;

import org.psilynx.psikit.LogTable;
import org.psilynx.psikit.LoggableInputs;

class TestInputs implements LoggableInputs {
    public double number;

    @Override
    public void toLog(LogTable table) {
        table.put("number", number);
    }

    @Override
    public void fromLog(LogTable table) {
        number = table.get("number", 0.0);
    }
}
