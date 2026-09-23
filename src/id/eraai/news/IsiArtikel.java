package id.eraai.news;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mengambil badan artikel dari halaman blog lalu menyusun tampilan baca bersih. */
final class IsiArtikel {

    private IsiArtikel() {}

    private static final Pattern BUKA_BADAN =
            Pattern.compile("<div[^>]*class=['\"]post-body['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIV =
            Pattern.compile("<(/?)div\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMG =
            Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern KREDIT =
            Pattern.compile("data-kredit=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern JUDUL =
            Pattern.compile("<h1[^>]*class=['\"][^'\"]*post-title[^'\"]*['\"][^>]*>(.*?)</h1>",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern WAKTU =
            Pattern.compile("<time[^>]*datetime=['\"]([^'\"]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern KICKER =
            Pattern.compile("<[^>]*class=['\"][^'\"]*kicker[^'\"]*['\"][^>]*>([^<]{2,30})<",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern WRAP_GAMBAR =
            Pattern.compile("<div[^>]*class=['\"]gambar-berita['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIV_KREDIT =
            Pattern.compile("<div[^>]*class=['\"]kredit-gambar['\"][^>]*>.*?</div>",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG_IMG =
            Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_KOSONG =
            Pattern.compile("<p>\\s*(?:<br\\s*/?>)?\\s*</p>", Pattern.CASE_INSENSITIVE);

    /** Badan artikel (HTML) dengan penyeimbang tag div. */
    static String ambil(String halaman) {
        if (halaman == null) return null;
        Matcher m = BUKA_BADAN.matcher(halaman);
        if (!m.find()) return null;
        int awal = m.end();
        int kedalaman = 1;
        Matcher d = DIV.matcher(halaman.substring(awal));
        while (d.find()) {
            if ("/".equals(d.group(1))) {
                kedalaman--;
                if (kedalaman == 0) {
                    return halaman.substring(awal, awal + d.start());
                }
            } else {
                kedalaman++;
            }
        }
        int batas = Math.min(halaman.length(), awal + 60000);
        return halaman.substring(awal, batas);
    }

    /**
     * Buang gambar utama dari badan artikel karena aplikasi sudah menampilkan
     * gambar itu di bagian atas. Tanpa ini gambar tampil DUA KALI.
     * Urutan kerja:
     *   1) buang blok "gambar-berita" yang berisi gambar
     *   2) buang gambar pertama yang masih tersisa (sebagian artikel
     *      menaruh gambarnya di dalam paragraf biasa)
     *   3) buang sisa wadah/baris kredit yang jadi kosong
     */
    static String bersihkan(String badan, boolean gambarDiAtas) {
        if (badan == null) return null;
        if (!gambarDiAtas) return badan;
        String hasil = badan;

        // 1) blok gambar-berita
        Matcher w = WRAP_GAMBAR.matcher(hasil);
        StringBuffer sb = new StringBuffer();
        while (w.find()) {
            int akhir = cariPenutupDiv(hasil, w.end());
            if (akhir < 0) continue;
            if (TAG_IMG.matcher(hasil.substring(w.end(), akhir)).find()) {
                w.appendReplacement(sb, Matcher.quoteReplacement(""));
            }
        }
        w.appendTail(sb);
        hasil = sb.toString();

        // 2) gambar pertama yang masih tersisa
        Matcher g = TAG_IMG.matcher(hasil);
        if (g.find()) {
            hasil = hasil.substring(0, g.start()) + hasil.substring(g.end());
        }

        // 3) sisa wadah kosong
        hasil = DIV_KREDIT.matcher(hasil).replaceAll("");
        hasil = P_KOSONG.matcher(hasil).replaceAll("");
        return hasil;
    }

    /** Posisi penutup div yang seimbang, mulai dari dalam sebuah div. */
    private static int cariPenutupDiv(String s, int mulai) {
        int kedalaman = 1;
        Matcher d = DIV.matcher(s.substring(mulai));
        while (d.find()) {
            if ("/".equals(d.group(1))) {
                kedalaman--;
                if (kedalaman == 0) return mulai + d.start();
            } else {
                kedalaman++;
            }
        }
        return -1;
    }

    static String gambar(String halaman) {
        String badan = ambil(halaman);
        if (badan == null) return null;
        Matcher m = IMG.matcher(badan);
        return m.find() ? m.group(1).replace("&amp;", "&") : null;
    }

    static String kredit(String halaman) {
        String badan = ambil(halaman);
        if (badan == null) return null;
        Matcher m = KREDIT.matcher(badan);
        if (!m.find()) return null;
        return rapikan(m.group(1));
    }

    static String judul(String halaman) {
        Matcher m = JUDUL.matcher(halaman);
        return m.find() ? rapikan(m.group(1)) : null;
    }

    static String waktu(String halaman) {
        Matcher m = WAKTU.matcher(halaman);
        return m.find() ? m.group(1) : null;
    }

    static String label(String halaman) {
        Matcher m = KICKER.matcher(halaman);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String t = rapikan(m.group(1));
            if (t.length() == 0) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(t);
            if (sb.length() > 60) break;
        }
        return sb.toString();
    }

    private static String rapikan(String t) {
        if (t == null) return "";
        return t.replace("&#8212;", "—").replace("&#8211;", "–")
                .replace("&#8217;", "'").replace("&#8216;", "'")
                .replace("&#8220;", "\"").replace("&#8221;", "\"")
                .replace("&amp;", "&").replace("&nbsp;", " ")
                .replace("&#39;", "'").replace("&quot;", "\"")
                .replaceAll("\\s+", " ").trim();
    }

    /** Susun halaman baca: label, judul, gambar + kredit, isi, sumber. */
    static String bungkus(String judul, String badan, String gambar, String kredit,
                          int ukuran, String tautan, String label) {
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">");
        h.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
        h.append("<style>");
        h.append("body{margin:0;padding:16px 18px 40px;background:#fff;color:#1a1a1a;");
        h.append("font-family:-apple-system,'Segoe UI',Roboto,sans-serif;line-height:1.72;");
        h.append("font-size:").append(ukuran).append("px;-webkit-text-size-adjust:100%}");
        h.append(".label{color:#C62828;font-size:.72em;font-weight:700;");
        h.append("letter-spacing:.08em;text-transform:uppercase;margin-bottom:6px}");
        h.append("h1{font-size:1.5em;line-height:1.25;margin:0 0 10px;font-weight:800}");
        h.append(".meta{color:#8a8a8a;font-size:.78em;margin-bottom:18px}");
        h.append(".gambar img{width:100%;height:auto;border-radius:10px;display:block}");
        h.append(".kredit{color:#8a8a8a;font-size:.72em;margin:6px 0 20px}");
        h.append("p{margin:0 0 1.15em}");
        h.append("img{max-width:100%;height:auto;border-radius:10px}");
        h.append("h2,h3{font-size:1.15em;margin:1.4em 0 .5em}");
        h.append("blockquote{border-left:3px solid #C62828;margin:1em 0;padding:0 0 0 14px;color:#444}");
        h.append("a{color:#C62828}");
        h.append(".kredit-gambar{display:none}");
        h.append(".sumber{margin-top:26px;padding-top:14px;border-top:1px solid #eee;");
        h.append("color:#8a8a8a;font-size:.75em}");
        h.append("</style></head><body>");

        if (label != null && label.length() > 0) {
            h.append("<div class=\"label\">").append(aman(label)).append("</div>");
        }
        if (judul != null && judul.length() > 0) {
            h.append("<h1>").append(aman(judul)).append("</h1>");
        }
        h.append("<div class=\"meta\">Eraai Daily News</div>");
        if (gambar != null && gambar.length() > 0) {
            h.append("<div class=\"gambar\"><img src=\"").append(gambar).append("\" alt=\"")
             .append(aman(judul)).append("\"></div>");
            if (kredit != null && kredit.length() > 0) {
                h.append("<div class=\"kredit\">").append(aman(kredit)).append("</div>");
            }
        }
        h.append(badan == null ? "" : badan);
        h.append("<div class=\"sumber\">Sumber asli: ")
         .append("<a href=\"").append(tautan).append("\">eraaidailynews.blogspot.com</a></div>");
        h.append("</body></html>");
        return h.toString();
    }

    private static String aman(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
