package jp.oist.abcvlib.util;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jp.oist.abcvlib.core.BuildConfig;

public class HttpConnection {

    private final ExecutorService executorService;
    private final HttpCallback callback;
    private final Context context;

    public HttpConnection(Context context, HttpCallback callback) {
        this.executorService = Executors.newSingleThreadExecutor();
        this.context = context;
        this.callback = callback;
    }

    public interface HttpCallback {
        void onFileReceived(String filename, byte[] fileData);
        void onSuccess(String response);
        void onError(String error);
    }

    public void sendData(byte[] data, HttpDataType dataType, Object extraInfo) {
        executorService.execute(() -> {
            String urlString = "http://" + BuildConfig.IP + ":" + BuildConfig.PORT;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/octet-stream");
                urlConnection.setRequestProperty("Content-Length", Integer.toString(data.length));
                urlConnection.setRequestProperty("Data-Type", dataType.getType());

                // Add additional headers based on data type
                if (dataType == HttpDataType.FILE && extraInfo instanceof HttpExtraInfo.FileInfo) {
                    HttpExtraInfo.FileInfo fileInfo = (HttpExtraInfo.FileInfo) extraInfo;
                    urlConnection.setRequestProperty("File-Name", fileInfo.getFileName());
                    urlConnection.setRequestProperty("File-Size", String.valueOf(fileInfo.getFileSize()));
                    urlConnection.setRequestProperty("File-Type", fileInfo.getFileType());
                } else if (dataType == HttpDataType.FLATBUFFER && extraInfo instanceof HttpExtraInfo.FlatbufferInfo) {
                    HttpExtraInfo.FlatbufferInfo flatbufferInfo = (HttpExtraInfo.FlatbufferInfo) extraInfo;
                    urlConnection.setRequestProperty("Flatbuffer-Name", flatbufferInfo.getFlatbufferName());
                    urlConnection.setRequestProperty("Flatbuffer-Size", String.valueOf(flatbufferInfo.getFlatbufferSize()));
                }

                // Send the data
                try (OutputStream os = urlConnection.getOutputStream()) {
                    os.write(data, 0, data.length);
                }

                // Get the response
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream is = urlConnection.getInputStream()) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                        }
                        final String response = new String(byteArrayOutputStream.toByteArray());
                        callback.onSuccess(response);
                    }
                } else {
                    try (InputStream errorStream = urlConnection.getErrorStream()) {
                        if (errorStream != null) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            ByteArrayOutputStream errorOutputStream = new ByteArrayOutputStream();
                            while ((bytesRead = errorStream.read(buffer)) != -1) {
                                errorOutputStream.write(buffer, 0, bytesRead);
                            }
                            final String errorResponse = new String(errorOutputStream.toByteArray());
                            callback.onError("Error: " + responseCode + " " + errorResponse);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Exception: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    public void getData(String filename) {
        executorService.execute(() -> {
            String urlString = "http://" + BuildConfig.IP + ":" + BuildConfig.PORT + "/file?filename=" + filename;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Get the file size from response headers
                    String contentDisposition = urlConnection.getHeaderField("Content-Disposition");
                    boolean isFile = contentDisposition != null && contentDisposition.startsWith("attachment;");
                    if (isFile) {
                        // Handle large files by saving to disk
                        File file = new File(context.getExternalFilesDir(null), filename);
                        try (InputStream is = urlConnection.getInputStream();
                             FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                            callback.onSuccess("File saved to: " + file.getAbsolutePath());
                        }
                    } else {
                        // Handle small files by reading into memory
                        try (InputStream is = urlConnection.getInputStream();
                             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                byteArrayOutputStream.write(buffer, 0, bytesRead);
                            }
                            final byte[] fileData = byteArrayOutputStream.toByteArray();
                            callback.onFileReceived(filename, fileData);
                        }
                    }
                } else {
                    try (InputStream errorStream = urlConnection.getErrorStream()) {
                        if (errorStream != null) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            ByteArrayOutputStream errorOutputStream = new ByteArrayOutputStream();
                            while ((bytesRead = errorStream.read(buffer)) != -1) {
                                errorOutputStream.write(buffer, 0, bytesRead);
                            }
                            final String errorResponse = new String(errorOutputStream.toByteArray());
                            callback.onError("Error: " + responseCode + " " + errorResponse);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Exception: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }
}
