package id.eraai.news;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Pembaca berita: mode baca bersih, ukuran huruf, simpan offline, bagikan. */
public class ReaderActivity extends Activity {

    private static final String NAMA_ALAMAT = "eraaidailynews.blogspot.com";

    private WebView web;
    private ProgressBar muat;
    private TextView tombolSimpan;
    private String tautan;
    private String judul;
    private String halaman;
    private String badanArtikel;
    private String gambarArtikel;
    private String kreditArtikel;
    private String labelArtikel;
    private int ukuran = 17;
    private boolean modeBaca = true;
    private boolean gelap = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        tautan = getIntent().getStringExtra("tautan");
        judul = getIntent().getStringExtra("judul");
        if (tautan == null) { finish(); return; }

        gelap = Tampilan.gelap(this);

        // lapisi warna seluruh layar baca (layout XML selalu gelap)
        findViewById(R.id.akar).setBackgroundColor(Tampilan.bg(gelap));
        findViewById(R.id.kepala_reader).setBackgroundColor(Tampilan.bg2(gelap));
        findViewById(R.id.kaki_reader).setBackgroundColor(Tampilan.bg2(gelap));
        ((TextView) findViewById(R.id.tombol_ukuran)).setTextColor(Tampilan.teks2(gelap));
        ((TextView) findViewById(R.id.tombol_bagikan)).setTextColor(Tampilan.teks2(gelap));
        ((TextView) findViewById(R.id.tombol_browser)).setTextColor(Tampilan.teks2(gelap));
        getWindow().setStatusBarColor(Tampilan.bg(gelap));
        getWindow().setNavigationBarColor(Tampilan.bg(gelap));

        TextView labelSumber = findViewById(R.id.label_sumber);
        labelSumber.setText(judul == null ? getString(R.string.sumber) : judul);
        labelSumber.setTextColor(Tampilan.teks2(gelap));

        muat = findViewById(R.id.muat);
        ((ProgressBar) muat).getIndeterminateDrawable().setTint(Tampilan.aksen(gelap));
        ((ProgressBar) muat).getProgressDrawable().setColorFilter(
                Tampilan.aksen(gelap), android.graphics.PorterDuff.Mode.SRC_IN);
        web = findViewById(R.id.web);
        setelanWeb(web);
        tombolSimpan = findViewById(R.id.tombol_simpan);
        tombolSimpan.setTextColor(Tampilan.aksenTeks(gelap));
        segarkanTombolSimpan();

