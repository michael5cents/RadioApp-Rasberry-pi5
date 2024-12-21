package com.radio;

import java.io.*;
import java.net.*;
import java.util.*;
import javafx.application.Platform;

public class IcyMetadataReader {
    private final String streamUrl;
    private volatile boolean running = true;
    private MetadataCallback callback;

    public interface MetadataCallback {
        void onMetadataUpdate(String title, String artist);
    }

    public IcyMetadataReader(String streamUrl, MetadataCallback callback) {
        this.streamUrl = streamUrl;
        this.callback = callback;
    }

    public void start() {
        Thread thread = new Thread(this::readMetadata);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
    }

    private void readMetadata() {
        while (running) {
            try {
                URL url = new URL(streamUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Icy-MetaData", "1");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.connect();

                // Get metadata interval
                int metaInt = getMetadataInterval(connection.getHeaderFields());
                if (metaInt > 0) {
                    readStreamAndMetadata(connection.getInputStream(), metaInt);
                }

                connection.disconnect();
            } catch (Exception e) {
                try {
                    Thread.sleep(5000); // Wait before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private int getMetadataInterval(Map<String, List<String>> headers) {
        List<String> values = headers.get("icy-metaint");
        if (values != null && !values.isEmpty()) {
            try {
                return Integer.parseInt(values.get(0));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private void readStreamAndMetadata(InputStream stream, int metaInt) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        int metaLength;

        while (running) {
            // Skip audio data
            long toSkip = metaInt - bytesRead;
            while (toSkip > 0) {
                long skipped = stream.skip(toSkip);
                if (skipped <= 0) break;
                toSkip -= skipped;
            }

            // Read metadata length byte
            metaLength = stream.read() * 16;

            if (metaLength > 0) {
                // Read metadata
                byte[] metaData = new byte[metaLength];
                int totalRead = 0;
                while (totalRead < metaLength) {
                    int read = stream.read(metaData, totalRead, metaLength - totalRead);
                    if (read <= 0) break;
                    totalRead += read;
                }

                String metaString = new String(metaData, 0, totalRead).trim();
                parseMetadata(metaString);
            }

            bytesRead = 0;
        }
    }

    private void parseMetadata(String metadata) {
        if (metadata.contains("StreamTitle='")) {
            int titleStart = metadata.indexOf("StreamTitle='") + 12;
            int titleEnd = metadata.indexOf("';", titleStart);
            if (titleStart >= 0 && titleEnd >= 0) {
                String streamTitle = metadata.substring(titleStart, titleEnd).trim();
                
                // Split title and artist if the format is "title - artist"
                String[] parts = streamTitle.split(" - ", 2);
                if (parts.length == 2) {
                    String title = parts[0].trim();
                    String artist = parts[1].trim();
                    if (callback != null) {
                        Platform.runLater(() -> callback.onMetadataUpdate(title, artist));
                    }
                }
            }
        }
    }
}
