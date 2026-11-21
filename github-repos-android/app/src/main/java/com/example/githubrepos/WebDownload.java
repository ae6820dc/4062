package com.example.githubrepos;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.webkit.CookieManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

final class WebDownload {
    private WebDownload() {}

    static boolean save(ContentResolver resolver, Uri destination, String address, String userAgent) {
        HttpsURLConnection connection = null;
        try {
            connection = connect(address, userAgent);
            try (InputStream input = connection.getInputStream();
                 OutputStream output = resolver.openOutputStream(destination, "wt")) {
                if (output == null) throw new IOException("Destination unavailable");
                byte[] buffer = new byte[32768];
                long bytes = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    bytes += count;
                }
                String expected = connection.getHeaderField("Content-Length");
                if (expected != null && bytes != Long.parseLong(expected)) {
                    throw new IOException("Incomplete download");
                }
            }
            return true;
        } catch (IOException | RuntimeException failed) {
            try {
                DocumentsContract.deleteDocument(resolver, destination);
            } catch (IOException | RuntimeException unavailable) {
                // Some document providers do not support deleting a failed download.
            }
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static HttpsURLConnection connect(String address, String userAgent) throws IOException {
        URL url = new URL(address);
        for (int redirects = 0; redirects <= 8; redirects++) {
            if (!"https".equalsIgnoreCase(url.getProtocol()) || url.getUserInfo() != null) {
                throw new IOException("Invalid download URL");
            }
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            try {
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("Accept-Encoding", "identity");
                if (userAgent != null) connection.setRequestProperty("User-Agent", userAgent);
                String cookie = CookieManager.getInstance().getCookie(url.toString());
                if (cookie != null) connection.setRequestProperty("Cookie", cookie);
                int status = connection.getResponseCode();
                if (status == 200) return connection;
                String redirect = connection.getHeaderField("Location");
                if ((status == 301 || status == 302 || status == 303 || status == 307 || status == 308)
                        && redirect != null) {
                    url = new URL(url, redirect);
                } else {
                    throw new IOException("Download HTTP status " + status);
                }
            } catch (IOException | RuntimeException failed) {
                connection.disconnect();
                throw failed;
            }
            connection.disconnect();
        }
        throw new IOException("Too many redirects");
    }
}
