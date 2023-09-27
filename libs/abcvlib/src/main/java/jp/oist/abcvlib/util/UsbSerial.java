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
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
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


//        byte[] intialization = new byte[]{Commands.DO_NOTHING.getHexValue()};
//        sendPacket(intialization);


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
            serialCommManager.verifyPacket(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(byte[] packet, int timeout) throws IOException {
        port.write(packet, timeout);
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
