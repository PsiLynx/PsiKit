# Installation process
![TeamCode build.gradle](_media/teamcode_build_gradle.png)
1. This is the TeamCode `build.gradle` (**in blue above**), you need to add the three lines mentioned as `IMPORTANT`:
```groovy
repositories {
    // IMPORTANT
    maven { url 'https://repo.dairy.foundation/releases' }
    // IMPORTANT (thanks to dairy for hosting!)
}
```

```groovy
dependencies {
    implementation project(':FtcRobotController')
    // IMPORTANT
    implementation 'org.psilynx.psikit:core:0.1.0-beta2'
    implementation 'org.psilynx.psikit:ftc:0.1.0-beta2'
    // IMPORTANT
}
```

### That's it!  Everything's installed, you can now move on to the&nbsp;[Usage Guide](usage.md)
