# Installation process
![TeamCode build.gradle](_media/teamcode_build_gradle.png)
1. This is the TeamCode `build.gradle` (**in blue above**), you need to add the two lines mentioned as `IMPORTANT`:

```groovy
// TeamCode/build.gradle

apply from: '../build.common.gradle'
apply from: '../build.dependencies.gradle'

android {
    namespace = 'org.firstinspires.ftc.teamcode'


    packagingOptions {
        jniLibs.useLegacyPackaging true
        
        // IMPORTANT
        implementation 'org.psilynx.psikit:core:0.1.0-beta1'
        implementation 'org.psilynx.psikit:ftc:0.1.0-beta1'
        
        // these are for replay
        testImplementation "org.robolectric:robolectric:4.12.1"
        testImplementation 'junit:junit:4.12'
        testImplementation "io.mockk:mockk:1.14.5"
        testImplementation "io.mockk:mockk-android:1.14.5"
        testImplementation "io.mockk:mockk-agent:1.14.5"

        // IMPORTANT
    }
}
repositories {

    // IMPORTANT (thanks to dairy for hosting!)
    maven { url 'https://repo.dairy.foundation/releases' }
    // IMPORTANT (thanks to dairy for hosting!)

}

dependencies {
    implementation project(':FtcRobotController')

    // IMPORTANT
    implementation 'org.psilynx.psikit:core:0.1.0'
    implementation 'org.psilynx.psikit:ftc:0.1.0'
    // IMPORTANT
}
```

### That's it!  Everything's installed, you can now move on to the&nbsp;[Usage Guide](usage.md)
