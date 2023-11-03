package jp.oist.abcvlib.util;

public class RP2040ToAndroidPacket {
    public static class Offsets {
        public static final int START_MARKER = 0;
        public static final int PACKET_TYPE = 1;
        public static final int DATA_SIZE = 2;
        public static final int DATA = 4;
        public static final int END_MARKER = 0; // Adjust this as needed
    }
    public static class Sizes {
        // Define sizes for fields
        public static final int START_MARKER = 1;
        public static final int PACKET_TYPE = 1;
        public static final int DATA_SIZE = 2;
        public static final int END_MARKER = 1;
    }
}
