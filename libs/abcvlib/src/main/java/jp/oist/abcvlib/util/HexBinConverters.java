package jp.oist.abcvlib.util;

public class HexBinConverters {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        int newLength = 6;
        char[] hexChars = new char[bytes.length * newLength];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * newLength] = '0';
            hexChars[j * newLength + 1] = 'x';
            hexChars[j * newLength + 2] = HEX_ARRAY[v >>> 4];;
            hexChars[j * newLength + 3] = HEX_ARRAY[v & 0x0F];
            hexChars[j * newLength + 4] = ',';
            hexChars[j * newLength + 5] = ' ';
        }
        return new String(hexChars);
    }
}
