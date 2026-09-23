package id.eraai.news;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Pemuat gambar berita dengan simpanan sementara di memori. */
final class Gambar {

    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(14 * 1024 * 1024) {
                @Override
                protected int sizeOf(String kunci, Bitmap b) {
                    return b.getByteCount();
                }
            };

    private static final Map<ImageView, String> TUGAS =
            Collections.synchronizedMap(new WeakHashMap<ImageView, String>());

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private Gambar() {}

    static void muat(final String url, final ImageView target) {
        if (url == null || url.length() == 0) {
            target.setImageDrawable(null);
            return;
        }
        Bitmap ada = CACHE.get(url);
        if (ada != null) {
            target.setImageBitmap(ada);
            return;
        }
        target.setImageDrawable(null);
        TUGAS.put(target, url);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap b = unduh(url);
                if (b != null) CACHE.put(url, b);
                UI.post(new Runnable() {
                    @Override
                    public void run() {
                        String tugas = TUGAS.get(target);
                        if (tugas != null && tugas.equals(url)) {
                            if (b != null) target.setImageBitmap(b);
                        }
                    }
                });
            }
        }, "gambar");
        t.start();
    }

    private static Bitmap unduh(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(12000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "EraAIdailyNews/2.3 (Android)");
            c.setInstanceFollowRedirects(true);
            if (c.getResponseCode() < 200 || c.getResponseCode() >= 300) return null;
            InputStream is = new BufferedInputStream(c.getInputStream(), 16384);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            int total = 0;
            while ((n = is.read(buf)) != -1) {
                total += n;
                if (total > 6 * 1024 * 1024) break; // batas aman
                bos.write(buf, 0, n);
            }
            is.close();
            byte[] data = bos.toByteArray();

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int lebar = o.outWidth, tinggi = o.outHeight;
            int sampel = 1;
            while (lebar / sampel > 900 || tinggi / sampel > 900) sampel *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sampel;
            o2.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(data, 0, data.length, o2);
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
