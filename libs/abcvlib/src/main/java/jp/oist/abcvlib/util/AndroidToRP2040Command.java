package jp.oist.abcvlib.util;

import java.util.HashMap;
import java.util.Map;

enum AndroidToRP2040Command {
    GET_LOG((byte) 0x00),
    SET_MOTOR_LEVELS((byte) 0x01),
    RESET_STATE((byte) 0x02),
    GET_STATE((byte) 0x03),
    NACK((byte) 0xFC),
    ACK((byte) 0xFD),
    START((byte) 0xFE),
    STOP((byte) 0xFF);

    private final byte hexValue;
    AndroidToRP2040Command(byte hexValue){
        this.hexValue = hexValue;
    }
    public byte getHexValue(){
        return hexValue;
    }

    private static final Map<Byte, AndroidToRP2040Command> map = new HashMap<>();

    static {
        for (AndroidToRP2040Command command : AndroidToRP2040Command.values()) {
            map.put(command.getHexValue(), command);
        }
    }

    public static AndroidToRP2040Command getEnumByValue(byte value) {
        return map.get(value);
    }
}
