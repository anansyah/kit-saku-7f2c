package id.eraai.news;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Terjemahan artikel ke bahasa lain, sama seperti tombol bahasa di website.
 *
 * Dua jalur pengambilan (tanpa kunci apa pun, tanpa biaya):
 *   1) Google Translate (mutu terbaik) — kalau diblokir/dibatasi, otomatis
 *   2) MyMemory (cadangan)
 *
 * Hasil terjemahan DISIMPAN per artikel+bahasa, jadi membuka ulang artikel
 * yang sama tidak memakai kuota lagi dan langsung tampil tanpa menunggu.
 *
 * Cara kerja: badan artikel dipecah jadi blok (paragraf, sub-judul, butir),
 * tiap blok diterjemahkan sendiri lalu disusun ulang. Ini lebih tahan gagal
 * daripada mengirim seluruh HTML sekaligus, dan menjaga susunan tetap rapi.
 */
final class Terjemah {

    /** Kode bahasa + nama tampil, mengikuti daftar di website. */
    static final String[] KODE = {
            "id", "en", "ar", "zh-CN", "ja", "ko", "es", "fr",
            "de", "pt", "ru", "tr", "hi", "ms", "th", "vi"
    };
    static final String[] NAMA = {
            "Bahasa Indonesia (asli)", "Inggris", "Arab", "Mandarin", "Jepang", "Korea",
            "Spanyol", "Prancis", "Jerman", "Portugis", "Rusia", "Turki",
            "Hindi", "Melayu", "Thailand", "Vietnam"
    };

    private static final String BERKAS = "era-terjemah";
    private static final int MAKS_UNDUH = 60;          // jumlah terjemahan tersimpan
    private static final int PANJANG_KIRIM = 1200;     // huruf per panggilan

    private static final Pattern BLOK = Pattern.compile(
            "(?is)<(p|h2|h3|h4|li|blockquote)\\b[^>]*>(.*?)</\\1>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    private Terjemah() { }

    static String nama(String kode) {
        for (int i = 0; i < KODE.length; i++) {
            if (KODE[i].equals(kode)) return NAMA[i];
        }
        return kode;
    }

    // ---------------- simpanan hasil ----------------

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(BERKAS, Context.MODE_PRIVATE);
    }

    private static String kunci(String tautan, String kode) {
        return kode + "|" + (tautan == null ? "" : tautan);
    }

    /** Terjemahan yang sudah pernah dibuat, atau null. */
    static String tersimpan(Context c, String tautan, String kode) {
        String v = sp(c).getString(kunci(tautan, kode), null);
        return (v == null || v.length() == 0) ? null : v;
    }

    private static void simpan(Context c, String tautan, String kode, String html) {
        SharedPreferences p = sp(c);
        p.edit().putString(kunci(tautan, kode), html).apply();
        // batasi jumlah simpanan supaya tidak menumpuk
        JSONArray arr = new JSONArray();
        try {
            arr = new JSONArray(p.getString("daftar", "[]"));
        } catch (Exception ignored) { }
        List<String> isi = new ArrayList<String>();
        for (int i = 0; i < arr.length(); i++) {
            String k = arr.optString(i, "");
            if (k.length() > 0 && !k.equals(kunci(tautan, kode))) isi.add(k);
        }
        isi.add(0, kunci(tautan, kode));
        JSONArray baru = new JSONArray();
        for (int i = 0; i < isi.size() && i < MAKS_UNDUH; i++) baru.put(isi.get(i));
        p.edit().putString("daftar", baru.toString()).apply();
    }

    static void bersihkan(Context c) {
        sp(c).edit().clear().apply();
    }

    // ---------------- penerjemahan ----------------

