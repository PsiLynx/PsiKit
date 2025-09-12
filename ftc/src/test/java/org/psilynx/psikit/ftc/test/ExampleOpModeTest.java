package org.psilynx.psikit.ftc.test;


import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.core.rlog.RLOGServer;
import org.psilynx.psikit.core.rlog.RLOGWriter;
import org.psilynx.psikit.ftc.PsiKitLinearOpMode;

class ConceptPsiKitLogger extends PsiKitLinearOpMode {
    @Override
    public void runOpMode() {
        Logger.addDataReceiver(new RLOGServer());
        Logger.addDataReceiver(new RLOGWriter("/sdcard/FIRST/log.rlog"));
        Logger.recordMetadata("some metadata", "string value");
        Logger.start(); // Start logging! No more data receivers, replay sources, or metadata values may be added.
        Logger.periodicAfterUser(0, 0);

        while(!getPsiKitIsStarted()){
            Logger.periodicBeforeUser();

            processHardwareInputs();
            // this MUST come before any logic

            /*

             Init logic goes here

             */

            Logger.periodicAfterUser(0.0, 0.0);
            // logging these timestamps is completely optional
        }

        while(!getPsiKitIsStopRequested()) {

            double beforeUserStart = Logger.getTimestamp();
            Logger.periodicBeforeUser();
            double beforeUserEnd = Logger.getTimestamp();

            processHardwareInputs();
            // this MUST come before any logic

            /*

             OpMode logic goes here

             */

            Logger.recordOutput("OpMode/example", 2.0);
            // example


            double afterUserStart = Logger.getTimestamp();
            Logger.periodicAfterUser(
                    afterUserStart - beforeUserEnd,
                    beforeUserEnd - beforeUserStart
            );
        }
    }
}
