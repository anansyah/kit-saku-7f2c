package id.eraai.news;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;

/**
 * Warna aplikasi disamakan dengan tema blog "Bintang Sains" (tema v18).
 * Nilai diambil langsung dari variabel CSS tema:
 *
 *   gelap  : --bg #0b1220  --bg-2 #0e1729  --surface #131e34  --text #e8edf6
 *            --text-2 #b9c4d8  --muted #8d9cb5  --rule #24334f  --gold #f2b544
 *            --accent #f2b544  --on-gold #1b1300
 *   terang : --bg #f4f6fa  --bg-2 #e9edf4  --surface #ffffff  --text #0e182b
 *            --text-2 #33425c  --muted #56667f  --rule #d3dbe8  --gold #e9a62a
 *            --accent #8a5200  --on-gold #1b1300
 *
 * PENTING: tema blog memakai DUA emas berbeda.
 *   --gold   dipakai untuk LATAR/tombol terisi  (terang: #e9a62a)
 *   --accent dipakai untuk TEKS/tautan          (terang: #8a5200)
 * Di mode terang, emas #e9a62a sebagai teks hanya punya kontras 1,95:1
 * (nyaris tak terbaca). Karena itu ada aksen() dan aksenTeks().
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

    static int bg(boolean g)        { return Color.parseColor(g ? "#0b1220" : "#f4f6fa"); }
    static int bg2(boolean g)       { return Color.parseColor(g ? "#0e1729" : "#e9edf4"); }
    static int permukaan(boolean g) { return Color.parseColor(g ? "#131e34" : "#ffffff"); }
    static int teks(boolean g)      { return Color.parseColor(g ? "#e8edf6" : "#0e182b"); }
    static int teks2(boolean g)     { return Color.parseColor(g ? "#b9c4d8" : "#33425c"); }
    static int pudar(boolean g)     { return Color.parseColor(g ? "#8d9cb5" : "#56667f"); }
    static int garis(boolean g)     { return Color.parseColor(g ? "#24334f" : "#d3dbe8"); }

    /** Aksen untuk LATAR / tombol terisi (--gold). */
    static int aksen(boolean g)     { return Color.parseColor(g ? "#f2b544" : "#e9a62a"); }

    /** Aksen untuk TEKS / tautan (--accent). Di terang wajib lebih gelap. */
    static int aksenTeks(boolean g) { return Color.parseColor(g ? "#f2b544" : "#8a5200"); }

    /** Warna huruf di atas latar aksen (--on-gold). */
    static int diAtasAksen()        { return Color.parseColor("#1b1300"); }

    /** Latar pil label (aksen tembus pandang) — ikut berubah saat tema berganti. */
    static android.graphics.drawable.GradientDrawable pil(boolean g) {
        int a = aksen(g);
        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        d.setColor(Color.argb(34, Color.red(a), Color.green(a), Color.blue(a)));
        d.setCornerRadius(6);
        return d;
    }

    /** Bentuk kotak membulat untuk dipakai ulang (mengganti drawable XML yang
     *  warnanya statis, sehingga ikut berubah saat tema berganti). */
    static android.graphics.drawable.GradientDrawable kotak(int isi, int garis, float radius) {
        android.graphics.drawable.GradientDrawable g =
                new android.graphics.drawable.GradientDrawable();
        g.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        g.setColor(isi);
        g.setCornerRadius(radius);
        g.setStroke(1, garis);
        return g;
    }

    // ---------- nama untuk dipakai di halaman baca (HTML) ----------

    static String htmlBg(boolean g)        { return g ? "#0b1220" : "#f4f6fa"; }
    static String htmlTeks(boolean g)      { return g ? "#e8edf6" : "#0e182b"; }
    static String htmlTeks2(boolean g)     { return g ? "#b9c4d8" : "#33425c"; }
    static String htmlPudar(boolean g)     { return g ? "#8d9cb5" : "#56667f"; }
    static String htmlAksenTeks(boolean g) { return g ? "#f2b544" : "#8a5200"; }
    static String htmlGaris(boolean g)     { return g ? "#24334f" : "#d3dbe8"; }
}
