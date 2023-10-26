package jp.oist.abcvlib.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
packet[0] = START marker
packet[1] = AndroidToRP2040Command
packet[2..9] = payload
packet[10] = STOP marker

 */
class AndroidToRP2040Packet {
    // + 1 for command
    public static final int AndroidToRP2040PayloadSize = (Float.BYTES * 2) + 1;
    // Making room for start and stop marks
    public static int packetSize = AndroidToRP2040PayloadSize + 2;
    private AndroidToRP2040Command command;
    // make room for packet_type, and start and stop marks
    protected ByteBuffer payload = ByteBuffer.allocate(AndroidToRP2040PayloadSize);
    private ByteBuffer packet = ByteBuffer.allocate(packetSize);

    public AndroidToRP2040Packet(){
        //rp2040 is little endian whereas Java is big endian. This is to ensure that the bytes are
        //written in the correct order for parsing on the rp2040
        payload.order(ByteOrder.LITTLE_ENDIAN);
        packet.order(ByteOrder.LITTLE_ENDIAN);
        packet.put(AndroidToRP2040Command.START.getHexValue());
    }

    protected void setCommand(AndroidToRP2040Command command){
        this.command = command;
        payload.put(this.command.getHexValue());
    }

    // Add data to packet then the end mark
    protected byte[] packetTobytes(){
        payload.rewind();
        packet.put(payload);
        packet.put(AndroidToRP2040Command.STOP.getHexValue());
        return packet.array();
    }

    protected void clear(){
        packet.clear();
        packet.put(AndroidToRP2040Command.START.getHexValue());
        payload.clear();
    }
}
