package id.eraai.news;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Pengambil sederhana untuk mengunduh teks dari internet (di thread latar). */
final class Pengambil {
    private Pengambil() {}

    interface Dengar {
        void selesai(String isi, String galat);
    }

    static void unduh(final String alamat, final Dengar dengar) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String hasil = null;
                String galat = null;
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(alamat).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(20000);
                    c.setRequestProperty("User-Agent", "EraaiNews/1.0 (Android)");
                    int kode = c.getResponseCode();
                    InputStream is = (kode >= 200 && kode < 300) ? c.getInputStream() : c.getErrorStream();
                    StringBuilder sb = new StringBuilder();
                    if (is != null) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                        char[] buf = new char[8192];
                        int n;
                        while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
                        r.close();
                    }
                    if (kode >= 200 && kode < 300) {
                        hasil = sb.toString();
                    } else {
                        galat = "HTTP " + kode;
                    }
                    c.disconnect();
                } catch (Exception e) {
                    galat = e.getMessage() == null ? e.toString() : e.getMessage();
                }
                dengar.selesai(hasil, galat);
            }
        }, "pengambil");
        t.start();
    }
}
