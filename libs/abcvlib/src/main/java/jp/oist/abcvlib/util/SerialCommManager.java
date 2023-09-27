package jp.oist.abcvlib.util;

import android.util.Log;

import com.hoho.android.usbserial.driver.SerialTimeoutException;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;


public class SerialCommManager {
    /*
    Class to manage request-response patter between Android phone and USB Serial connection
    over a separate thread.
    send()
    onReceive()
     */

    private UsbSerial usbSerial;
    // fifoQueue is used to store the commands that are sent from the mcu to be executed
    // on the Android phone
    // Preallocated bytebuffer to write motor levels to
    private ByteBuffer motorLevels = ByteBuffer.allocate((Float.BYTES * 2) + 1);





    // Constructor to initialize SerialCommManager
    public SerialCommManager(UsbSerial usbSerial) {
        this.usbSerial = usbSerial;
        //rp2040 is little endian whereas Java is big endian. This is to ensure that the bytes are
        //written in the correct order for parsing on the rp2040
        motorLevels.order(ByteOrder.LITTLE_ENDIAN);
    }

    // Create a default Runnable that sends the Commands.DO_NOTHING command
    // This default Runnable will be overridden or specified in the constructor of this class
    // such that when started, the overriden or specified Runnable will be executed instead of the default

    Runnable defaultRunnable = new Runnable() {
        UsbSerial usbSerial = SerialCommManager.this.usbSerial;
        @Override
        public void run() {
            parseFifoPacket();
            setMotorLevels(0.0f, 0.0f);
        }
    };

