package test;

import org.psilynx.psikit.core.LogTable;
import org.psilynx.psikit.core.LoggableInputs;
import org.psilynx.psikit.core.wpi.Pose2d;

import java.util.Map;

class TestInput implements LoggableInputs {
    public int number;
    public Pose2d pose;

    @Override
    public void toLog(LogTable table) {
        table.put("number", number);
        table.put("pose", pose);
    }

    @Override
    public void fromLog(LogTable table) {
        number = table.get("number", -1);
        pose = table.get("pose", Pose2d.struct, Pose2d.kZero);
    }
}
