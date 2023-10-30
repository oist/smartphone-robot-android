package jp.oist.abcvlib.util;

public class RP2040State {

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
    protected class BatteryDetails{
        // Raw byte data
        short voltage;
        byte safety_status;
        short temperature;
        byte state_of_health;
        short flags;

        /* convert mV to V
           See bq27441-G1 Technical Reference Manual, Section 4.1.5
         */
        public float getVoltage() {
            return ((float) voltage / 1000f);
        }
    }
    //    CHARGE_SIDE_USB((byte) 0x01), Mux MAX77976 and NCP3901 Data: Charger-side USB Voltage, and Wireless Coil State
//    PHONE_SIDE_USB((byte) 0x0C), // MAX77958 Phone-side USB Controller
    protected class ChargeSideUSB{
        int max77976_chg_details;
        boolean ncp3901_wireless_charger_attached;
    }
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