    // Start method to start the thread
    public void start() {
        ProcessPriorityThreadFactory threadFactory = new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY, "SerialCommManager");
        Executors.newSingleThreadExecutor(threadFactory).execute(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    private int parseFifoPacket() {
        int result = 0;
        // Run the command on a thread handler to allow the queue to keep being added to
        byte[] packet = usbSerial.fifoQueue.poll();
        if (packet != null) {
            // The first byte after the start mark is the command
            UsbSerialProtocol command = UsbSerialProtocol.getEnumByValue(packet[0]);
            switch (command) {
                case DO_NOTHING:
                    Log.d("serial", "parseDoNothing");
                    break;
                case GET_CHARGE_DETAILS:
                    onResponseGetChargeDetails(packet);
                    break;
                case GET_LOGS:
                    onResponseGetLog(packet);
                    break;
                case VARIOUS:
                    onResponseVarious(packet);
                    break;
                case GET_ENCODER_COUNTS:
                    onResponseGetEncoderCounts(packet);
                    break;
                case RESET_ENCODER_COUNTS:
                    onResponseResetEncoderCounts(packet);
                    break;
                case SET_MOTOR_LEVELS:
                    onResponseSetMotorLevels(packet);
                    break;
                case SET_MOTOR_BRAKE:
                    onResponseSetMotorBrake(packet);
                    break;
                case GET_USB_VOLTAGE:
                    onResponseGetUSBVoltage(packet);
                    break;
                case ON_WIRELESS_ATTACHED:
                    onWirelessCoilAttached(packet);
                    break;
                case ON_WIRELESS_DETACHED:
                    onWirelessCoilDetached(packet);
                    break;
                case ON_MOTOR_FAULT:
                    onMotorFault(packet);
                    break;
                case ON_USB_ERROR:
                    onUSBError(packet);
                    break;
                case NACK:
                    onNack(packet);
                    Log.w("serial", "Nack issued from device");
                    result = -1;
                    break;
                case ACK:
                    onAck(packet);
                    Log.d("serial", "parseAck");
                    break;
                case START:
                    Log.e("serial", "parseStart. Start should never be a command");
                    result = -1;
                    break;
                case STOP:
                    Log.e("serial", "parseStop. Stop should never be a command");
                    result = -1;
                    break;
                default:
                    Log.e("serial", "parsePacket. Command not found");
                    result = -1;
                    break;
            }
        }
        return result;
    }

    protected int sendPacket(byte[] bytes) {
        byte[] packetSend = new byte[11];
        // Note this size should match the one being sent by rp2040.
        byte[] returnPacket = new byte[512];
        // check whether mResponse is large enough to hold all of response and the start/stop marks
        if (packetSend.length < bytes.length + 2){
            Log.e("serial", "mResponse is not large enough to hold all of response and the stop mark");
            return -1;
        }else{
            packetSend[0] = UsbSerialProtocol.START.getHexValue();
            //Copies the contents of response into mResponse starting at index 1 so as not to overwrite the start mark
            System.arraycopy(bytes, 0, packetSend, 1, bytes.length);
            packetSend[bytes.length+1] = UsbSerialProtocol.STOP.getHexValue();
            try {
                this.usbSerial.send(packetSend, 1000);
                //TODO wait for onNewData to be called and succeed in finding a packet
            } catch (SerialTimeoutException e){
                Log.e("serial", "SerialTimeoutException on send");
                return -2;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;
    }

    //-------------------------------------------------------------------///
    // ---- API function calls for requesting something from the mcu ----///
    //-------------------------------------------------------------------///
    private void sendAck() throws IOException {
        byte[] ack = new byte[]{UsbSerialProtocol.ACK.getHexValue()};
        sendPacket(ack);
    }

    /*
    parameters:
    int: left [-1,1] representing full speed backward to full speed forward
    int: right (same as left)
     */
    public void setMotorLevels(float left, float right) {
        // Truncate levels above or below 1 or -1
        if (left > 1){
            left = 1;
            Log.w("serial", "Left motor level truncated to 1");
        }else if (left < -1){
            left = -1;
            Log.w("serial", "Left motor level truncated to -1");
        }
        if (right > 1){
            right = 1;
            Log.w("serial", "Right motor level truncated to 1");
        }else if (right < -1){
            right = -1;
            Log.w("serial", "Right motor level truncated to -1");
        }

        // Normalize [-1,1] to [-5.08,5.08] as this is the range accepted by the chip
        left = left * 5.08f;
        right = right * 5.08f;

        motorLevels.clear();
        motorLevels.put(UsbSerialProtocol.SET_MOTOR_LEVELS.getHexValue());
        motorLevels.putFloat(left);
        motorLevels.putFloat(right);
        byte[] commandData = motorLevels.array();
        //TODO this can likely be optimized by reuse rather than allocation every loop.
        byte[] packetReturn = new byte[8];
        if (sendPacket(commandData) != 0){
            Log.e("serial", "Error sending packet");
        }else{
            Log.d("serial", "Packet sent");
        }
    }

    //----------------------------------------------------------///
    // ---- Handlers for when data is returned from the mcu ----///
    // ---- Override these defaults with your own handlers -----///
    //----------------------------------------------------------///
    private void parseDoNothing(byte[] bytes) {
        Log.d("serial", "parseDoNothing");
    }
    private void onResponseGetChargeDetails(byte[] bytes) {
        Log.d("serial", "parseGetChargeDetails");
    }
    private void onResponseGetLog(byte[] bytes) {
        Log.d("serial", "parseGetLogs");
    }
    private void onResponseVarious(byte[] bytes) {
        Log.d("serial", "parseVarious");
    }
    private void onResponseGetEncoderCounts(byte[] bytes) {
        Log.d("serial", "parseGetEncoderCounts");
    }
    private void onResponseResetEncoderCounts(byte[] bytes) {
        Log.d("serial", "parseResetEncoderCounts");
    }
    private void onResponseSetMotorLevels(byte[] bytes) {
        Log.d("serial", "parseSetMotorLevels");
    }
    private void onResponseSetMotorBrake(byte[] bytes) {
        Log.d("serial", "parseSetMotorBrake");
    }
    private void onResponseGetUSBVoltage(byte[] bytes) {
        Log.d("serial", "parseGetUSBVoltage");
    }
    private void onWirelessCoilAttached(byte[] bytes) {
        Log.d("serial", "parseOnWirelessAttached");
    }
    private void onWirelessCoilDetached(byte[] bytes) {
        Log.d("serial", "parseOnWirelessDetached");
    }
    private void onMotorFault(byte[] bytes) {
        Log.d("serial", "parseOnMotorFault");
    }
    private void onUSBError(byte[] bytes) {
        Log.d("serial", "parseOnUSBError");
    }
    private void onNack(byte[] bytes) {
        Log.d("serial", "parseNack");
    }
    private void onAck(byte[] bytes) {
        Log.d("serial", "parseAck");
    }


}
