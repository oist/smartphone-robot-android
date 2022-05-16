package jp.oist.abcvlib.core;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class UsbSerial implements SerialInputOutputManager.Listener{

    private final Context context;
    private final UsbManager usbManager;
    private UsbSerialPort port;
    private int cnt = 0;
    private float[] pwm = new float[]{1.0f, 0.5f, 0.0f, -0.5f, -1.0f};

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
            port.setParameters(9600, 8, 1, UsbSerialPort.PARITY_NONE);
            port.setDTR(true);
            this.port = port;
        } catch (IOException e) {
            e.printStackTrace();
        }

        SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
        usbIoManager.start();

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
        customTable.addProduct(0x2886, 0x802F, CdcAcmSerialDriver.class);
        UsbSerialProber prober = new UsbSerialProber(customTable);
        List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);
        return availableDrivers.get(0);
    }

    @Override
    public void onNewData(byte[] data) {
        try {
            ReadLineHandler(data);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private String readBuffer = "";

    private void ReadLineHandler(byte[] data) throws JSONException, IOException {
        String incoming = new String(data, StandardCharsets.US_ASCII);
//        Log.d("serial", "incoming: " + incoming);
        readBuffer += incoming;
        while(readBuffer.length() > 0 && readBuffer.contains("\n")){
            String jsonString = readBuffer.substring(0, readBuffer.indexOf("\n")).trim();
            //Handle this line here
            parsePacket(jsonString);
            sendResponse();
            Log.d("serial", "thisLine: " + jsonString);
            //Trim the processed line from the readBuffer
            readBuffer = readBuffer.substring(readBuffer.indexOf("\n") + 1);
//            Log.d("serial", "readBuffer: " + readBuffer);
        }
    }

    protected void sendResponse() throws IOException {
        float val = pwm[cnt % pwm.length];
        String responseStr = "{\"time\":";
        responseStr = responseStr.concat(String.valueOf(System.nanoTime()));
        responseStr = responseStr.concat(",\"wheelL\":");
        responseStr = responseStr.concat(Float.toString(val));
        responseStr = responseStr.concat(",\"wheelR\":0.8");
        responseStr = responseStr.concat("}\n");
        byte[] response = responseStr.getBytes(StandardCharsets.US_ASCII);
        port.write(response, 1000);
        cnt++;
    }

    private void parsePacket(String jsonString) {
        try{
            JSONObject jsonObject = new JSONObject(jsonString);
            Log.d("serial", "QuadEncoder1 : " + jsonObject.get("Q1"));
            Log.d("serial", "QuadEncoder2 : " + jsonObject.get("Q2"));
            Log.d("serial", "Battery : " + jsonObject.get("B"));
            Log.d("serial", "Charger : " + jsonObject.get("Ch"));
            Log.d("serial", "Coil : " + jsonObject.get("Co"));
        }catch (JSONException e){
            Log.d("Serial", "Not Jsonstring: " + jsonString);
        }
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