        TextView tKembali = findViewById(R.id.tombol_kembali);
        tKembali.setTextColor(Tampilan.aksenTeks(gelap));
        tKembali.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!modeBaca && web.canGoBack()) web.goBack(); else finish();
            }
        });

        findViewById(R.id.tombol_browser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bukaLuar(tautan);
            }
        });

        findViewById(R.id.tombol_bagikan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent kirim = new Intent(Intent.ACTION_SEND);
                kirim.setType("text/plain");
                kirim.putExtra(Intent.EXTRA_TEXT,
                        (judul == null ? "" : judul + "\n") + tautan + "\n\n— EraAIdailyNews");
                startActivity(Intent.createChooser(kirim, getString(R.string.berbagi)));
            }
        });

        tombolSimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Simpan.ada(ReaderActivity.this, tautan)) {
                    Simpan.hapus(ReaderActivity.this, tautan);
                    Toast.makeText(ReaderActivity.this, R.string.terhapus, Toast.LENGTH_SHORT).show();
                } else {
                    // simpan HANYA badan artikel (kecil), bukan seluruh halaman
                    ParseFeed.Berita b = new ParseFeed.Berita();
                    b.judul = judul;
                    b.tautan = tautan;
                    b.isi = badanArtikel;
                    b.gambar = gambarArtikel;
                    b.kredit = kreditArtikel;
                    b.label = labelArtikel;
                    Simpan.tambah(ReaderActivity.this, b);
                    Toast.makeText(ReaderActivity.this, R.string.tersimpan_ok, Toast.LENGTH_SHORT).show();
                }
                segarkanTombolSimpan();
            }
        });

        findViewById(R.id.tombol_ukuran).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ukuran = ukuran >= 24 ? 15 : ukuran + 3;
                Toast.makeText(ReaderActivity.this, "Ukuran huruf " + ukuran, Toast.LENGTH_SHORT).show();
                if (halaman != null) tampilkanBaca(halaman);
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView w, int maju) {
                muat.setVisibility(maju < 100 ? View.VISIBLE : View.GONE);
                muat.setProgress(maju);
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView w, String url) {
                if (url.contains(NAMA_ALAMAT)) {
                    if (modeBaca) {
                        modeWeb(url);
                        return true;
                    }
                    return false;
                }
                bukaLuar(url);
                return true;
            }
        });

        // 1) coba dari simpanan (bisa dibaca tanpa internet)
        List<ParseFeed.Berita> tersimpan = Simpan.semua(this);
        for (ParseFeed.Berita b : tersimpan) {
            if (b.tautan != null && b.tautan.equals(tautan) && b.isi != null && b.isi.length() > 200) {
                if (judul == null || judul.length() == 0) judul = b.judul;
                badanArtikel = b.isi;
                gambarArtikel = b.gambar;
                kreditArtikel = b.kredit;
                labelArtikel = b.label;
                if (gambarArtikel == null || gambarArtikel.length() == 0) {
                    gambarArtikel = IsiArtikel.gambar(b.isi);
                }
                if (kreditArtikel == null || kreditArtikel.length() == 0) {
                    kreditArtikel = IsiArtikel.kredit(b.isi);
                }
                String html = IsiArtikel.bungkus(judul, badanArtikel, gambarArtikel,
                        kreditArtikel, ukuran, tautan, labelArtikel, gelap);
                web.loadDataWithBaseURL(tautan, html, "text/html", "UTF-8", null);
                return;
            }
        }

        // 2) unduh dari blog
        muat.setVisibility(View.VISIBLE);
        Pengambil.unduh(tautan, new Pengambil.Dengar() {
            @Override
            public void selesai(final String data, final String galat) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        muat.setVisibility(View.GONE);
                        if (data == null) {
                            modeBaca = false;
                            web.loadUrl(tautan);
                            return;
                        }
                        halaman = data;
                        String j = IsiArtikel.judul(data);
                        if (j != null && j.length() > 0) judul = j;
                        tampilkanBaca(data);
                    }
                });
            }
        });
    }

    private void segarkanTombolSimpan() {
        tombolSimpan.setText(Simpan.ada(this, tautan) ? R.string.disimpan : R.string.simpan);
    }

    /** Ambil badan artikel lalu susun tampilan baca yang bersih. */
    private void tampilkanBaca(String isi) {
        badanArtikel = IsiArtikel.ambil(isi);
        gambarArtikel = IsiArtikel.gambar(isi);
        kreditArtikel = IsiArtikel.kredit(isi);
        labelArtikel = IsiArtikel.label(isi);
        // gambar ditampilkan terpisah di atas -> buang dari badan agar tidak dobel
        String badan = IsiArtikel.bersihkan(badanArtikel, gambarArtikel != null);
        String html = IsiArtikel.bungkus(judul, badan, gambarArtikel,
                kreditArtikel, ukuran, tautan, labelArtikel, gelap);
        web.loadDataWithBaseURL(tautan, html, "text/html", "UTF-8", null);
    }

    private void bukaLuar(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Tidak ada aplikasi browser", Toast.LENGTH_SHORT).show();
        }
    }

    /** Beralih ke tampilan web penuh (JavaScript hidup) untuk tautan internal. */
    private void modeWeb(String url) {
        modeBaca = false;
        web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.loadUrl(url);
    }

    private void setelanWeb(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(false);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setDefaultTextEncodingName("UTF-8");
        w.setBackgroundColor(Tampilan.bg(gelap));
    }

    @Override
    public void onBackPressed() {
        if (!modeBaca && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
