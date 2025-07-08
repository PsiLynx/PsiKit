# Replay

Replay is a very powerful feature in PsiKit. It allows you to record
all the inputs your code receives, and then replay the code from a log file.
This allows you to replay the 
**exact internal state of the robot code**,
as it was while the robot was running

### Common Use Cases:
* replay logs from when a bug occurred, and
  - log additional values such as internal state in a calculation
  - use the IDE debugger to step through the robot code
* record a log of matches, when live streaming the data is not allowed
* keep debugging the code when the pesky "*build team*" takes away the robot

### Usage:
1. Use `Logger.addDataReceiver(new RLOGWriter(String folder, String fileName))`
to record data to a log file. 
2. Create an Inputs class, That implements `LoggableInputs`
3. Use `Logger.processInputs(LoggableInputs input)` on all Input classes
4. Make sure to never use the actual hardware, and instead always 
read input values from the Input class you made in step 2

##### Example LoggableInput
```java
package org.firstinspires.ftc.teamcode;

public class LiftInput implements LoggableInputs{
    private IntSupplier posSupplier;
    private int position;
    
    public LiftInput(IntSupplier posSupplier){
        this.posSupplier = posSupplier;
        // supplier would be something like myMotor::getPosition
    }
    
    /**
     * if in replay mode, returns the next logged input, 
     * otherwise returns the measured value
     */
    public int getPosition(){
        return position;
    }
    
    /**
     * Only called while actually running, updates the value of position
     * and writes it to a table. 
     * @param table the LogTable to fill out with the values represented
     * by this Input
     */
    @Override
    public void ToLog(LogTable table){
        position = posSupplier.getAsInt();
        table.put("position", position);
    }
    
    /**
     * Only called while replaying, updates the value of position
     * by reading its value from the LogTable
     * @param table the LogTable containing all the data for this Input
     * at this point in time
     */
    @Override
    public void FromLog(LogTable table){
        position = table.get("position", 0.0);
        // the second value is the defualt, in case "position" isn't found in the table
    }
```
##### Example Usage Of An Input
```java
package org.firstinspires.ftc.teamcode;

public class Lift{
  private DcMotor left;
  private DcMotor right;
  private EncoderInput input;
  private Int setpoint;
  
  private Double k_p = 1.0;
  
  /**
   * Simple lift that uses a P controller to achive a desired position. 
   * to use, simply call update() every loop. 
   */
  public Lift(DcMotor left, DcMotor right){
      this.left = left; 
      this.right = right; 
      this.input = LiftInput(left::getPosition);
  }
  
  public int getPosition(){
      return input.getPosition();
  }
  
  public int getSetpoint(){
      return setpoint;
  }
  public void setSetpoint(int newValue){
      setpoint = newValue;
  }
  
  public void update(){
      Logger.processInputs("Lift", input);
      
      double power = ( setpoint - getPosition() ) * k_p;
      // simple P controller
      
      left.setPower(power);
      right.setPower(power);
      
      Logger.recordOutput("Lift/k_p", k_p);
      Logger.recordOutput("Lift/power", power);
      Logger.recordOutput("Lift/setpoint", setpoint);
      // we don't need to record position because processInputs already does that
  }
      

```
  