package opmode;

import Debug.Connector;
import Hardware.Hardware;
import Hardware.HardwareConstants;
import Hardware.HardwareData;
import Hardware.SensorData;
import Motion.MecanumSystem;
import State.DriveState;
import State.LogicState;
import State.StateMachineManager;
import math.Vector2;
import math.Vector3;
import math.Vector4;
@com.qualcomm.robotcore.eventloop.opmode.TeleOp
public class TeleOp extends BasicOpmode {
    public TeleOp() {
        super(1, true);
    }

    @Override
    public void setup() {
        robot.enableAll();
        robot.enableDevice(Hardware.HardwareDevices.LEFT_PIXY);
        StateMachineManager initManager = new StateMachineManager(statemachine) {
            @Override
            public void setup() {
                logicStates.put("init", new LogicState(stateMachine) {
                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {
                        telemetry.addData("In Init", true);
                    }
                });
            }

            @Override
            public void update(SensorData sensors, HardwareData hardware) {
                terminate = isStarted();
            }
        };
        final StateMachineManager teleOpMode1 = new StateMachineManager(statemachine) {
            @Override
            public void setup() {
                driveState.put("drive", new DriveState(stateMachine) {
                    @Override
                    public Vector4 getWheelVelocities(SensorData sensors) {
                        return MecanumSystem.translate(new Vector3(gamepad1.left_stick_x, gamepad1.left_stick_y, -gamepad1.right_stick_x));
                    }

                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {
                        int test = sensors.getPixy()[sensors.getPixy().length-2] & 0xFF;
                        telemetry.addData("SkyStone?", Math.abs(test) < 180);
                        telemetry.addData("SkyStone", Math.abs(test));
                        telemetry.addData("SkySton", test);
                    }
                });
                logicStates.put("latchSystem", new LogicState(stateMachine) {
                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {
                        if(gamepad2.left_trigger > 0){
                            hardware.setLatchServos(HardwareConstants.LATCH_OFF);
                        }
                        if(gamepad2.x){
                            hardware.setLatchServos(HardwareConstants.LATCH_ON);
                        }
                    }
                });
                logicStates.put("intake", new LogicState(stateMachine) {
                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {
                        if(gamepad2.right_trigger > 0){
                            hardware.setIntakePowers(gamepad2.right_trigger);
                        }else{
                            hardware.setIntakePowers(-gamepad2.left_trigger);
                        }
                    }
                });
                logicStates.put("Odometer", new LogicState(stateMachine) {
                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {
                        telemetry.addData("Factor", sensors.getAux() / Math.toDegrees(sensors.getGyro()));                    }
                });
            }

            @Override
            public void update(SensorData sensors, HardwareData hardware) {
                terminate = gamepad1.x;
            }
        };
        StateMachineManager teleOpMode2 = new StateMachineManager(statemachine) {
            @Override
            public void setup() {
                driveState.put("drive", new DriveState(stateMachine) {
                    double offset = 0;
                    @Override
                    public Vector4 getWheelVelocities(SensorData data) {
                        double r = Math.sqrt(gamepad1.left_stick_x * gamepad1.left_stick_x + gamepad1.left_stick_y * gamepad1.left_stick_y);
                        if(gamepad1.right_bumper){
                            offset = data.getGyro();
                        }
                        double theta = Math.atan2(gamepad1.left_stick_y, gamepad1.left_stick_x) + (data.getGyro()-offset);
                        return MecanumSystem.translate(new Vector3(r * Math.cos(theta), r * Math.sin(theta), -gamepad1.right_stick_x));
                    }

                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {

                    }
                });
                logicStates.put("latchSystem", new LogicState(stateMachine) {
                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {
                        if(gamepad2.left_trigger > 0){
                            hardware.setLatchServos(HardwareConstants.LATCH_OFF);
                        }
                        if(gamepad2.x){
                            hardware.setLatchServos(HardwareConstants.LATCH_ON);
                        }
                    }
                });
                logicStates.put("intake", new LogicState(stateMachine) {
                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {
                        if(gamepad2.right_trigger > 0){
                            hardware.setIntakePowers(gamepad2.right_trigger, gamepad2.right_trigger);
                        }else{
                            hardware.setIntakePowers(-gamepad2.left_trigger, -gamepad2.left_trigger);
                        }
                    }
                });
                logicStates.put("intakeServos", new LogicState(stateMachine) {
                    double position = 0;
                    long timePrev = 0;
                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {
                        if(gamepad2.right_bumper){
                            position += 0.1 * ((System.currentTimeMillis() - timePrev)/1000.0);
                        }else if(gamepad2.left_bumper){
                            position -= 0.1 * ((System.currentTimeMillis() - timePrev)/1000.0);
                        }
                        position = Math.min(position, 1);
                        position = Math.max(position, 0);
                        hardware.setIntakeServos(position, 1-position);
                        telemetry.addData("Position", position);
                        timePrev = System.currentTimeMillis();
                    }
                });
                logicStates.put("Gyro", new LogicState(stateMachine) {
                    @Override
                    public void update(SensorData sensors, HardwareData hardware) {
                        Connector.getInstance().addOrientation(Vector2.ZERO(), Math.toDegrees(sensors.getGyro()));
                        telemetry.addData("Gyro", Math.toDegrees(sensors.getGyro()));
                    }
                });
            }

            @Override
            public void update(SensorData sensors, HardwareData hardware) {
                terminate = gamepad1.y;
            }
        };
        stateMachineSwitcher.start(initManager, teleOpMode1, teleOpMode2);
    }
}
