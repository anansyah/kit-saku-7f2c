package id.eraai.news;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;

/**
 * Warna aplikasi disamakan dengan tema blog "Bintang Sains":
 *   gelap  -> latar #0B1220, permukaan #131E34, teks #E8EDF6, aksen emas #F2B544
 *   terang -> latar #F4F6FA, permukaan #FFFFFF, teks #0E182B, aksen emas #E9A62A
 */
final class Tampilan {

    static final int OTOMATIS = 0;
    static final int GELAP = 1;
    static final int TERANG = 2;

    private static final String BERKAS = "era-tampilan";
    private static final String KUNCI = "mode";

    private Tampilan() {}

    static int mode(Context c) {
        return c.getSharedPreferences(BERKAS, Context.MODE_PRIVATE).getInt(KUNCI, OTOMATIS);
    }

    static void setMode(Context c, int m) {
        SharedPreferences p = c.getSharedPreferences(BERKAS, Context.MODE_PRIVATE);
        p.edit().putInt(KUNCI, m).apply();
    }

    /** Nama mode untuk ditampilkan pada tombol. */
    static String namaMode(Context c) {
        int m = mode(c);
        if (m == GELAP) return "Gelap";
        if (m == TERANG) return "Terang";
        return "Otomatis";
    }

    /** Mode berikutnya saat tombol tema diketuk: otomatis -> gelap -> terang. */
    static int modeBerikutnya(Context c) {
        int m = mode(c);
        if (m == OTOMATIS) return GELAP;
        if (m == GELAP) return TERANG;
        return OTOMATIS;
    }

    static boolean gelap(Context c) {
        int m = mode(c);
        if (m == GELAP) return true;
        if (m == TERANG) return false;
        int ui = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return ui == Configuration.UI_MODE_NIGHT_YES;
    }

    // ---------- warna ----------

    static int bg(boolean g)        { return Color.parseColor(g ? "#0B1220" : "#F4F6FA"); }
    static int bg2(boolean g)       { return Color.parseColor(g ? "#0E1729" : "#E9EDF4"); }
    static int permukaan(boolean g) { return Color.parseColor(g ? "#131E34" : "#FFFFFF"); }
    static int teks(boolean g)      { return Color.parseColor(g ? "#E8EDF6" : "#0E182B"); }
    static int teks2(boolean g)     { return Color.parseColor(g ? "#B9C4D8" : "#33425C"); }
    static int pudar(boolean g)     { return Color.parseColor(g ? "#8D9CB5" : "#56667F"); }
    static int garis(boolean g)     { return Color.parseColor(g ? "#24334F" : "#D3DBE8"); }
    static int aksen(boolean g)     { return Color.parseColor(g ? "#F2B544" : "#E9A62A"); }
    static int diAtasAksen()        { return Color.parseColor("#1B1300"); }
}
