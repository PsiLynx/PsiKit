| Device                  |    | Status                                                |
|-------------------------|----|-------------------------------------------------------|
| DcMotors                | ✅  | Full Support                                          |
| Servos                  | ✅  | Full Support                                          |
| CRServos                | ✅  | Full Support                                          |
| Quadrature Inputs       | ✅  | Full Support                                          |
| Analog Inputs           | ✅  | Full Support                                          |
| Digital I/O             | ✅  | Full Support                                          |
| Internal Voltage Sensor | ✅  | Full Support                                          |
| Gamepads                | ✅  | PS4 controllers are untested, expected to work fully. |
| goBILDA Pinpoint v1     | ✅  | Full Support                                          |
| goBILDA Pinpoint v2     | ⚠️ | Untested                                              |
| Sparkfun OTOS           | ⚠️ | Untested                                              |

> All other I2C devices are unsupported. More savvy users can take a look at `HardwareMapWrapper.kt` in the FTC module; adding support for other devices is relatively easy. If you do add support for a device, please send a PR my way.
