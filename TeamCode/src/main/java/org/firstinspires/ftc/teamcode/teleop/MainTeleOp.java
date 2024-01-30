package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.hardware.bosch.BHI260IMU;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

@TeleOp(name = "MainTeleOp")
public class MainTeleOp extends LinearOpMode {

    DcMotor rightFront, rightRear, leftFront, leftRear;
    DcMotor leftSlide, rightSlide;
    DcMotor leftLift, rightLift;
    Servo leftArm, rightArm;
    IMU imu;
    IMU.Parameters myIMUparameters;
    Orientation myRobotOrientation;
    double axial_drive;
    double lateral_drive;
    double yaw_drive;
    double heading_drive;
    double input_lift;


    //constants
    double DRIVE_POWER_SCALE = 0.9;
    double SLIDE_POWER_SCALE = 0.3;
    double LIFT_POWER_SCALE = 0.75;
    int slideUpPos = 1800;
    int slideDownPos = 0;
    int liftDownPos = 0;
    double armUpPos = 1;
    double armDownPos = 0;



    @Override
    public void runOpMode() throws InterruptedException {

        //drive initialization
        rightFront = hardwareMap.get(DcMotor.class, "rightFront");
        rightRear = hardwareMap.get(DcMotor.class,"rightRear");
        leftFront = hardwareMap.get(DcMotor.class,"leftFront");
        leftRear = hardwareMap.get(DcMotor.class,"leftRear");
        rightFront.setDirection(DcMotor.Direction.REVERSE);
        rightRear.setDirection(DcMotor.Direction.REVERSE);
        leftFront.setDirection(DcMotor.Direction.FORWARD);
        leftRear.setDirection(DcMotor.Direction.FORWARD);
        rightFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightRear.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftRear.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        //slide initialization
        leftSlide = hardwareMap.get(DcMotor.class, "leftSlide");
        rightSlide = hardwareMap.get(DcMotor.class, "rightSlide");
        leftSlide.setDirection(DcMotor.Direction.FORWARD);
        rightSlide.setDirection(DcMotor.Direction.REVERSE);
        leftSlide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightSlide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        //lift initialization
        leftLift = hardwareMap.get(DcMotor.class, "leftLift");
        rightLift = hardwareMap.get(DcMotor.class, "rightLift");
        leftLift.setDirection(DcMotorSimple.Direction.FORWARD);
        rightLift.setDirection(DcMotorSimple.Direction.REVERSE);
        leftLift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightLift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftLift.setTargetPosition(liftDownPos);
        rightLift.setTargetPosition(liftDownPos);
        leftLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        //arm initialization
        leftArm = hardwareMap.get(Servo.class, "leftArm");
        rightArm = hardwareMap.get(Servo.class, "rightArm");
        leftArm.setDirection(Servo.Direction.FORWARD);
        rightArm.setDirection(Servo.Direction.REVERSE);

        //imu initialization
        imu = hardwareMap.get(IMU.class, "imu");
        RevHubOrientationOnRobot.LogoFacingDirection logoDirection = RevHubOrientationOnRobot.LogoFacingDirection.UP;
        RevHubOrientationOnRobot.UsbFacingDirection  usbDirection  = RevHubOrientationOnRobot.UsbFacingDirection.RIGHT;
        RevHubOrientationOnRobot orientationOnRobot = new RevHubOrientationOnRobot(logoDirection, usbDirection);
        imu.initialize(new IMU.Parameters(orientationOnRobot));
        myRobotOrientation = imu.getRobotOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) return;

        while (opModeIsActive()) {
            myRobotOrientation = imu.getRobotOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS);
            axial_drive = -gamepad1.left_stick_y;
            lateral_drive = gamepad1.left_stick_x;
            yaw_drive = gamepad1.right_stick_x;
            heading_drive = myRobotOrientation.firstAngle;

            input_lift = gamepad2.left_stick_y;

