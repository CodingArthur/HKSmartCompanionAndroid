package hk.edu.hku.cs7506.smartcompanion.data.network;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OpenDataHttpClient {
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public OpenDataHttpClient() {
        this(10000, 10000);
    }

    public OpenDataHttpClient(int connectTimeoutMillis, int readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String get(String url) throws Exception {
        return getResponse(url).getBody();
    }

    public ResponseData getResponse(String url) throws Exception {
        HttpURLConnection connection = openConnection(url, "GET");
        return execute(connection, null);
    }

    public String postJson(String url, String body) throws Exception {
        return postJsonResponse(url, body).getBody();
    }

    public ResponseData postJsonResponse(String url, String body) throws Exception {
        HttpURLConnection connection = openConnection(url, "POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        return execute(connection, body);
    }

    private HttpURLConnection openConnection(String url, String method) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setRequestMethod(method);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/json,text/xml,text/csv,text/plain,*/*");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        connection.setRequestProperty("User-Agent", "HKSmartCompanion/1.0");
        return connection;
    }

    private ResponseData execute(HttpURLConnection connection, String body) throws Exception {
        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
        }

        int statusCode = connection.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("HTTP " + statusCode + " for " + connection.getURL());
        }

        long lastModifiedEpochMillis = connection.getHeaderFieldDate("Last-Modified", -1);
        long fetchedAtEpochMillis = System.currentTimeMillis();
        try (InputStream inputStream = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return new ResponseData(builder.toString(), lastModifiedEpochMillis, fetchedAtEpochMillis);
        } finally {
            connection.disconnect();
        }
    }

    public static class ResponseData {
        private final String body;
        private final long lastModifiedEpochMillis;
        private final long fetchedAtEpochMillis;

        ResponseData(String body, long lastModifiedEpochMillis, long fetchedAtEpochMillis) {
            this.body = body;
            this.lastModifiedEpochMillis = lastModifiedEpochMillis;
            this.fetchedAtEpochMillis = fetchedAtEpochMillis;
        }

        public String getBody() {
            return body;
        }

        public long getLastModifiedEpochMillis() {
            return lastModifiedEpochMillis;
        }

        public long getFetchedAtEpochMillis() {
            return fetchedAtEpochMillis;
        }
    }
}
