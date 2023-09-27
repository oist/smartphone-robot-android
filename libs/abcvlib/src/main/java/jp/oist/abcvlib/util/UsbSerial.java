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

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.SerialTimeoutException;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;


public class UsbSerial implements SerialInputOutputManager.Listener{

    private final Context context;
    private final UsbManager usbManager;
    private UsbSerialPort port;
    private SerialReadyListener serialReadyListener;
    private int cnt = 0;
    private float[] pwm = new float[]{1.0f, 0.5f, 0.0f, -0.5f, -1.0f};
    private SerialCommManager serialCommManager;
    private byte[] responseData;
    protected CircularFifoQueue<byte[]> fifoQueue = new CircularFifoQueue<>(256);
    int timeout = 1000; //1s
    int totalBytesRead = 0; // Track total bytes read
    ByteBuffer packetBuffer = ByteBuffer.allocate(1024); // Adjust the buffer size as needed
    boolean packetFound = false;
    //Ensure a proper start and stop mark present before adding anything to the fifoQueue
    StartStopIndex startStopIdx;
    // Used to signal when a new packet is available between thread handling sending and receiving
    private final Object newPacketReeceived = new Object();

    private class StartStopIndex{
        private int startIdx;
        private int stopIdx;
        private StartStopIndex(int startIdx, int stopIdx){
            this.startIdx = startIdx;
            this.stopIdx = stopIdx;
        }
    }

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";

    public UsbSerial(Context context,
                     UsbManager usbManager,
                     SerialReadyListener serialReadyListener,
                     SerialCommManager serialCommManager) throws IOException {
        this.context = context;
        this.serialReadyListener = serialReadyListener;
        this.serialCommManager = serialCommManager;
        // Find all available drivers from attached devices.
        this.usbManager = usbManager;
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);


        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.isEmpty()){
            throw new IOException("No USB devices found");
        }

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
            Log.i("serial", "Has permission to connect to device");
            UsbDeviceConnection connection = usbManager.openDevice(device);
            openPort(connection);
        }else{
            Log.i("serial", "Requesting permission to connect to device");
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(device, permissionIntent);
        }
    }

    private void openPort(UsbDeviceConnection connection) {
        Log.i("serial", "Opening port");
        UsbSerialDriver driver = getDriver();
        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(115200, 8, 1, UsbSerialPort.PARITY_NONE);
            port.setDTR(true);
            this.port = port;
            assert port != null;
            SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
            usbIoManager.start();
            serialReadyListener.onSerialReady(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            if (verifyPacket(data)){
                Log.d("serial", "Packet verified");
                synchronized (newPacketReeceived){
                    newPacketReeceived.notify();
                }
            }
            else{
                Log.d("serial", "Incomplete Packet. Waiting for more data");
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(byte[] packet, int timeout) throws IOException {
        port.write(packet, timeout);
        awaitResponse();
    }

    /**
     * Blocks until a response is received
     */
    public void awaitResponse() {
        try {
            // Wait until packet is available
            Log.i("serial", "Waiting for packet to be available");
            synchronized (newPacketReeceived){
                newPacketReeceived.wait();
            }
            Log.i("serial", "Packet available");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private StartStopIndex startStopIndexSearch(byte[] data) throws IOException {
        StartStopIndex startStopIdx = new StartStopIndex(-1, -1);

        for (int i = 0; i < data.length; i++){
            if (data[i] == UsbSerialProtocol.STOP.getHexValue()){
                startStopIdx.stopIdx = i;
            }
            else if (data[i] == UsbSerialProtocol.START.getHexValue()){
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

    /**
     * @param bytes to be verified to contain a valid packet. If so, the packet is added to the fifoQueue
     * @return 0 if successful, -1 if unsuccessful
     * @throws IOException if fifoQueue is full
     */
    protected boolean verifyPacket(byte[] bytes) throws IOException {
        packetBuffer.put(bytes);

        startStopIdx = startStopIndexSearch(packetBuffer.array());
        if (startStopIdx.startIdx != -1 && startStopIdx.stopIdx != -1){
            // If the packet is not empty, copy the contents of the packet into the byte[]
            if (startStopIdx.stopIdx - (startStopIdx.startIdx + 1) >= 0) {
                // Extract the packet and add it to the fifoQueue
                packetBuffer.position(startStopIdx.startIdx);
                ByteBuffer packet = packetBuffer.slice();
                packet.limit(startStopIdx.stopIdx - startStopIdx.startIdx + 1);
                packetBuffer.clear();
                if (fifoQueue.isAtFullCapacity()) {
                    Log.e("serial", "fifoQueue is full");
                    throw new RuntimeException("fifoQueue is full");
                } else {
                    fifoQueue.add(packet.array());
                    return true;
                }
            }
        }
        return false;
    }

    public int read(byte[] packet, int timeout) throws IOException {
        return port.read(packet, timeout);
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
