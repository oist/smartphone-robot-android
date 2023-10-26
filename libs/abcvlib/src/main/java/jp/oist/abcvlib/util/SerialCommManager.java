package jp.oist.abcvlib.util;
import android.util.Log;
import com.hoho.android.usbserial.driver.SerialTimeoutException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    private class AndroidToRP2040Packet {
        private byte packetType;
        protected ByteBuffer data = ByteBuffer.allocate(Float.BYTES * 2);
        // make room for packet_type, and start and stop marks
        private ByteBuffer packet = ByteBuffer.allocate(Float.BYTES * 2 + 3);

        public AndroidToRP2040Packet(byte packetType){
            //rp2040 is little endian whereas Java is big endian. This is to ensure that the bytes are
            //written in the correct order for parsing on the rp2040
            data.order(ByteOrder.LITTLE_ENDIAN);
            packet.order(ByteOrder.LITTLE_ENDIAN);
            this.packetType = packetType;
            packet.put(UsbSerialProtocol.START.getHexValue());
            packet.put(packetType);
        }

        // Add data to packet then the end mark
        protected byte[] packetTobytes(){
            data.rewind();
            packet.put(data);
            packet.put(UsbSerialProtocol.STOP.getHexValue());
            return packet.array();
        }

        protected void clear(){
            packet.clear();
            packet.put(UsbSerialProtocol.START.getHexValue());
            packet.put(packetType);
            data.clear();
        }
    }

    // Preallocated bytebuffer to write motor levels to
    private AndroidToRP2040Packet motorLevels = new AndroidToRP2040Packet(UsbSerialProtocol.SET_MOTOR_LEVELS.getHexValue());
    private boolean shutdown = false;
    private Runnable pi2AndroidReader;
    private Runnable android2PiWriter;


    // Constructor to initialize SerialCommManager
    public SerialCommManager(UsbSerial usbSerial, Runnable pi2AndroidReader, Runnable android2PiWriter) {
        this.usbSerial = usbSerial;
        if (pi2AndroidReader == null){
            this.pi2AndroidReader = defaultPi2AndroidReader;
            Log.w("serial", "pi2AndroidReader was null. Using default rather than custom");
        }else {
            this.pi2AndroidReader = pi2AndroidReader;
        }
        if (android2PiWriter == null){
            this.android2PiWriter = defaultAndroid2PiWriter;
            Log.w("serial", "android2PiWriter was null. Using default rather than custom");
        }else{
            this.android2PiWriter = android2PiWriter;
        }
    }

    public SerialCommManager(UsbSerial usbSerial, Runnable android2PiWriter){
        this(usbSerial, null, android2PiWriter);
    }

    private final Runnable defaultPi2AndroidReader = new Runnable() {
        @Override
        public void run() {
            while (!shutdown) {
                int result = parseFifoPacket();
                usbSerial.packetParsed.setStatus(result);
                Log.i(Thread.currentThread().getName(), "usbSerial.packetParsed.notify()");
                synchronized (usbSerial.packetParsed){
                    usbSerial.packetParsed.notify();
                }
                usbSerial.awaitPacketReceived();
            }
        }
    };

    private final Runnable defaultAndroid2PiWriter = new Runnable() {
        @Override
        public void run() {
            while (!shutdown) {
                //TODO
            }
        }
    };

    // Start method to start the thread
    public void start() {
        ProcessPriorityThreadFactory serialCommManager_Pi2Android_factory =
                new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY,
                        "SerialCommManager_Pi2Android");
        ProcessPriorityThreadFactory serialCommManager_Android2Pi_factory =
                new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY,
                        "SerialCommManager_Android2Pi");
        Executors.newSingleThreadScheduledExecutor(serialCommManager_Pi2Android_factory).
                execute(pi2AndroidReader);
        Executors.newSingleThreadScheduledExecutor(serialCommManager_Android2Pi_factory).
                scheduleWithFixedDelay(android2PiWriter, 0, 10, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    protected void stop() {
        shutdown = true;
    }

    //TODO paseFifoPacket() should call the various SerialResponseListener methods.
    protected int parseFifoPacket() {
        int result = 0;
        byte[] packet = null;
        // Run the command on a thread handler to allow the queue to keep being added to
        synchronized (usbSerial.fifoQueue) {
            packet = usbSerial.fifoQueue.poll();
        }
            // Check if there is a packet in the queue (fifoQueue
        if (packet != null) {

            // Log packet as an array of hex bytes
            StringBuilder sb = new StringBuilder();
            for (byte b : packet) {
                sb.append(String.format("%02X ", b));
            }
            Log.i(Thread.currentThread().getName(), "Received packet: " + sb.toString());

            // The first byte after the start mark is the command
            UsbSerialProtocol command = UsbSerialProtocol.getEnumByValue(packet[0]);
            Log.i(Thread.currentThread().getName(), "Received " + command + " from pi");
            if (command == null){
                Log.e("Pi2AndroidReader", "Command not found");
                return -1;
            }
            switch (command) {
                case DO_NOTHING:
                    Log.d("Pi2AndroidReader", "parseDoNothing");
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
                    Log.w("Pi2AndroidReader", "Nack issued from device");
                    result = -1;
                    break;
                case ACK:
                    onAck(packet);
                    result = 1;
                    Log.d("Pi2AndroidReader", "parseAck");
                    break;
                case START:
                    Log.e("Pi2AndroidReader", "parseStart. Start should never be a command");
                    result = -1;
                    break;
                case STOP:
                    Log.e("Pi2AndroidReader", "parseStop. Stop should never be a command");
                    result = -1;
                    break;
                default:
                    Log.e("Pi2AndroidReader", "parsePacket. Command not found");
                    result = -1;
                    break;
            }
        }
        else {
            Log.i(Thread.currentThread().getName(), "No packet in queue");
            result = 0;
        }
        return result;
    }


    /**
     * Do not use this method unless you are very familiar with the protocol on both the rp2040 and
     * Android side. This method is used to send raw bytes to the rp2040. It is recommended to use
     * the wrapped methods for doing higher level commands such as setMotorLevels. Sending the wrong
     * bytes to the rp2040 can cause it to crash and require a reset or worse.
     * @param bytes The raw bytes to be sent to the rp2040
     * @return 0 if successful, -1 if mResponse is not large enough to hold all of response and the stop mark,
     * -2 if SerialTimeoutException on send
     */
    private int sendPacket(byte[] bytes) {
        try {
            this.usbSerial.send(bytes, 1000);
        } catch (SerialTimeoutException e){
            Log.e("serial", "SerialTimeoutException on send");
            return -2;
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        motorLevels.data.putFloat(left);
        motorLevels.data.putFloat(right);
        byte[] commandData = motorLevels.packetTobytes();
        if (sendPacket(commandData) != 0){
            Log.e("Android2PiWriter", "Error sending packet");
        }else{
            Log.d("Android2PiWriter", "Packet sent");
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
