package jp.oist.abcvlib.util;

public class RP2040State {
//    CHARGE_SIDE_USB((byte) 0x01), Mux MAX77976 and NCP3901 Data: Charger-side USB Voltage, and Wireless Coil State
//    BATTERY_DETAILS((byte) 0x03), // BQ27742-G1 Data: Battery Voltage, Current, and State of Charge
//    PHONE_SIDE_USB((byte) 0x0C), // MAX77958 Phone-side USB Controller
    protected class MotorsState{
        protected class ControlValues{
            byte left;
            byte right;
        }
        protected class Faults{
            byte left;
            byte right;
        }
        protected class EncoderCounts{
            int left;
            int right;
        }

        ControlValues controlValues;
        EncoderCounts encoderCounts;
        Faults faults;

        public MotorsState(){
            controlValues = new ControlValues();
            encoderCounts = new EncoderCounts();
            faults = new Faults();
        }
    }
    protected class BatteryDetails{}
    protected class ChargeSideUSB{}
    protected class PhoneSideUSB{}

    MotorsState motorsState;
    BatteryDetails batteryDetails;
    ChargeSideUSB chargeSideUSB;
    PhoneSideUSB phoneSideUSB;

    public RP2040State(){
        motorsState = new MotorsState();
        batteryDetails = new BatteryDetails();
        chargeSideUSB = new ChargeSideUSB();
        phoneSideUSB = new PhoneSideUSB();
    }
}
