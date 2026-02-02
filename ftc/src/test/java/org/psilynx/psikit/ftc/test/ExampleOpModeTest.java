package org.psilynx.psikit.ftc.test;


import com.qualcomm.robotcore.eventloop.opmode.PsiKitLinearOpMode;

import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.core.rlog.RLOGServer;
import org.psilynx.psikit.core.rlog.RLOGWriter;

class ConceptPsiKitLogger extends PsiKitLinearOpMode {
    @Override
    protected void onPsiKitConfigureLogging() {
        Logger.addDataReceiver(new RLOGServer());
        Logger.addDataReceiver(new RLOGWriter("log.rlog"));
        Logger.recordMetadata("some metadata", "string value");
    }

    @Override
    public void runOpMode() {
        waitForStart();

        while (opModeIsActive()) {
            // Your linear-style OpMode logic goes here.
            Logger.recordOutput("OpMode/example", 2.0);
        }
    }
}
