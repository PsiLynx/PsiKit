# Instalation process
1. this is the TeamCode build.gradle, you need to add the two lines mentioned as `IMPORTANT`
```gradle
    // TeamCode/build.gradle
    
    apply from: '../build.common.gradle'
    apply from: '../build.dependencies.gradle'

    android {
        namespace = 'org.firstinspires.ftc.teamcode'
        
        packagingOptions {
            jniLibs.useLegacyPackaging true
        }
    }
    repositories {
        // IMPORTANT
        maven { url 'https://psilynx.github.io/PsiKit/gradle'}
    }

    dependencies {
        implementation project(':FtcRobotController')

        // IMPORTANT
        implementation 'org.psilynx:psikit:0.0.1'
    }
```
2. This is an example opmode, showing all the necessary code to run the server.
```java
    package org.firstinspires.ftc.teamcode;

    import com.qualcomm.robotcore.eventloop.opmode.OpMode;
    import org.psilynx.psikit.LogFileUtil;
    import org.psilynx.psikit.Logger;
    import org.psilynx.psikit.WPILOGReader;
    import org.psilynx.psikit.WPILOGWriter;

    class CommandOpMode {
        @Override;
        public void init() {
            RLOGServer server = RLOGServer();
            Logger.addDataReceiver(server);
            Logger.start();
            Logger.periodicAfterUser(0, 0);
            Logger.start(); // Start logging! No more data receivers, replay sources, or metadata values may be added.
        }

        @Override;
        public void loop() {
            Logger.periodicBeforeUser();
            
            
            // all logic goes here
            
            
            Logger.periodicAfterUser(0, 0);
            //the values periodicAfterUser accepts can be used to auto log information about how long things take

        }

    }
```