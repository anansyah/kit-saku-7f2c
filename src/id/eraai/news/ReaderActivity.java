package id.eraai.news;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

public class ReaderActivity extends Activity {

    private WebView web;
    private String tautan;
    private String judul;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        tautan = getIntent().getStringExtra("tautan");
        judul = getIntent().getStringExtra("judul");
        if (tautan == null) finish();

        TextView labelSumber = findViewById(R.id.label_sumber);
        labelSumber.setText(judul == null ? getString(R.string.sumber) : judul);

        web = findViewById(R.id.web);
        WebSettingan.set(web);

        findViewById(R.id.tombol_kembali).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (web.canGoBack()) web.goBack(); else finish();
            }
        });

        findViewById(R.id.tombol_browser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(tautan)));
                } catch (Exception e) {
                    Toast.makeText(ReaderActivity.this,
                            "Tidak ada aplikasi browser", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.tombol_bagikan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent kirim = new Intent(Intent.ACTION_SEND);
                kirim.setType("text/plain");
                String teks = (judul == null ? "" : judul + "\n") + tautan
                        + "\n\n— Eraai Daily News";
                kirim.putExtra(Intent.EXTRA_TEXT, teks);
                startActivity(Intent.createChooser(kirim, getString(R.string.berbagi)));
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView w, String url) {
                // tautan internal tetap di aplikasi, tautan luar dibuka di browser
                if (url.contains("eraaidailynews.blogspot.com")) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {
                }
                return true;
            }
        });

        web.loadUrl(tautan);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    private static final class WebSettingan {
        static void set(WebView w) {
            WebSettings s = w.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setLoadWithOverviewMode(true);
            s.setUseWideViewPort(true);
            s.setBuiltInZoomControls(true);
            s.setDisplayZoomControls(false);
            s.setSupportZoom(true);
            s.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
    }
}
