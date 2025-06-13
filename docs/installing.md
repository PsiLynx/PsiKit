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
### That's It! Everything's Installed, You Can Move On To The &nbsp;[Usage Guide](usage.md)