package id.eraai.news;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pembaca artikel dengan suara (text-to-speech) memakai mesin suara bawaan
 * Android. Dipilih daripada cara website (speechSynthesis JavaScript) karena:
 *   - jalan tanpa internet (JavaScript di WebView sengaja dimatikan)
 *   - memakai suara bahasa Indonesia yang sudah ada di HP
 *
 * Teks dipecah per beberapa kalimat (maks ~300 huruf) supaya mesin suara
 * tidak terpotong saat membaca artikel panjang.
 */
final class Bicara {

    /** Kabar saat mesin suara siap dipakai (atau ternyata tidak ada). */
    interface Siap {
        void hasil(boolean bisa);
    }

    private TextToSpeech tts;
    private boolean siapMesin = false;
    private boolean gagal = false;
    private boolean jalan = false;
    private int pos = 0;
    private final List<String> bagian = new ArrayList<String>();
    private final Handler ui = new Handler(Looper.getMainLooper());

    Bicara(Context c, final Siap siap) {
        tts = new TextToSpeech(c.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int r = tts.setLanguage(new Locale("id", "ID"));
                    if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // kalau suara Indonesia tidak ada, pakai bahasa HP
                        int r2 = tts.setLanguage(Locale.getDefault());
                        if (r2 == TextToSpeech.LANG_MISSING_DATA
                                || r2 == TextToSpeech.LANG_NOT_SUPPORTED) {
                            gagal = true;
                        }
                    }
                    if (!gagal) {
                        tts.setSpeechRate(0.95f);
                        tts.setPitch(1.0f);
                        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String id) { }

                            @Override
                            public void onDone(String id) {
                                ui.post(new Runnable() {
                                    @Override
                                    public void run() { lanjut(); }
                                });
                            }

                            @Override
                            public void onError(String id) {
                                ui.post(new Runnable() {
                                    @Override
                                    public void run() { lanjut(); }
                                });
                            }
                        });
                    }
                    siapMesin = true;
                } else {
                    gagal = true;
                    siapMesin = true;
                }
                // kalau tombol sudah ditekan sebelum mesin siap, mulai sekarang
                if (jalan && !gagal) ucapkan();
                if (siap != null) siap.hasil(!gagal);
            }
        });
    }

    /** Mesin suara tidak tersedia di HP ini. */
    boolean tidakAda() {
        return gagal;
    }

    /** Apakah sedang membacakan. */
    boolean sedangJalan() {
        return jalan;
    }

    /**
     * Mulai membacakan teks. Mengembalikan false kalau mesin suara belum siap
     * atau tidak ada.
     */
    boolean mulai(String judul, String teks) {
        if (gagal) return false;
        stop();
        bagian.clear();
        if (judul != null && judul.trim().length() > 0) {
            bagian.add(judul.trim() + ".");
        }
        bagian.addAll(pecah(teks));
        if (bagian.isEmpty()) return false;
        pos = 0;
        jalan = true;
        if (siapMesin) ucapkan();
        return true;
    }

    /** Hentikan pembacaan. */
    void stop() {
        jalan = false;
        pos = 0;
        if (tts != null) {
            try { tts.stop(); } catch (Exception ignored) { }
        }
    }

    /** Lepaskan mesin suara (panggil saat layar ditutup). */
    void tutup() {
        stop();
        if (tts != null) {
            try { tts.shutdown(); } catch (Exception ignored) { }
            tts = null;
        }
    }

    private void lanjut() {
        if (!jalan) return;
        pos++;
        ucapkan();
    }

    private void ucapkan() {
        if (!jalan || tts == null) return;
        if (pos >= bagian.size()) {
            jalan = false;
            return;
        }
        String teks = bagian.get(pos);
        tts.speak(teks, TextToSpeech.QUEUE_FLUSH, null, "era-" + pos);
    }

    /**
     * Pecah teks jadi potongan yang aman untuk mesin suara (maks ~300 huruf),
     * dipotong di batas kalimat supaya bacaannya tetap mengalir.
     */
    static List<String> pecah(String teks) {
        List<String> keluar = new ArrayList<String>();
        if (teks == null) return keluar;
        String t = teks.replaceAll("\\s+", " ").trim();
        if (t.length() == 0) return keluar;
        String[] kalimat = t.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kalimat.length; i++) {
            String k = kalimat[i].trim();
            if (k.length() == 0) continue;
            if (sb.length() > 0 && sb.length() + k.length() > 300) {
                keluar.add(sb.toString());
                sb.setLength(0);
            }
            if (sb.length() > 0) sb.append(' ');
            sb.append(k);
        }
        if (sb.length() > 0) keluar.add(sb.toString());
        return keluar;
    }
}
