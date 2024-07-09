package jp.oist.abcvlib.util;

public enum HttpDataType {
    STRING("string"),
    FILE("file"),
    FLATBUFFER("flatbuffer");

    private final String type;

    HttpDataType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
