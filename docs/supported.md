| Device                  |    | Status                                                |
|-------------------------|----|-------------------------------------------------------|
| DcMotors                | ✅  | Full Support                                          |
| Servos                  | ✅  | Full Support                                          |
| CrServos                | ✅  | Full Support                                          |
| Quadrature Inputs       | ✅  | Full Support                                          |
| Analog Inputs           | ✅  | Full Support                                          |
| Digital I/O             | ✅  | Full Support                                          |
| Internal Voltage Sensor | ✅  | Full Support                                          |
| GamePads                | ✅  | Ps4 controllers are untested, expected to work fully. |
| GoBilda PinPoint v1     | ✅  | Full Support                                          |
| GoBilda PinPoint v2     | ⚠️ | Untested                                              |
| Sparkfun OTOS           | ⚠️ | Untested                                              |

> All other i2c devices are unsupported. More savvy users can take a look at 
> HardwareMapWrapper.kt in the ftc module, adding support for other devices is 
> relatively easy. If you do add support for a device, please send a PR my way.
