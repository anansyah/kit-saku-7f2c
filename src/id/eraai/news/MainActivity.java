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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String FEED =
            "https://eraaidailynews.blogspot.com/feeds/posts/default?alt=json&max-results=20";

    private static final String[] LABEL_UTAMA = {
            "Sains", "Teknologi", "Ekonomi", "Kesehatan", "Fisika",
            "Internasional", "Olahraga", "Bola", "Politik", "Lokal"
    };

    private ListView daftar;
    private TextView status, lebih;
    private LinearLayout barisLabel;
    private EditText cari;
    private Adapter adapter;
    private final List<ParseFeed.Berita> isi = new ArrayList<>();
    private String labelDipilih = "Semua";
    private String kataKunci = "";
    private int mulai = 1;
    private boolean sedangAmbil = false;
    private boolean modeTersimpan = false;

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
            android.widget.ImageView gambar = v.findViewById(R.id.gambar);

            judul.setText(b.judul);
            waktu.setText(b.waktuPendek());
            cuplikan.setText(b.cuplikan == null ? "" : b.cuplikan);
            String utama = b.labelUtama();
            if (utama.length() > 0) {
                label.setVisibility(View.VISIBLE);
                label.setText(utama);
            } else {
                label.setVisibility(View.GONE);
            }
            final String tautan = b.tautan;
            final boolean kesimpanan = Simpan.ada(MainActivity.this, tautan);
            simpan.setText(kesimpanan ? R.string.disimpan : R.string.simpan);
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
        lebih = findViewById(R.id.lebih);
        barisLabel = findViewById(R.id.baris_label);
        cari = findViewById(R.id.cari);
        TextView segar = findViewById(R.id.tombol_segar);
        TextView tersimpan = findViewById(R.id.tombol_tersimpan);

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

        segar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modeTersimpan = false;
                mulai = 1;
                kataKunci = "";
                cari.setText("");
                labelDipilih = "Semua";
                isi.clear();
                adapter.notifyDataSetChanged();
                pasangLabel();
                ambil();
            }
        });

        tersimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modeTersimpan = true;
                lebih.setVisibility(View.GONE);
                muatTersimpan();
            }
        });

        cari.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int aksi, android.view.KeyEvent e) {
                if (aksi == EditorInfo.IME_ACTION_SEARCH) {
                    modeTersimpan = false;
                    kataKunci = cari.getText().toString().trim();
                    mulai = 1;
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
        lp.setMargins(0, 0, 14, 0);
        for (final String nama : LABEL_UTAMA) {
            TextView t = new TextView(this);
            t.setText(nama);
            t.setTextSize(13);
            t.setPadding(22, 8, 22, 8);
            t.setGravity(Gravity.CENTER);
            boolean pilih = nama.equals(labelDipilih);
            GradientDrawable latar = new GradientDrawable();
            latar.setCornerRadius(30);
            latar.setColor(pilih ? Color.parseColor("#C62828") : Color.WHITE);
            latar.setStroke(1, Color.parseColor("#DDDDDD"));
            t.setBackground(latar);
            t.setTextColor(pilih ? Color.WHITE : Color.parseColor("#444444"));
            t.setLayoutParams(lp);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    modeTersimpan = false;
                    labelDipilih = nama;
                    mulai = 1;
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
            String label = labelDipilih.replace(" ", "%20");
            u.append("/-/").append(label);
        }
        u.append("?alt=json&max-results=20");
        if (kataKunci.length() > 0) {
            try {
                u.append("&q=").append(java.net.URLEncoder.encode(kataKunci, "UTF-8"));
            } catch (Exception ignored) {
            }
        }
        u.append("&start-index=").append(mulai);
        return u.toString();
    }

    private void ambil() {
        if (sedangAmbil) return;
        sedangAmbil = true;
        if (mulai == 1) {
            status.setVisibility(View.VISIBLE);
            status.setText(R.string.memuat);
            lebih.setVisibility(View.GONE);
        }
        final String alamat = alamat();
        Pengambil.unduh(alamat, new Pengambil.Dengar() {
            @Override
            public void selesai(final String data, final String galat) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sedangAmbil = false;
                        if (galat != null || data == null) {
                            if (isi.isEmpty()) {
                                status.setText(R.string.gagal);
                            } else {
                                status.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this,
                                        R.string.gagal, Toast.LENGTH_SHORT).show();
                            }
                            if (mulai > 1) lebih.setVisibility(View.VISIBLE);
                            return;
                        }
                        List<ParseFeed.Berita> baru = ParseFeed.ambil(data);
                        status.setVisibility(View.GONE);
                        if (mulai == 1 && baru.isEmpty()) {
                            status.setVisibility(View.VISIBLE);
                            status.setText(kataKunci.length() > 0
                                    ? R.string.tidak_ada_hasil : R.string.kosong);
                        } else {
                            isi.addAll(baru);
                            adapter.notifyDataSetChanged();
                        }
                        lebih.setVisibility(baru.size() >= 20 ? View.VISIBLE : View.GONE);
                        mulai += 20;
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (modeTersimpan || kataKunci.length() > 0 || !"Semua".equals(labelDipilih)) {
            modeTersimpan = false;
            kataKunci = "";
            cari.setText("");
            labelDipilih = "Semua";
            mulai = 1;
            isi.clear();
            adapter.notifyDataSetChanged();
            pasangLabel();
            ambil();
        } else {
            super.onBackPressed();
        }
    }
}
