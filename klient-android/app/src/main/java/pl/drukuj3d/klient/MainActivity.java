package pl.drukuj3d.klient;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * DRUKUJ 3D – aplikacja dla klientów (beta). Pokazuje stronę z cennikiem i wyceną.
 * Najpierw próbuje wersji z internetu (zawsze aktualna), a bez sieci otwiera kopię zapisaną w aplikacji.
 * Uprawnienia: tylko INTERNET. Żadnych danych właściciela.
 */
public class MainActivity extends Activity {
    private static final String ONLINE = "https://strzygatv.github.io/drukuj3d-program/";
    private static final String OFFLINE = "file:///android_asset/index.html";
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private boolean offline = false;

    public class Most {
        @JavascriptInterface public String wersja() { return "beta"; }
        /** Numer wersji aplikacji – strona porównuje go z najnowszym wydaniem i proponuje aktualizację. */
        @JavascriptInterface public long kod() {
            try { return getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode(); } catch (Exception e) { return 0; }
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#111316")); // po starcie pasek w kolorze aplikacji
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.addJavascriptInterface(new Most(), "KlientApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith(ONLINE) || u.startsWith("file:///android_asset/")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (ActivityNotFoundException ignored) { }
                return true;
            }
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame() && !offline) { offline = true; v.loadUrl(OFFLINE); }
            }
            @Override
            public void onReceivedHttpError(WebView v, WebResourceRequest r, android.webkit.WebResourceResponse resp) {
                if (r.isForMainFrame() && !offline) { offline = true; v.loadUrl(OFFLINE); }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("*/*");                                  // pliki STL / OBJ / 3MF nie mają stałego typu MIME
                try { startActivityForResult(Intent.createChooser(pick, "Wybierz plik modelu"), 7); }
                catch (ActivityNotFoundException e) { fileCallback = null; return false; }
                return true;
            }
        });
        web.loadUrl(ONLINE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != 7 || fileCallback == null) return;
        Uri[] out = null;
        if (res == RESULT_OK && data != null && data.getData() != null) out = new Uri[]{data.getData()};
        fileCallback.onReceiveValue(out);
        fileCallback = null;
    }

    @Override
    public void onBackPressed() { if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
