package id.eraai.news;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PER_HALAMAN = 20;

    // daftar label nyata dari blog (dihitung dari 151 artikel terbit)
    private static final String[] LABEL_UTAMA = {
            "Semua", "Sains", "Teknologi", "Ekonomi", "Kesehatan",
            "Fisika", "Internasional", "Olahraga", "Bola", "Politik",
            "Lokal", "Astronomi", "Hiburan", "AI"
    };

    private ListView daftar;
    private LinearLayout panelMuat, lebih, barisLabel;
    private TextView status, teksMuat, teksLebih, tombolTema, tombolTersimpan;
    private ProgressBar pemutar, pemutarKecil;
    private EditText cari;
    private Adapter adapter;
    private final List<ParseFeed.Berita> isi = new ArrayList<>();

    private String labelDipilih = "Semua";
    private String kataKunci = "";
    private int mulai = 1;
    private int total = -1;          // jumlah artikel di blog (dari feed)
    private boolean sedangAmbil = false;
    private boolean modeTersimpan = false;
    private boolean gelap = true;
    private boolean habis = false;   // semua berita sudah termuat

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() { return isi.size(); }

        @Override
        public Object getItem(int pos) { return isi.get(pos); }

        @Override
        public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View lama, ViewGroup induk) {
            View v = lama;
            if (v == null) {
                v = getLayoutInflater().inflate(R.layout.baris_berita, induk, false);
            }
            ParseFeed.Berita b = isi.get(pos);
            TextView judul = v.findViewById(R.id.judul);
            TextView waktu = v.findViewById(R.id.waktu);
            TextView label = v.findViewById(R.id.label);
            TextView cuplikan = v.findViewById(R.id.cuplikan);
            TextView simpan = v.findViewById(R.id.simpan);
            ImageView gambar = v.findViewById(R.id.gambar);

            judul.setTextColor(Tampilan.teks(gelap));
            cuplikan.setTextColor(Tampilan.teks2(gelap));
            waktu.setTextColor(Tampilan.pudar(gelap));
            simpan.setTextColor(Tampilan.aksen(gelap));
            gambar.setBackgroundColor(Tampilan.permukaan(gelap));

            judul.setText(b.judul);
            waktu.setText(b.waktuPendek());
            cuplikan.setText(b.cuplikan == null ? "" : b.cuplikan);
            String utama = b.labelUtama();
            if (utama.length() > 0) {
                label.setVisibility(View.VISIBLE);
                label.setText(utama.toUpperCase());
            } else {
                label.setVisibility(View.GONE);
            }

            final String tautan = b.tautan;
            simpan.setText(Simpan.ada(MainActivity.this, tautan)
                    ? R.string.disimpan : R.string.simpan);
            simpan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ParseFeed.Berita x = cariBerita(tautan);
                    if (x == null) return;
                    if (Simpan.ada(MainActivity.this, tautan)) {
                        Simpan.hapus(MainActivity.this, tautan);
                        Toast.makeText(MainActivity.this,
                                R.string.terhapus, Toast.LENGTH_SHORT).show();
                    } else {
                        Simpan.tambah(MainActivity.this, x);
                        Toast.makeText(MainActivity.this,
                                R.string.tersimpan_ok, Toast.LENGTH_SHORT).show();
                    }
                    if (modeTersimpan) muatTersimpan();
                    else adapter.notifyDataSetChanged();
                }
            });
            Gambar.muat(b.gambar, gambar);
            return v;
        }

        private ParseFeed.Berita cariBerita(String tautan) {
            for (ParseFeed.Berita x : isi) {
                if (x.tautan != null && x.tautan.equals(tautan)) return x;
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        daftar = findViewById(R.id.daftar);
        status = findViewById(R.id.status);
        panelMuat = findViewById(R.id.panel_muat);
        teksMuat = findViewById(R.id.teks_muat);
        pemutar = findViewById(R.id.pemutar);
        lebih = findViewById(R.id.lebih);
        teksLebih = findViewById(R.id.teks_lebih);
        pemutarKecil = findViewById(R.id.pemutar_kecil);
        barisLabel = findViewById(R.id.baris_label);
        cari = findViewById(R.id.cari);
        tombolTema = findViewById(R.id.tombol_tema);
        tombolTersimpan = findViewById(R.id.tombol_tersimpan);
        TextView segar = findViewById(R.id.tombol_segar);

        gelap = Tampilan.gelap(this);
        tombolTema.setText("Tema: " + Tampilan.namaMode(this));

        adapter = new Adapter();
        daftar.setAdapter(adapter);
        daftar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> induk, View v, int pos, long id) {
                ParseFeed.Berita b = isi.get(pos);
                Intent it = new Intent(MainActivity.this, ReaderActivity.class);
                it.putExtra("tautan", b.tautan);
                it.putExtra("judul", b.judul);
                startActivity(it);
            }
        });

        // ==== TOMBOL "MUAT BERITA LAIN" — kini benar-benar berfungsi ====
        lebih.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (habis) return;
                ambilLagi();
            }
        });

        segar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modeTersimpan = false;
                mulai = 1;
                total = -1;
                kataKunci = "";
                cari.setText("");
                labelDipilih = "Semua";
                isi.clear();
                adapter.notifyDataSetChanged();
                pasangLabel();
                ambil();
            }
        });

        tombolTersimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modeTersimpan = true;
                lebih.setVisibility(View.GONE);
                muatTersimpan();
            }
        });

        tombolTema.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tampilan.setMode(MainActivity.this, Tampilan.modeBerikutnya(MainActivity.this));
                recreate();
            }
        });

        cari.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int aksi, android.view.KeyEvent e) {
                if (aksi == EditorInfo.IME_ACTION_SEARCH) {
                    modeTersimpan = false;
                    kataKunci = cari.getText().toString().trim();
                    mulai = 1;
                    total = -1;
                    isi.clear();
                    adapter.notifyDataSetChanged();
                    pasangLabel();
                    ambil();
                    return true;
                }
                return false;
            }
        });

        pasangLabel();
        ambil();
    }

    private void muatTersimpan() {
        isi.clear();
        isi.addAll(Simpan.semua(this));
        adapter.notifyDataSetChanged();
        status.setVisibility(isi.isEmpty() ? View.VISIBLE : View.GONE);
        status.setText(isi.isEmpty() ? getString(R.string.belum_ada_simpanan) : "");
        Toast.makeText(this, "Tersimpan: " + isi.size() + " berita", Toast.LENGTH_SHORT).show();
    }

    private void pasangLabel() {
        barisLabel.removeAllViews();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 8, 0);
        for (final String nama : LABEL_UTAMA) {
            TextView t = new TextView(this);
            t.setText(nama);
            t.setTextSize(12);
            t.setPadding(18, 7, 18, 7);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            boolean pilih = nama.equals(labelDipilih);
            GradientDrawable latar = new GradientDrawable();
            latar.setCornerRadius(30);
            latar.setColor(pilih ? Tampilan.aksen(gelap) : Tampilan.permukaan(gelap));
            latar.setStroke(1, Tampilan.garis(gelap));
            t.setBackground(latar);
            t.setTextColor(pilih ? Tampilan.diAtasAksen() : Tampilan.teks2(gelap));
            t.setLayoutParams(lp);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    modeTersimpan = false;
                    labelDipilih = nama;
                    mulai = 1;
                    total = -1;
                    isi.clear();
                    adapter.notifyDataSetChanged();
                    pasangLabel();
                    ambil();
                }
            });
            barisLabel.addView(t);
        }
    }

    private String alamat() {
        // bentuk sah: .../feeds/posts/default[-/Label]?alt=json&max-results=20[&q=..][&start-index=..]
        StringBuilder u = new StringBuilder(
                "https://eraaidailynews.blogspot.com/feeds/posts/default");
        if (!"Semua".equals(labelDipilih)) {
            u.append("/-/").append(labelDipilih.replace(" ", "%20"));
        }
        u.append("?alt=json&max-results=").append(PER_HALAMAN);
        if (kataKunci.length() > 0) {
            try {
                u.append("&q=").append(java.net.URLEncoder.encode(kataKunci, "UTF-8"));
            } catch (Exception ignored) {
            }
        }
        u.append("&start-index=").append(mulai);
        return u.toString();
    }

    /** Ambil halaman berikutnya — dipakai tombol "Muat berita lain". */
    private void ambilLagi() {
        if (sedangAmbil) return;
        tampilkanMuatLagi(true);
        ambil();
    }

    private void tampilkanMuatLagi(boolean sedang) {
        if (sedang) {
            lebih.setVisibility(View.VISIBLE);
            pemutarKecil.setVisibility(View.VISIBLE);
            teksLebih.setText(R.string.sedang_memuat_lagi);
        } else {
            pemutarKecil.setVisibility(View.GONE);
            teksLebih.setText(R.string.muat_lagi);
        }
    }

    private void ambil() {
        if (sedangAmbil) return;
        sedangAmbil = true;
        if (mulai == 1) {
            // mulai dari awal -> bukan "sudah habis" lagi
            habis = false;
            // animasi lingkaran berputar saat memuat pertama / ganti kategori / cari
            lebih.setVisibility(View.GONE);
            status.setVisibility(View.GONE);
            teksMuat.setText(R.string.memuat);
            pemutar.setVisibility(View.VISIBLE);
            panelMuat.setVisibility(View.VISIBLE);
        }
        final String alamat = alamat();
        Pengambil.unduh(alamat, new Pengambil.Dengar() {
            @Override
            public void selesai(final String data, final String galat) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sedangAmbil = false;
                        panelMuat.setVisibility(View.GONE);
                        if (galat != null || data == null) {
                            tampilkanMuatLagi(false);
                            if (isi.isEmpty()) {
                                status.setVisibility(View.VISIBLE);
                                status.setText(R.string.gagal);
                            } else {
                                Toast.makeText(MainActivity.this,
                                        R.string.gagal, Toast.LENGTH_SHORT).show();
                                lebih.setVisibility(View.VISIBLE);
                            }
                            return;
                        }
                        String t = ParseFeed.total(data);
                        if (t != null && t.length() > 0) {
                            try { total = Integer.parseInt(t); } catch (Exception ignored) {}
                        }
                        List<ParseFeed.Berita> baru = ParseFeed.ambil(data);
                        if (mulai == 1 && baru.isEmpty()) {
                            status.setVisibility(View.VISIBLE);
                            status.setText(kataKunci.length() > 0
                                    ? R.string.tidak_ada_hasil : R.string.kosong);
                            lebih.setVisibility(View.GONE);
                            return;
                        }
                        // buang yang sudah ada supaya tidak ada berita kembar
                        int ditambah = 0;
                        for (ParseFeed.Berita b : baru) {
                            if (!sudahAda(b.tautan)) {
                                isi.add(b);
                                ditambah++;
                            }
                        }
                        adapter.notifyDataSetChanged();
                        mulai += PER_HALAMAN;

                        // masih ada halaman berikutnya?
                        boolean masihAda = baru.size() >= PER_HALAMAN
                                && (total < 0 || isi.size() < total);
                        habis = !masihAda;
                        tampilkanMuatLagi(false);
                        lebih.setVisibility(View.VISIBLE);
                        if (habis) {
                            pemutarKecil.setVisibility(View.GONE);
                            teksLebih.setText(R.string.sudah_habis);
                        }
                    }
                });
            }
        });
    }

    private boolean sudahAda(String tautan) {
        if (tautan == null) return true;
        for (ParseFeed.Berita x : isi) {
            if (tautan.equals(x.tautan)) return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (modeTersimpan || kataKunci.length() > 0 || !"Semua".equals(labelDipilih)) {
            modeTersimpan = false;
            kataKunci = "";
            cari.setText("");
            labelDipilih = "Semua";
            mulai = 1;
            total = -1;
            isi.clear();
            adapter.notifyDataSetChanged();
            pasangLabel();
            ambil();
        } else {
            super.onBackPressed();
        }
    }
}