            mecanum_drive_field(axial_drive,lateral_drive,yaw_drive,heading_drive);
            lift(input_lift);

            if (gamepad2.b) {
                slide(slideUpPos);
            }
            if (gamepad2.left_stick_button) {
                slide(slideDownPos);
            }
            if (gamepad2.x) {
                arm(armDownPos);
            } else {
                arm(armUpPos);
            }
        }
    }
    public void mecanum_drive_field(double axial, double lateral, double yaw, double heading) {
        // Rotate the movement direction counter to the bot's rotation
        double rotX = lateral * Math.cos(-heading) - axial * Math.sin(-heading);
        double rotY = lateral * Math.sin(-heading) + axial * Math.cos(-heading);

        // Denominator is the largest motor power (absolute value) or 1
        // This ensures all the powers maintain the same ratio,
        // but only if at least one is out of the range [-1, 1]
        double denominator = Math.max(Math.abs(rotY) + Math.abs(rotX) + Math.abs(yaw), 1);
        double leftFrontPower = (rotY + rotX + yaw) / denominator;
        double leftRearPower = (rotY - rotX + yaw) / denominator;
        double rightFrontPower = (rotY - rotX - yaw) / denominator;
        double rightRearPower = (rotY + rotX - yaw) / denominator;
        leftFront.setPower(leftFrontPower * DRIVE_POWER_SCALE);
        leftRear.setPower(leftRearPower * DRIVE_POWER_SCALE);
        rightFront.setPower(rightFrontPower * DRIVE_POWER_SCALE);
        rightRear.setPower(rightRearPower * DRIVE_POWER_SCALE);
    }

    public void mecanum_drive_robot(double axial, double lateral, double yaw) {
        double denominator = Math.max(Math.abs(axial) + Math.abs(lateral) + Math.abs(yaw), 1);
        double leftFrontPower = (axial + lateral + yaw) / denominator;
        double leftRearPower = (axial - lateral + yaw) / denominator;
        double rightFrontPower = (axial - lateral - yaw) / denominator;
        double rightRearPower = (axial + lateral - yaw) / denominator;
        leftFront.setPower(leftFrontPower * DRIVE_POWER_SCALE);
        leftRear.setPower(leftRearPower * DRIVE_POWER_SCALE);
        rightFront.setPower(rightFrontPower * DRIVE_POWER_SCALE);
        rightRear.setPower(rightRearPower * DRIVE_POWER_SCALE);
    }

    public void slide(int position) {
        leftSlide.setTargetPosition(position);
        rightSlide.setTargetPosition(position);
        leftSlide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightSlide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftSlide.setPower(SLIDE_POWER_SCALE);
        rightSlide.setPower(SLIDE_POWER_SCALE);
    }

    public void arm(double position) {
        leftArm.setPosition(position);
        rightArm.setPosition(position);
    }

    public void lift(double power) {
        if (power < 0) {
            leftLift.setTargetPosition(leftLift.getCurrentPosition() + 100);
            rightLift.setTargetPosition(rightLift.getCurrentPosition() + 100);
            leftLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            rightLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            leftLift.setPower(LIFT_POWER_SCALE);
            rightLift.setPower(LIFT_POWER_SCALE);
        } else if (power < 0) {
            leftLift.setTargetPosition(leftLift.getCurrentPosition() + 100);
            rightLift.setTargetPosition(rightLift.getCurrentPosition() + 100);
            leftLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            rightLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            leftLift.setPower(-LIFT_POWER_SCALE);
            rightLift.setPower(-LIFT_POWER_SCALE);
        } else {
            leftLift.setTargetPosition(leftLift.getCurrentPosition());
            rightLift.setTargetPosition(rightLift.getCurrentPosition());
            leftLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            rightLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            leftLift.setPower(LIFT_POWER_SCALE);
            rightLift.setPower(LIFT_POWER_SCALE);
        }
    }

}
