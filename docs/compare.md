# Comparison to FTC Dashboard

| Feature                      | PsiKit                                                                                              | FTC Dashboard                                            |
| ---------------------------- | --------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| Logging                      | ✅ Supports logging inputs and outputs, as well as replaying those log files                        | 🚫 No log support                                        |
| Telemetry                    | ✅ Record any primitive data types and Strings, as well as poses, mechanisms, and custom data types | ⚠️ Display primitive data types and strings only         |
| Live value change            | 🚫 No live value change support (FTC Dashboard-style tuning coming soon)                            | ✅ Changing values causes immediate updates on the robot |
| Console                      | ✅ `System.out` is automatically logged                                                             | 🚫 No `System.out` logging                               |
| AdvantageScope compatibility | ✅ Live streaming with RLOG                                                                         | ✅ Live streaming with FTC dashboard protocol            |
