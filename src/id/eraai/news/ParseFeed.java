package id.eraai.news;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Memecah JSON feed Blogger menjadi daftar berita. */
final class ParseFeed {
    private ParseFeed() {}

    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern IMG = Pattern.compile("<img[^>]+src=\"([^\"]+)\"");
    private static final Pattern KREDIT = Pattern.compile("data-kredit=\"([^\"]*)\"");

    static class Berita {
        String judul;
        String tautan;
        String waktuMentah;
        String label;
        String gambar;
        String kredit;
        String cuplikan;
        String isi;

        String waktuPendek() {
            if (waktuMentah == null || waktuMentah.length() < 19) return "";
            try {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date d = f.parse(waktuMentah.substring(0, 19));
                SimpleDateFormat k = new SimpleDateFormat("d MMM yyyy • HH:mm", new Locale("id"));
                if (d != null) return k.format(d) + " WIB";
            } catch (ParseException ignored) {
            }
            return "";
        }

        String labelUtama() {
            if (label == null || label.length() == 0) return "";
            int koma = label.indexOf(',');
            return koma > 0 ? label.substring(0, koma).trim() : label.trim();
        }
    }

    private static String bersih(String html) {
        if (html == null) return "";
        String t = TAG.matcher(html).replaceAll(" ");
        t = t.replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"")
             .replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
             .replace("&hellip;", "…").replace("&mdash;", "—").replace("&ndash;", "–")
             .replace("&rsquo;", "'").replace("&lsquo;", "'")
             .replace("&ldquo;", "\"").replace("&rdquo;", "\"");
        return t.replaceAll("\\s+", " ").trim();
    }

    static List<Berita> ambil(String json) {
        List<Berita> keluar = new ArrayList<>();
        try {
            JSONObject akar = new JSONObject(json);
            JSONObject feed = akar.optJSONObject("feed");
            if (feed == null) return keluar;
            JSONArray entries = feed.optJSONArray("entry");
            if (entries == null) return keluar;
            for (int i = 0; i < entries.length(); i++) {
                JSONObject e = entries.getJSONObject(i);
                Berita b = new Berita();
                JSONObject t = e.optJSONObject("title");
                b.judul = t == null ? "" : t.optString("$t", "");
                b.judul = b.judul.replace("&amp;", "&").replace("&#39;", "'");

                JSONArray links = e.optJSONArray("link");
                if (links != null) {
                    for (int j = 0; j < links.length(); j++) {
                        JSONObject l = links.getJSONObject(j);
                        if ("alternate".equals(l.optString("rel"))) {
                            b.tautan = l.optString("href", "");
                            break;
                        }
                    }
                }
                JSONObject p = e.optJSONObject("published");
                b.waktuMentah = p == null ? "" : p.optString("$t", "");

                JSONArray kat = e.optJSONArray("category");
                if (kat != null && kat.length() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < kat.length() && j < 4; j++) {
                        String term = kat.getJSONObject(j).optString("term", "");
                        if (term.length() == 0) continue;
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(term);
                    }
                    b.label = sb.toString();
                }

                JSONObject isi = e.optJSONObject("content");
                if (isi == null) isi = e.optJSONObject("summary");
                String html = isi == null ? "" : isi.optString("$t", "");
                Matcher mi = IMG.matcher(html);
                if (mi.find()) b.gambar = mi.group(1).replace("&amp;", "&");
                Matcher mk = KREDIT.matcher(html);
                if (mk.find()) b.kredit = mk.group(1);
                b.cuplikan = bersih(html);
                // buang embel-embel kredit gambar dari awal cuplikan
                b.cuplikan = b.cuplikan.replaceAll(
                        "^(?:Gambar|Foto|Image|Photo)\\s*:[^.]*?\\.(?:jpg|jpeg|png|webp|gif|svg)\\s*", "");
                b.cuplikan = b.cuplikan.replaceAll(
                        "^(?:Gambar|Foto|Image|Photo)\\s*:[^—\\-]*[—\\-]\\s*", "");
                b.cuplikan = b.cuplikan.trim();
                if (b.cuplikan.length() > 200) {
                    b.cuplikan = b.cuplikan.substring(0, 200).trim() + "…";
                }
                b.isi = html;

                if (b.judul.length() > 0 && b.tautan != null && b.tautan.length() > 0) {
                    keluar.add(b);
                }
            }
        } catch (JSONException ignored) {
        }
        return keluar;
    }

    static String total(String json) {
        try {
            JSONObject feed = new JSONObject(json).optJSONObject("feed");
            if (feed == null) return "";
            JSONObject t = feed.optJSONObject("openSearch$totalResults");
            return t == null ? "" : t.optString("$t", "");
        } catch (JSONException e) {
            return "";
        }
    }
}
