package jp.oist.abcvlib.util;

public class HttpExtraInfo {

    public static class FileInfo {
        private final String fileName;
        private final int fileSize;
        private final String fileType;

        public FileInfo(String fileName, int fileSize, String fileType) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.fileType = fileType;
        }

        public String getFileName() {
            return fileName;
        }

        public int getFileSize() {
            return fileSize;
        }

        public String getFileType() {
            return fileType;
        }
    }

    public static class FlatbufferInfo {
        private final String flatbufferName;
        private final int flatbufferSize;

        public FlatbufferInfo(String flatbufferName, int flatbufferSize) {
            this.flatbufferName = flatbufferName;
            this.flatbufferSize = flatbufferSize;
        }

        public String getFlatbufferName() {
            return flatbufferName;
        }

        public int getFlatbufferSize() {
            return flatbufferSize;
        }
    }
}
