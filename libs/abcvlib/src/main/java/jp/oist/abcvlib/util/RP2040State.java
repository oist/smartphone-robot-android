package jp.oist.abcvlib.util;

public class RP2040State {
//    CHARGE_SIDE_USB((byte) 0x01), Mux MAX77976 and NCP3901 Data: Charger-side USB Voltage, and Wireless Coil State
//    BATTERY_DETAILS((byte) 0x03), // BQ27742-G1 Data: Battery Voltage, Current, and State of Charge
//    PHONE_SIDE_USB((byte) 0x0C), // MAX77958 Phone-side USB Controller
//    MOTOR_DETAILS // DRV8830DRCR Data: Includes MOTOR_FAULT, ENCODER_COUNTS, MOTOR_LEVELS, MOTOR_BRAKE
    protected class MotorDetails{}
    protected class BatteryDetails{}
    protected class ChargeSideUSB{}
    protected class PhoneSideUSB{}
}
