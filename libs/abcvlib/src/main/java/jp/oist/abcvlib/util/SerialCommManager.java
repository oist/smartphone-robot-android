package jp.oist.abcvlib.util;

import android.util.Log;
import com.hoho.android.usbserial.driver.SerialTimeoutException;

import java.io.IOException;
import java.nio.ByteBuffer;
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
    private AndroidToRP2040Packet androidToRP2040Packet = new AndroidToRP2040Packet();
    private RP2040State rp2040State = new RP2040State();
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
            AndroidToRP2040Command command = AndroidToRP2040Command.getEnumByValue(packet[1]);
            Log.i(Thread.currentThread().getName(), "Received " + command + " from pi");
            if (command == null){
                Log.e("Pi2AndroidReader", "Command not found");
                return -1;
            }
            switch (command) {
                case GET_LOG:
                    parseLog(packet);
                    result = 1;
                    break;
                case SET_MOTOR_LEVELS:
                case RESET_STATE:
                    parseStatus(packet);
                    result = 1;
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
        if (bytes.length != AndroidToRP2040Packet.packetSize) {
            throw new IllegalArgumentException("Input byte array must have a length of " + AndroidToRP2040Packet.packetSize);
        }
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
        byte[] ack = new byte[]{AndroidToRP2040Command.ACK.getHexValue()};
        sendPacket(ack);
    }

    /*
    parameters:
    int: left [-1,1] representing full speed backward to full speed forward
    int: right (same as left)
     */
    public void setMotorLevels(float left, float right, boolean leftBrake, boolean rightBrake) {
        androidToRP2040Packet.clear();
        androidToRP2040Packet.setCommand(AndroidToRP2040Command.SET_MOTOR_LEVELS);

        final float LOWER_LIMIT = 0.49f;

        // Normalize [-1,1] to [-5.06,5.06] as this is the range accepted by the chip
        left = left * 5.06f;
        right = right * 5.06f;

        byte DRV8830_IN1_BIT = 0;
        byte DRV8830_IN2_BIT = 1;

        float[] voltages = new float[]{left, right};
        float[] abs_voltages = new float[2];
        byte[] control_values = new byte[2];
        boolean[] brakes = new boolean[]{leftBrake, rightBrake};

        for (int i = 0; i < voltages.length; i++) {
            float voltage = voltages[i]; // Get the current voltage
            control_values[i] = 0; // Reset the control value
            // Exclude or truncate voltages between -0.48V and 0.48V to 0V
            // Changing to 0.49 as the scaling function would result in 0x05h for 0.48V and
            // cause the rp2040 to perform unexpectely as it is a reserved register value
            if (voltage >= -LOWER_LIMIT && voltage <= LOWER_LIMIT) {
                voltages[i] = 0.0f; // Update the value in the array
            }else{
                // Clamp the voltage within the valid range
                // Need to clamp here rather than at byte representation to prevent overflow
                if (voltages[i] < -5.06) {
                    voltages[i] = -5.06f; // Update the value in the array
                }
                else if (voltages[i] > 5.06) {
                    voltages[i] = 5.06f; // Update the value in the array
                }

                abs_voltages[i] = Math.abs(voltages[i]);
                // Convert voltage to control value (-0x3F to 0x3F)
                control_values[i] = (byte)(((64 * abs_voltages[i]) / (4 * 1.285)) - 1);
                // voltage is defined by bits 2-7. Shift the control value to the correct position
                control_values[i] = (byte) (control_values[i] << 2);
            }


            // Set the IN1 and IN2 bits based on the sign of the voltage
            if (brakes[i]) {
                control_values[i] |= (1 << DRV8830_IN1_BIT);
                control_values[i] |= (1 << DRV8830_IN2_BIT);
            }else{
                if (voltage < 0) {
                    control_values[i] |= (1 << DRV8830_IN1_BIT);
                    control_values[i] &= ~(1 << DRV8830_IN2_BIT);
                } else if (voltage > 0) {
                    control_values[i] |= (1 << DRV8830_IN2_BIT);
                    control_values[i] &= ~(1 << DRV8830_IN1_BIT);
                } else {
                    // Standby/Coast: Both IN1 and IN2 set to 0
                    control_values[i] = 0;
                }
            }
            androidToRP2040Packet.payload.put(control_values[i]);
        }

        // Just to check for error in return packet
        rp2040State.motorsState.controlValues.left = control_values[0];
        rp2040State.motorsState.controlValues.right = control_values[1];

        byte[] commandData = androidToRP2040Packet.packetTobytes();
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
    private void parseLog(byte[] bytes) {
        Log.d("serial", "parseLogs");
    }
    private void parseStatus(byte[] bytes) {
        Log.d("serial", "parseStatus");
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        byteBuffer.position(2); // Skip the start and command bytes
        if (rp2040State.motorsState.controlValues.left != byteBuffer.get()){
            Log.e("serial", "Left control value mismatch");
        }
        if (rp2040State.motorsState.controlValues.right != byteBuffer.get()){
            Log.e("serial", "Right control value mismatch");
        }
        rp2040State.motorsState.faults.left = byteBuffer.get();
        rp2040State.motorsState.faults.right = byteBuffer.get();
        rp2040State.motorsState.encoderCounts.left = byteBuffer.getInt();
        rp2040State.motorsState.encoderCounts.right = byteBuffer.getInt();
        Log.v("serial", "Left encoder count: " + rp2040State.motorsState.encoderCounts.left);
        Log.v("serial", "Right encoder count: " + rp2040State.motorsState.encoderCounts.right);
        rp2040State.batteryDetails.voltage = byteBuffer.getShort();
        rp2040State.batteryDetails.safety_status = byteBuffer.get();
        rp2040State.batteryDetails.temperature = byteBuffer.getShort();
        rp2040State.batteryDetails.state_of_health = byteBuffer.get();
        rp2040State.batteryDetails.flags = byteBuffer.getShort();
        Log.v("serial", "Battery voltage: " + rp2040State.batteryDetails.voltage);
        Log.v("serial", "Battery voltage in V: " + rp2040State.batteryDetails.getVoltage());
        Log.v("serial", "Battery temperature: " + rp2040State.batteryDetails.temperature);
        rp2040State.chargeSideUSB.max77976_chg_details = byteBuffer.getInt();
        Log.v("serial", "max77976_chg_details: " + rp2040State.chargeSideUSB.max77976_chg_details);
    }
    private void onNack(byte[] bytes) {
        Log.d("serial", "parseNack");
    }
    private void onAck(byte[] bytes) {
        Log.d("serial", "parseAck");
    }


}
