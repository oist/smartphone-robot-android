package jp.oist.abcvlib.util;

public class RP2040State {

    public class MotorsState{
        public class ControlValues{
            public byte left;
            public byte right;
            public byte getLeft() {
                return left;
            }
            public byte getRight() {
                return right;
            }
        }
        public class Faults{
            public byte left;
            public byte right;
            public byte getLeft() {
                return left;
            }
            public byte getRight() {
                return right;
            }
        }
        public class EncoderCounts{
            int left;
            int right;
            public int getLeft() {
                return left;
            }
            public int getRight() {
                return right;
            }
        }

        public ControlValues controlValues;
        public EncoderCounts encoderCounts;
        public Faults faults;

        protected MotorsState(){
            controlValues = new ControlValues();
            encoderCounts = new EncoderCounts();
            faults = new Faults();
        }

        public ControlValues getControlValues() {
            return controlValues;
        }
        public EncoderCounts getEncoderCounts() {
            return encoderCounts;
        }
        public Faults getFaults() {
            return faults;
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
        public byte getSafetyStatus() {
            return safety_status;
        }
        public float getTemperature() {
            return ((float) temperature / 10f);
        }
        public byte getStateOfHealth() {
            return state_of_health;
        }
        public short getFlags() {
            return flags;
        }
    }
    public class ChargeSideUSB{
        int max77976_chg_details;
        boolean ncp3901_wireless_charger_attached;
        short usb_charger_voltage;

        public int getMax77976ChgDetails() {
            return max77976_chg_details;
        }
        public boolean isNcp3901WirelessChargerAttached() {
            return ncp3901_wireless_charger_attached;
        }
        public float getUsbChargerVoltage() {
            return ((float) usb_charger_voltage / 1000f);
        }
    }

    public MotorsState motorsState;
    public BatteryDetails batteryDetails;
    public ChargeSideUSB chargeSideUSB;

    public RP2040State(){
        motorsState = new MotorsState();
        batteryDetails = new BatteryDetails();
        chargeSideUSB = new ChargeSideUSB();
    }
}
