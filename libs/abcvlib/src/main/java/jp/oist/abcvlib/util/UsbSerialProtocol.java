package jp.oist.abcvlib.util;

import java.util.HashMap;
import java.util.Map;

enum UsbSerialProtocol {
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
    UsbSerialProtocol(byte hexValue){
        this.hexValue = hexValue;
    }
    public byte getHexValue(){
        return hexValue;
    }

    private static final Map<Byte, UsbSerialProtocol> map = new HashMap<>();

    static {
        for (UsbSerialProtocol command : UsbSerialProtocol.values()) {
            map.put(command.getHexValue(), command);
        }
    }

    public static UsbSerialProtocol getEnumByValue(byte value) {
        return map.get(value);
    }
}
