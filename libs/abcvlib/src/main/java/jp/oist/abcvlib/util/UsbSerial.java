package jp.oist.abcvlib.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.google.flatbuffers.ByteBufferUtil;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.apache.commons.collections4.QueueUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.json.JSONException;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import jp.oist.abcvlib.util.ErrorHandler;

public class UsbSerial implements SerialInputOutputManager.Listener{

    private final Context context;
    private final UsbManager usbManager;
    private UsbSerialPort port;
    private int cnt = 0;
    private float[] pwm = new float[]{1.0f, 0.5f, 0.0f, -0.5f, -1.0f};
    private CircularFifoQueue<Byte> fifoQueue = new CircularFifoQueue<>(256);

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";

    public UsbSerial(Context context, UsbManager usbManager) throws IOException {
        this.context = context;
        // Find all available drivers from attached devices.
        this.usbManager = usbManager;
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice d: deviceList.values()){
            if (d.getManufacturerName().equals("Seeed") && d.getProductName().equals("Seeeduino XIAO")){
                Log.i("serial", "Found a XIAO. Connecting...");
                connect(d);
            }
            else if (d.getManufacturerName().equals("Raspberry Pi") && d.getProductName().equals("Pico Test Device")){
                Log.i("serial", "Found a Pico Test Device. Connecting...");
                connect(d);
            }
            else if (d.getManufacturerName().equals("Raspberry Pi") && d.getProductName().equals("Pico")){
                Log.i("serial", "Found a Pi. Connecting...");
                connect(d);
            }
        }

        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        BroadcastReceiver usbReceiver = new MyBroadcastReceiver();
        context.registerReceiver(usbReceiver, filter);
    }

    private void connect(UsbDevice device) throws IOException {
        if(usbManager.hasPermission(device)){
            UsbDeviceConnection connection = usbManager.openDevice(device);
            openPort(connection);
        }else{
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(device, permissionIntent);
        }
    }

    private void openPort(UsbDeviceConnection connection) {
        UsbSerialDriver driver = getDriver();
        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(115200, 8, 1, UsbSerialPort.PARITY_NONE);
            port.setDTR(true);
            this.port = port;
        } catch (IOException e) {
            e.printStackTrace();
        }

        SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
        usbIoManager.start();
        try {
            sendPacket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//            ScheduledExecutorServiceWithException executor =
//                    new ScheduledExecutorServiceWithException(1,
//                            new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY,
//                                    "serial"));
//            executor.scheduleAtFixedRate(() -> {
//                try {
//                    Log.i("serial", "Writing qp to serial port");
//                    port.write("a".getBytes(), 500);
//                    port.write("p".getBytes(), 500);
//                    byte[] recv = new byte[port.getReadEndpoint().getMaxPacketSize()];
//                    int len = port.read(recv, 2000);
//                    if (len > 0){
//                        Log.i("serial", "Read " + len + " bytes from serial");
//                        Log.i("serial", "Read " + new String(recv) + " from serial port");
//                    } else{
//                        Log.i("serial", "Zero bytes read");
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private UsbSerialDriver getDriver(){
        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(0x2886, 0x802F, CdcAcmSerialDriver.class); // Seeeduino XIAO
        customTable.addProduct(11914, 10, CdcAcmSerialDriver.class); // Raspberry Pi Pico
        customTable.addProduct(0x0000, 0x0001, CdcAcmSerialDriver.class); // Custom Raspberry Pi Pico
        UsbSerialProber prober = new UsbSerialProber(customTable);
        List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            ErrorHandler.eLog("Serial", "No USB Serial drivers found", new Exception(), true);
        }
        return availableDrivers.get(0);
    }

    @Override
    public void onNewData(byte[] data) {
        try {
            ReadBytesHandler(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class StartStopIndex{
        private int startIdx;
        private int stopIdx;
        private StartStopIndex(int startIdx, int stopIdx){
            this.startIdx = startIdx;
            this.stopIdx = stopIdx;
        }
    }

    private StartStopIndex startStopIndexSearch(byte[] data) throws IOException{
        StartStopIndex startStopIdx = new StartStopIndex(-1, -1);

        for (int i = 0; i < data.length; i++){
            if (data[i] == Commands.STOP.getHexValue()){
                startStopIdx.stopIdx = i;
            }
            else if (data[i] == Commands.START.getHexValue()){
                startStopIdx.startIdx = i;
            }
        }
        // Error handling for startIdx or stopIdx not found
        if (startStopIdx.startIdx == -1 || startStopIdx.stopIdx == -1){
            Log.d("serial", "startIdx or stopIdx not found");
        }
        // Error handling for startIdx after stopIdx
        else if (startStopIdx.startIdx > startStopIdx.stopIdx){
            Log.d("serial", "startIdx after stopIdx");
        }
        // Error handling for startIdx and stopIdx too close together
        else if (startStopIdx.stopIdx - startStopIdx.startIdx < 2){
            Log.d("serial", "startIdx and stopIdx too close together");
        }
        // Error handling for startIdx and stopIdx too far apart
        else if (startStopIdx.stopIdx - startStopIdx.startIdx > 100){
            Log.d("serial", "startIdx and stopIdx too far apart");
        }
        return startStopIdx;
    }

    private void ReadBytesHandler(byte[] data) throws IOException {
        //Ensure a proper start and stop mark present before adding anything to the fifoQueue
        StartStopIndex startStopIdx = startStopIndexSearch(data);
        // Add the data from between the start and stop marks to the fifoQueue
        if (startStopIdx.startIdx != -1 && startStopIdx.stopIdx != -1){
            for (int i = startStopIdx.startIdx + 1; i < startStopIdx.stopIdx; i++){

                if (fifoQueue.isAtFullCapacity()){
                    Log.e("serial", "fifoQueue is full");
                    throw new RuntimeException("fifoQueue is full");
                }
                fifoQueue.add(data[i]);

                // Run the command on a thread handler to allow the queue to keep being added to

                parsePacket(data);
                sendPacket();
            }
        }
    }

    private enum Commands{
        DO_NOTHING((byte) 0x00),
        GET_CHARGE_DETAILS((byte) 0x01),
        GET_LOGS((byte) 0x02),
        VARIOUS((byte) 0x03),
        GET_ENCODER_COUNTS((byte) 0x04),
        RESET_ENCODER_COUNTS((byte) 0x05),
        SET_MOTOR_LEVELS((byte) 0x06),
        SET_MOTOR_BRAKE((byte) 0x07),
        GET_USB_VOLTAGE((byte) 0x08),
        ON_WIRELESS_ATTACHED((byte) 0x09),
        ON_WIRELESS_DETACHED((byte) 0x0A),
        ON_MOTOR_FAULT((byte) 0x0B),
        ON_USB_ERROR((byte) 0x0C),

        NACK((byte) 0xFC),
        ACK((byte) 0xFD),
        START((byte) 0xFE),
        STOP((byte) 0xFF);

        private final byte hexValue;
        Commands(byte hexValue){
            this.hexValue = hexValue;
        }
        public byte getHexValue(){
            return hexValue;
        }

        private static final Map<Byte, Commands> map = new HashMap<>();

        static {
            for (Commands command : Commands.values()) {
                map.put(command.getHexValue(), command);
            }
        }

        public static Commands getEnumByValue(byte value) {
            return map.get(value);
        }
    }

    private void parsePacket(byte[] data) {
        // The first byte after the start mark is the command
        Commands command = Commands.getEnumByValue(data[1]);
        switch (command){
            case DO_NOTHING:
                Log.d("serial", "parseDoNothing");
                break;
            case GET_CHARGE_DETAILS:
                parseGetChargeDetails(data);
                break;
            case GET_LOGS:
                parseGetLogs(data);
                break;
            case VARIOUS:
                parseVarious(data);
                break;
            case GET_ENCODER_COUNTS:
                parseGetEncoderCounts(data);
                break;
            case RESET_ENCODER_COUNTS:
                parseResetEncoderCounts(data);
                break;
            case SET_MOTOR_LEVELS:
                parseSetMotorLevels(data);
                break;
            case SET_MOTOR_BRAKE:
                parseSetMotorBrake(data);
                break;
            case GET_USB_VOLTAGE:
                parseGetUSBVoltage(data);
                break;
            case ON_WIRELESS_ATTACHED:
                parseOnWirelessAttached(data);
                break;
            case ON_WIRELESS_DETACHED:
                parseOnWirelessDetached(data);
                break;
            case ON_MOTOR_FAULT:
                parseOnMotorFault(data);
                break;
            case ON_USB_ERROR:
                parseOnUSBError(data);
                break;
            case NACK:
                Log.w("serial", "Nack issued from device");
                break;
            case ACK:
                Log.d("serial", "parseAck");
                break;
            case START:
                Log.e("serial", "parseStart. Start should never be a command");
                break;
            case STOP:
                Log.e("serial", "parseStop. Stop should never be a command");
                break;
            default:
                Log.e("serial", "parsePacket. Command not found");
                break;
        }
    }

    protected void sendPacket() throws IOException {
        byte[] response = {(byte) Commands.GET_CHARGE_DETAILS.getHexValue()};
        port.write(response, 1000);
        cnt++;
    }

    private void parseDoNothing(byte[] bytes) {
        Log.d("serial", "parseDoNothing");
    }
    private void parseGetChargeDetails(byte[] bytes) {
        Log.d("serial", "parseGetChargeDetails");
    }
    private void parseGetLogs(byte[] bytes) {
        Log.d("serial", "parseGetLogs");
    }
    private void parseVarious(byte[] bytes) {
        Log.d("serial", "parseVarious");
    }
    private void parseGetEncoderCounts(byte[] bytes) {
        Log.d("serial", "parseGetEncoderCounts");
    }
    private void parseResetEncoderCounts(byte[] bytes) {
        Log.d("serial", "parseResetEncoderCounts");
    }
    private void parseSetMotorLevels(byte[] bytes) {
        Log.d("serial", "parseSetMotorLevels");
    }
    private void parseSetMotorBrake(byte[] bytes) {
        Log.d("serial", "parseSetMotorBrake");
    }
    private void parseGetUSBVoltage(byte[] bytes) {
        Log.d("serial", "parseGetUSBVoltage");
    }
    private void parseOnWirelessAttached(byte[] bytes) {
        Log.d("serial", "parseOnWirelessAttached");
    }
    private void parseOnWirelessDetached(byte[] bytes) {
        Log.d("serial", "parseOnWirelessDetached");
    }
    private void parseOnMotorFault(byte[] bytes) {
        Log.d("serial", "parseOnMotorFault");
    }
    private void parseOnUSBError(byte[] bytes) {
        Log.d("serial", "parseOnUSBError");
    }
    private void parseNack(byte[] bytes) {
        Log.d("serial", "parseNack");
    }
    private void parseAck(byte[] bytes) {
        Log.d("serial", "parseAck");
    }
    @Override
    public void onRunError(Exception e) {
        Log.e("serial", "error: " + e.getLocalizedMessage());
        e.printStackTrace();
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        try {
                            connect(device);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }else {
                Log.d("serial", "permission denied for device " + device);
            }
        }
    }
}
