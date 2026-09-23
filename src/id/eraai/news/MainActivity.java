package id.eraai.news;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String FEED =
            "https://eraaidailynews.blogspot.com/feeds/posts/default?alt=json&max-results=20";
    private static final String PARAM_MULAI = "&start-index=";

    private ListView daftar;
    private TextView status, lebih;
    private Adapter adapter;
    private final List<ParseFeed.Berita> isi = new ArrayList<>();
    private int mulai = 1;
    private boolean sedangAmbil = false;

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
            judul.setText(b.judul);
            waktu.setText(b.waktuPendek());
            label.setText(b.label == null ? "" : b.label);
            return v;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        daftar = findViewById(R.id.daftar);
        status = findViewById(R.id.status);
        lebih = findViewById(R.id.lebih);
        TextView segar = findViewById(R.id.tombol_segar);

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
                mulai = 1;
                isi.clear();
                adapter.notifyDataSetChanged();
                ambil();
            }
        });

        lebih.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ambil();
            }
        });

        ambil();
    }

    private void ambil() {
        if (sedangAmbil) return;
        sedangAmbil = true;
        if (mulai == 1) {
            status.setText(R.string.memuat);
            status.setVisibility(View.VISIBLE);
            lebih.setVisibility(View.GONE);
        } else {
            status.setText(R.string.memuat);
        }
        final String alamat = FEED + PARAM_MULAI + mulai;
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
                                status.setVisibility(View.VISIBLE);
                            } else {
                                status.setText("");
                                status.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this,
                                        R.string.gagal, Toast.LENGTH_SHORT).show();
                            }
                            lebih.setVisibility(isi.isEmpty() ? View.GONE : View.VISIBLE);
                            return;
                        }
                        List<ParseFeed.Berita> baru = ParseFeed.ambil(data);
                        status.setVisibility(View.GONE);
                        if (mulai == 1 && baru.isEmpty()) {
                            status.setText(R.string.kosong);
                            status.setVisibility(View.VISIBLE);
                        }
                        if (baru.isEmpty() && mulai > 1) {
                            Toast.makeText(MainActivity.this,
                                    "Semua berita sudah dimuat", Toast.LENGTH_SHORT).show();
                            lebih.setVisibility(View.GONE);
                        } else {
                            isi.addAll(baru);
                            adapter.notifyDataSetChanged();
                            if (baru.size() >= 20) {
                                lebih.setVisibility(View.VISIBLE);
                            } else {
                                lebih.setVisibility(View.GONE);
                            }
                        }
                        mulai += 20;
                    }
                });
            }
        });
    }
}
