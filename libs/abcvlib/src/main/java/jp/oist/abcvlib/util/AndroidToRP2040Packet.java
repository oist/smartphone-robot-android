package jp.oist.abcvlib.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class AndroidToRP2040Packet {
    public static int packetSize = Float.BYTES * 2 + 3;
    private byte packetType;
    // make room for packet_type, and start and stop marks
    protected ByteBuffer data = ByteBuffer.allocate(Float.BYTES * 2);
    private ByteBuffer packet = ByteBuffer.allocate(packetSize);

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
