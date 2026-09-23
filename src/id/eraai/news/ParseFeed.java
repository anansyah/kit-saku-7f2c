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

/** Memecah JSON feed Blogger menjadi daftar berita. */
final class ParseFeed {
    private ParseFeed() {}

    static class Berita {
        String judul;
        String tautan;
        String waktuMentah;
        String label;

        String waktuPendek() {
            if (waktuMentah == null) return "";
            try {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date d = f.parse(waktuMentah.substring(0, 19));
                SimpleDateFormat k = new SimpleDateFormat("d MMM yyyy • HH:mm", new Locale("id"));
                if (d != null) return k.format(d) + " WIB";
            } catch (ParseException | StringIndexOutOfBoundsException ignored) {
            }
            return waktuMentah;
        }
    }

    static List<Berita> ambil(String json) {
        List<Berita> keluar = new ArrayList<>();
        try {
            JSONObject akar = new JSONObject(json);
            JSONArray entries = akar.getJSONObject("feed").optJSONArray("entry");
            if (entries == null) return keluar;
            for (int i = 0; i < entries.length(); i++) {
                JSONObject e = entries.getJSONObject(i);
                Berita b = new Berita();
                JSONObject t = e.optJSONObject("title");
                b.judul = t == null ? "" : t.optString("$t", "");
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
                    for (int j = 0; j < kat.length() && j < 3; j++) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(kat.getJSONObject(j).optString("term", ""));
                    }
                    b.label = sb.toString();
                }
                if (b.judul.length() > 0 && b.tautan != null) keluar.add(b);
            }
        } catch (JSONException ignored) {
        }
        return keluar;
    }
}