    /**
     * Terjemahkan badan artikel (HTML) ke bahasa `kode`.
     * Mengembalikan HTML terjemahan, atau null kalau gagal.
     */
    static String html(Context c, String tautan, String kode, String badan) {
        if (badan == null || badan.length() == 0) return null;
        if ("id".equals(kode)) return badan;

        String ada = tersimpan(c, tautan, kode);
        if (ada != null) return ada;

        // pecah jadi blok; kalau tidak ada blok sama sekali, pakai seluruh teks
        List<String> jenis = new ArrayList<String>();
        List<String> isi = new ArrayList<String>();
        Matcher m = BLOK.matcher(badan);
        while (m.find()) {
            String bersih = bersihkan(m.group(2));
            if (bersih.length() == 0) continue;
            jenis.add(m.group(1).toLowerCase());
            isi.add(bersih);
        }
        if (isi.isEmpty()) {
            String bersih = bersihkan(badan);
            if (bersih.length() == 0) return null;
            jenis.add("p");
            isi.add(bersih);
        }

        StringBuilder html = new StringBuilder();
        for (int i = 0; i < isi.size(); i++) {
            String hasil = terjemahTeks(isi.get(i), kode);
            if (hasil == null) return null;   // gagal total -> jangan tampilkan separuh
            String t = jenis.get(i);
            html.append('<').append(t).append('>')
                .append(aman(hasil))
                .append("</").append(t).append('>');
        }
        String keluar = html.toString();
        simpan(c, tautan, kode, keluar);
        return keluar;
    }

    /** Terjemahkan satu teks (otomatis dipecah kalau panjang). */
    static String terjemahTeks(String teks, String kode) {
        if (teks == null) return null;
        String t = teks.trim();
        if (t.length() == 0) return "";
        StringBuilder hasil = new StringBuilder();
        int pos = 0;
        while (pos < t.length()) {
            int akhir = Math.min(t.length(), pos + PANJANG_KIRIM);
            if (akhir < t.length()) {
                // potong di spasi terdekat supaya kata tidak terbelah
                int spasi = t.lastIndexOf(' ', akhir);
                if (spasi > pos + 200) akhir = spasi;
            }
            String potong = t.substring(pos, akhir).trim();
            String ter = sekali(potong, kode);
            if (ter == null) return null;
            if (hasil.length() > 0) hasil.append(' ');
            hasil.append(ter.trim());
            pos = akhir;
        }
        return hasil.toString();
    }

    /** Satu panggilan: coba Google dulu, lalu MyMemory. */
    private static String sekali(String teks, String kode) {
        String a = google(teks, kode);
        if (a != null && a.trim().length() > 0) return a;
        String b = mymemory(teks, kode);
        if (b != null && b.trim().length() > 0) return b;
        return null;
    }

    private static String google(String teks, String kode) {
        try {
            String u = "https://translate.googleapis.com/translate_a/single"
                    + "?client=gtx&sl=id&tl=" + URLEncoder.encode(kode, "UTF-8")
                    + "&dt=t&q=" + URLEncoder.encode(teks, "UTF-8");
            String j = ambil(u);
            if (j == null) return null;
            JSONArray akar = new JSONArray(j);
            JSONArray bagian = akar.optJSONArray(0);
            if (bagian == null) return null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bagian.length(); i++) {
                JSONArray x = bagian.optJSONArray(i);
                if (x != null && x.length() > 0) {
                    String s = x.optString(0, "");
                    if (s.length() > 0) sb.append(s);
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String mymemory(String teks, String kode) {
        try {
            String u = "https://api.mymemory.translated.net/get?q="
                    + URLEncoder.encode(teks, "UTF-8")
                    + "&langpair=id%7C" + URLEncoder.encode(kode, "UTF-8");
            String j = ambil(u);
            if (j == null) return null;
            JSONObject o = new JSONObject(j).optJSONObject("responseData");
            if (o == null) return null;
            String t = o.optString("translatedText", "");
            // MyMemory kadang mengembalikan pesan galat sebagai hasil
            if (t.toUpperCase().contains("MYMEMORY WARNING")
                    || t.toUpperCase().contains("QUERY LENGTH LIMIT")) return null;
            return t.length() > 0 ? t : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String ambil(String alamat) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(alamat).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(25000);
            c.setRequestProperty("User-Agent", "EraAIdailyNews/2.4 (Android)");
            int kode = c.getResponseCode();
            if (kode < 200 || kode >= 300) return null;
            InputStream is = c.getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
            r.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Buang tag di dalam blok, sisakan teksnya saja. */
    private static String bersihkan(String html) {
        String t = TAG.matcher(html == null ? "" : html).replaceAll(" ");
        t = t.replace("&#8212;", "—").replace("&#8211;", "–")
             .replace("&#8217;", "'").replace("&#8216;", "'")
             .replace("&#8220;", "\"").replace("&#8221;", "\"")
             .replace("&nbsp;", " ").replace("&amp;", "&")
             .replace("&lt;", "<").replace("&gt;", ">")
             .replace("&#39;", "'").replace("&quot;", "\"");
        return t.replaceAll("\\s+", " ").trim();
    }

    private static String aman(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
