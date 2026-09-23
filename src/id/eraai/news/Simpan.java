package id.eraai.news;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Simpan berita untuk dibaca tanpa internet. */
final class Simpan {

    private static final String NAMA = "eraai_simpan";
    private static final String KUNCI = "daftar";
    private static final int MAKS = 40;

    private Simpan() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAMA, Context.MODE_PRIVATE);
    }

    static List<ParseFeed.Berita> semua(Context c) {
        List<ParseFeed.Berita> keluar = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp(c).getString(KUNCI, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ParseFeed.Berita b = new ParseFeed.Berita();
                b.judul = o.optString("judul");
                b.tautan = o.optString("tautan");
                b.waktuMentah = o.optString("waktu");
                b.label = o.optString("label");
                b.gambar = o.optString("gambar");
                b.cuplikan = o.optString("cuplikan");
                b.isi = o.optString("isi");
                b.kredit = o.optString("kredit");
                keluar.add(b);
            }
        } catch (Exception ignored) {
        }
        return keluar;
    }

    static boolean ada(Context c, String tautan) {
        for (ParseFeed.Berita b : semua(c)) {
            if (b.tautan != null && b.tautan.equals(tautan)) return true;
        }
        return false;
    }

    static void tambah(Context c, ParseFeed.Berita b) {
        List<ParseFeed.Berita> isi = semua(c);
        for (int i = isi.size() - 1; i >= 0; i--) {
            if (isi.get(i).tautan != null && isi.get(i).tautan.equals(b.tautan)) isi.remove(i);
        }
        isi.add(0, b);
        while (isi.size() > MAKS) isi.remove(isi.size() - 1);
        tulis(c, isi);
    }

    static void hapus(Context c, String tautan) {
        List<ParseFeed.Berita> isi = semua(c);
        for (int i = isi.size() - 1; i >= 0; i--) {
            if (isi.get(i).tautan != null && isi.get(i).tautan.equals(tautan)) isi.remove(i);
        }
        tulis(c, isi);
    }

    private static void tulis(Context c, List<ParseFeed.Berita> isi) {
        JSONArray arr = new JSONArray();
        try {
            for (ParseFeed.Berita b : isi) {
                JSONObject o = new JSONObject();
                o.put("judul", b.judul == null ? "" : b.judul);
                o.put("tautan", b.tautan == null ? "" : b.tautan);
                o.put("waktu", b.waktuMentah == null ? "" : b.waktuMentah);
                o.put("label", b.label == null ? "" : b.label);
                o.put("gambar", b.gambar == null ? "" : b.gambar);
                o.put("cuplikan", b.cuplikan == null ? "" : b.cuplikan);
                o.put("isi", b.isi == null ? "" : b.isi);
                o.put("kredit", b.kredit == null ? "" : b.kredit);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        sp(c).edit().putString(KUNCI, arr.toString()).apply();
    }
}
