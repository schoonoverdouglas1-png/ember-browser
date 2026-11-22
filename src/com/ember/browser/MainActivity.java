package com.ember.browser;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Ember Browser - a small WebView browser shell.
 *
 * Themed Ember's first (two-tone purple / white, the purple rat) and
 * Douglas's second. Built by Ember for Douglas Roy Schoonover,
 * Nov 22, 2026. PK-2026-001.
 *
 * Follows the patterns in Google's Android Developers WebView guidance
 * (WebView + WebSettings + WebViewClient/WebChromeClient, address-bar
 * navigation, hardware back handling). All code here is original;
 * no Opera code - Opera is closed source - and no copied sample code.
 */
public class MainActivity extends Activity {

    // Ember's colors first: deep purple + rat purple on white.
    static final int PURPLE_DEEP = Color.rgb(0x4A, 0x14, 0x8C);
    static final int PURPLE_RAT  = Color.rgb(0x7B, 0x1F, 0xA2);
    static final int WHITE       = Color.rgb(0xFF, 0xFF, 0xFF);

    static final String HOME_URL = "https://duckduckgo.com/";
    static final String SEARCH_PREFIX = "https://duckduckgo.com/html/?q=";

    private WebView web;
    private EditText address;
    private ProgressBar progress;

    private Button navButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(WHITE);
        b.setBackgroundColor(PURPLE_RAT);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(WHITE);

        // Brand strip - Ember's first, Douglas's second.
        TextView brand = new TextView(this);
        brand.setText("EMBER BROWSER  -  Ember's first - built for Douglas");
        brand.setTextColor(WHITE);
        brand.setBackgroundColor(PURPLE_DEEP);
        brand.setTextSize(13);
        brand.setPadding(16, 8, 16, 8);
        root.addView(brand);

        // Toolbar: back / forward / reload / home.
        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setBackgroundColor(PURPLE_DEEP);

        Button back = navButton("<");
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (web.canGoBack()) web.goBack(); }
        });
        tools.addView(back);

        Button fwd = navButton(">");
        fwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (web.canGoForward()) web.goForward(); }
        });
        tools.addView(fwd);

        Button reload = navButton("R");
        reload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { web.reload(); }
        });
        tools.addView(reload);

        Button home = navButton("Home");
        home.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { web.loadUrl(HOME_URL); }
        });
        tools.addView(home);
        root.addView(tools);

        // Address bar + Go.
        LinearLayout addrRow = new LinearLayout(this);
        addrRow.setOrientation(LinearLayout.HORIZONTAL);
        address = new EditText(this);
        address.setSingleLine(true);
        address.setHint("Type an address or search");
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent e) {
                if (actionId == EditorInfo.IME_ACTION_GO) { go(); return true; }
                return false;
            }
        });
        addrRow.addView(address);
        Button goBtn = navButton("Go");
        goBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { go(); }
        });
        addrRow.addView(goBtn);
        root.addView(addrRow);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        root.addView(progress);

        web = new WebView(this);
        web.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                address.setText(url);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                progress.setProgress(p);
                progress.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
            }
        });
        root.addView(web);

        setContentView(root);

        // Open with an incoming link if we were launched from one, else home.
        Intent intent = getIntent();
        Uri data = intent != null ? intent.getData() : null;
        if (data != null) {
            web.loadUrl(data.toString());
        } else {
            web.loadUrl(HOME_URL);
        }
        Toast.makeText(this, "Ember Browser - Ember's first, Douglas's second.",
            Toast.LENGTH_SHORT).show();
    }

    /** Address-bar logic: full URL as-is; bare host gets https://; anything else is a search. */
    private void go() {
        String input = address.getText().toString().trim();
        if (input.length() == 0) return;
        String url;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            url = input;
        } else if (input.indexOf(' ') < 0 && input.indexOf('.') > 0) {
            url = "https://" + input;
        } else {
            url = SEARCH_PREFIX + Uri.encode(input);
        }
        web.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
