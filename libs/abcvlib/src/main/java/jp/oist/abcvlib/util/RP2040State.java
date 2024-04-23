package jp.oist.abcvlib.util;

import android.content.Context;

import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;

public class RP2040State {

    protected class MotorsState{
        protected class ControlValues{
            protected byte left;
            protected byte right;
            protected byte getLeft() {
                return left;
            }
            protected byte getRight() {
                return right;
            }
        }
        protected class Faults{
            protected byte left;
            protected byte right;
            protected byte getLeft() {
                return left;
            }
            protected byte getRight() {
                return right;
            }
        }
        protected class EncoderCounts{
            int left;
            int right;
            protected int getLeft() {
                return left;
            }
            protected int getRight() {
                return right;
            }
        }

        protected ControlValues controlValues;
        protected EncoderCounts encoderCounts;
        protected Faults faults;

        protected MotorsState(){
            controlValues = new ControlValues();
            encoderCounts = new EncoderCounts();
            faults = new Faults();
        }

        protected ControlValues getControlValues() {
            return controlValues;
        }
        protected EncoderCounts getEncoderCounts() {
            return encoderCounts;
        }
        protected Faults getFaults() {
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
        protected float getVoltage() {
            return ((float) voltage / 1000f);
        }
        protected byte getSafetyStatus() {
            return safety_status;
        }
        protected float getTemperature() {
            return ((float) temperature / 10f);
        }
        protected byte getStateOfHealth() {
            return state_of_health;
        }
        protected short getFlags() {
            return flags;
        }
    }
    protected class ChargeSideUSB{
        int max77976_chg_details;
        boolean ncp3901_wireless_charger_attached;
        short usb_charger_voltage;

        protected int getMax77976ChgDetails() {
            return max77976_chg_details;
        }
        protected boolean isWirelessChargerAttached() {
            return ncp3901_wireless_charger_attached;
        }
        protected float getUsbChargerVoltage() {
            return ((float) usb_charger_voltage / 1000f);
        }
    }

    protected MotorsState motorsState;
    protected BatteryDetails batteryDetails;
    protected ChargeSideUSB chargeSideUSB;
    private BatteryData batteryData;
    private WheelData wheelData;
    

    protected RP2040State(BatteryData batteryData, WheelData wheelData){
        motorsState = new MotorsState();
        batteryDetails = new BatteryDetails();
        chargeSideUSB = new ChargeSideUSB();
        this.batteryData = batteryData;
        this.wheelData = wheelData;
    }

    protected void updatePublishers(){
        long ts = System.nanoTime();
        batteryData.onBatteryVoltageUpdate(ts, batteryDetails.getVoltage());
        //Todo need to implement coilVoltage get from rp2040
        batteryData.onChargerVoltageUpdate(ts, chargeSideUSB.getUsbChargerVoltage(), 0);
        wheelData.onWheelDataUpdate(ts, motorsState.getEncoderCounts().getLeft(), motorsState.getEncoderCounts().getRight());
    }
}
