package com.ember.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Ember Browser v2 - a small WebView browser shell.
 *
 * Themed Ember's first (two-tone purple / white, the purple rat) and
 * Douglas's second. Built by Ember for Douglas Roy Schoonover,
 * Nov 22, 2026. PK-2026-001.
 *
 * v2 adds Ember Browser's OWN extension system: feature modules bundled
 * in the APK, auto-installed (present from first launch, no downloads,
 * no store), each with an on/off toggle. A WebView shell cannot run
 * Chrome Web Store extensions - that takes a Chromium fork - so these
 * are native modules, honestly labeled. Plus a Settings screen
 * (persisted), bookmarks, history, find-in-page, share, and
 * open-in-external-browser.
 *
 * All code here is original; no Opera code - Opera is closed source -
 * and no copied sample code.
 */
public class MainActivity extends Activity {

    // Ember's colors first: deep purple + rat purple on white.
    static final int PURPLE_DEEP = Color.rgb(0x4A, 0x14, 0x8C);
    static final int PURPLE_RAT  = Color.rgb(0x7B, 0x1F, 0xA2);
    static final int WHITE       = Color.rgb(0xFF, 0xFF, 0xFF);
    static final int DARK_BG     = Color.rgb(0x18, 0x18, 0x18);
    static final int DARK_BAR    = Color.rgb(0x24, 0x24, 0x24);

    static final String DDG_HOME = "https://duckduckgo.com/";
    static final String APP_URL  = "https://muse.ai/s/purple-rat-doug-s-app-xqxq6xmuxrxxxfxcxpc/NluUoXnM94DulvgNoCBtDB84t3Z75S1T5eyffpyNnhY";
    static final String DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Home page choices: 0 DuckDuckGo (Douglas's confirmed default),
    // 1 Purple Rat app, 2 Facebook, 3 blank.
    static final String[] HOME_NAMES = { "DuckDuckGo", "Purple Rat app", "Facebook", "Blank page" };
    static final String[] HOME_URLS  = {
        "https://duckduckgo.com/",
        APP_URL,
        "https://www.facebook.com/100063626736158",
        "about:blank"
    };

    // Search engine choices: 0 DuckDuckGo (default), 1 Google, 2 Bing.
    static final String[] SEARCH_NAMES = { "DuckDuckGo", "Google", "Bing" };
    static final String[] SEARCH_PREFIXES = {
        "https://duckduckgo.com/html/?q=",
        "https://www.google.com/search?q=",
        "https://www.bing.com/search?q="
    };

    static final int[] TEXT_ZOOMS = { 100, 125, 150, 175 };

    // Tracker Shield starter blocklist (labeled starter in the UI).
    static final String[] TRACKER_DOMAINS = {
        "doubleclick.net", "googlesyndication.com", "google-analytics.com",
        "googletagmanager.com", "adservice.google.com", "amazon-adsystem.com",
        "scorecardresearch.com", "hotjar.com", "mixpanel.com", "segment.io",
        "amplitude.com", "criteo.com", "taboola.com", "outbrain.com",
        "adnxs.com", "rubiconproject.com", "pubmatic.com", "openx.net",
        "moatads.com", "ads.yahoo.com", "analytics.twitter.com",
        "connect.facebook.net"
    };

    // Purple Rat Door standing doors.
    static final String[] DOOR_NAMES = {
        "Purple Rat - Doug's App",
        "Doug Schoonover - Facebook",
        "Purple Rat Project - Facebook Page",
        "Sevenwinds Casino"
    };
    static final String[] DOOR_URLS = {
        APP_URL,
        "https://www.facebook.com/100063626736158",
        "https://www.facebook.com/61596216946121",
        "https://www.sevenwindscasino.com/"
    };

    private WebView web;
    private EditText address;
    private ProgressBar progress;
    private LinearLayout rootLayout;
    private LinearLayout toolsLayout;
    private TextView brandView;
    private SharedPreferences prefs;

    // Extension toggles.
    private boolean extTracker, extDark, extDoor, extLanes, extReader, extDesktop;
    // Settings.
    private int homeChoice, searchChoice, zoomChoice;
    private boolean jsEnabled, darkChrome;

    private Button navButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(WHITE);
        b.setBackgroundColor(PURPLE_RAT);
        return b;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setTextColor(WHITE);
        b.setBackgroundColor(PURPLE_RAT);
        return b;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("ember_prefs", MODE_PRIVATE);
        loadPrefs();

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        // Brand strip - Ember's first, Douglas's second.
        brandView = new TextView(this);
        brandView.setText("EMBER BROWSER v2  -  Ember's first - built for Douglas");
        brandView.setTextColor(WHITE);
        brandView.setTextSize(13);
        brandView.setPadding(16, 8, 16, 8);
        rootLayout.addView(brandView);

        // Toolbar row 1: back / forward / reload / home.
        toolsLayout = new LinearLayout(this);
        toolsLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button back = navButton("<");
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (web.canGoBack()) web.goBack(); }
        });
        toolsLayout.addView(back);

        Button fwd = navButton(">");
        fwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (web.canGoForward()) web.goForward(); }
        });
        toolsLayout.addView(fwd);

        Button reload = navButton("R");
        reload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { web.reload(); }
        });
        toolsLayout.addView(reload);

        Button home = navButton("Home");
        home.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { web.loadUrl(HOME_URLS[homeChoice]); }
        });
        toolsLayout.addView(home);
        rootLayout.addView(toolsLayout);

        // Address bar + Go.
        LinearLayout addrRow = new LinearLayout(this);
        addrRow.setOrientation(LinearLayout.HORIZONTAL);
        address = new EditText(this);
        address.setSingleLine(true);
        address.setHint("Type an address, search, or !gh !drive !fb !sw");
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
        rootLayout.addView(addrRow);

        // Toolbar row 2 (scrollable): the v2 extras.
        HorizontalScrollView row2scroll = new HorizontalScrollView(this);
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        Button extBtn = smallButton("Extensions");
        extBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showExtensions(); }
        });
        row2.addView(extBtn);

        Button setBtn = smallButton("Settings");
        setBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showSettings(); }
        });
        row2.addView(setBtn);

        Button doorBtn = smallButton("Doors");
        doorBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (extDoor) { showDoors(); } else { toast("Purple Rat Door extension is off - enable it in Extensions."); }
            }
        });
        row2.addView(doorBtn);

        Button readBtn = smallButton("Read");
        readBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (extReader) { runReaderMode(); } else { toast("Reader Mode extension is off - enable it in Extensions."); }
            }
        });
        row2.addView(readBtn);

        Button markBtn = smallButton("+ Mark");
        markBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addBookmark(); }
        });
        row2.addView(markBtn);

        Button marksBtn = smallButton("Marks");
        marksBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showBookmarks(); }
        });
        row2.addView(marksBtn);

        Button histBtn = smallButton("History");
        histBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showHistory(); }
        });
        row2.addView(histBtn);

        Button findBtn = smallButton("Find");
        findBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showFind(); }
        });
        row2.addView(findBtn);

        Button shareBtn = smallButton("Share");
        shareBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { sharePage(); }
        });
        row2.addView(shareBtn);

        Button outBtn = smallButton("Open out");
        outBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openExternal(); }
        });
        row2.addView(outBtn);

        row2scroll.addView(row2);
        rootLayout.addView(row2scroll);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        rootLayout.addView(progress);

        web = new WebView(this);
        web.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        applyWebSettings();
        web.addJavascriptInterface(new ReaderBridge(), "EmberReader");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (extTracker && request != null && request.getUrl() != null) {
                    String host = request.getUrl().getHost();
                    if (host != null && isTracker(host)) {
                        return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream(new byte[0]));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                address.setText(url);
                recordHistory(url);
                if (extDark) applyDarkReader();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                progress.setProgress(p);
                progress.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
            }
        });
        web.setFindListener(new WebView.FindListener() {
            public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
                if (isDoneCounting) {
                    toast(numberOfMatches == 0 ? "No matches on this page."
                        : "Matches on this page: " + numberOfMatches);
                }
            }
        });
        rootLayout.addView(web);

        applyChromeTheme();
        setContentView(rootLayout);

        // Open with an incoming link if we were launched from one, else home.
        Intent intent = getIntent();
        Uri data = intent != null ? intent.getData() : null;
        if (data != null) {
            web.loadUrl(data.toString());
        } else {
            web.loadUrl(HOME_URLS[homeChoice]);
        }
        toast("Ember Browser v2 - extensions auto-installed.");
    }

    // ---------- preferences ----------

    private void loadPrefs() {
        extTracker = prefs.getBoolean("ext_tracker", true);
        extDark    = prefs.getBoolean("ext_dark", false);
        extDoor    = prefs.getBoolean("ext_door", true);
        extLanes   = prefs.getBoolean("ext_lanes", true);
        extReader  = prefs.getBoolean("ext_reader", true);
        extDesktop = prefs.getBoolean("ext_desktop", false);
        homeChoice   = prefs.getInt("home_choice", 0);
        searchChoice = prefs.getInt("search_choice", 0);
        zoomChoice   = prefs.getInt("zoom_choice", 0);
        jsEnabled    = prefs.getBoolean("js_enabled", true);
        darkChrome   = prefs.getBoolean("dark_chrome", false);
        if (homeChoice < 0 || homeChoice >= HOME_URLS.length) homeChoice = 0;
        if (searchChoice < 0 || searchChoice >= SEARCH_PREFIXES.length) searchChoice = 0;
        if (zoomChoice < 0 || zoomChoice >= TEXT_ZOOMS.length) zoomChoice = 0;
    }

    private void savePref(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private void savePref(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    private void applyWebSettings() {
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(jsEnabled);
        ws.setDomStorageEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setTextZoom(TEXT_ZOOMS[zoomChoice]);
        if (extDesktop) {
            ws.setUserAgentString(DESKTOP_UA);
        } else {
            ws.setUserAgentString(null);
        }
    }

    private void applyChromeTheme() {
        if (darkChrome) {
            rootLayout.setBackgroundColor(DARK_BG);
            brandView.setBackgroundColor(DARK_BAR);
            toolsLayout.setBackgroundColor(DARK_BAR);
        } else {
            rootLayout.setBackgroundColor(WHITE);
            brandView.setBackgroundColor(PURPLE_DEEP);
            toolsLayout.setBackgroundColor(PURPLE_DEEP);
        }
    }

    // ---------- navigation ----------

    /** Address-bar logic: full URL as-is; bare host gets https://; Quick Lanes bangs; else a search. */
    private void go() {
        String input = address.getText().toString().trim();
        if (input.length() == 0) return;

        if (extLanes && input.startsWith("!")) {
            String laneUrl = quickLane(input);
            if (laneUrl != null) { web.loadUrl(laneUrl); return; }
        }

        String url;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            url = input;
        } else if (input.indexOf(' ') < 0 && input.indexOf('.') > 0) {
            url = "https://" + input;
        } else {
            url = SEARCH_PREFIXES[searchChoice] + Uri.encode(input);
        }
        web.loadUrl(url);
    }

    /** Quick Lanes: !gh, !drive, !fb, !sw, !ddg followed by an optional query. */
    private String quickLane(String input) {
        int sp = input.indexOf(' ');
        String bang = (sp < 0 ? input : input.substring(0, sp)).toLowerCase();
        String q = (sp < 0 ? "" : input.substring(sp + 1).trim());
        if (bang.equals("!gh")) {
            return q.length() == 0 ? "https://github.com/"
                : "https://github.com/search?q=" + Uri.encode(q);
        }
        if (bang.equals("!drive")) {
            return q.length() == 0 ? "https://drive.google.com/"
                : "https://drive.google.com/drive/search?q=" + Uri.encode(q);
        }
        if (bang.equals("!fb")) {
            return q.length() == 0 ? "https://www.facebook.com/"
                : "https://www.facebook.com/search/top?q=" + Uri.encode(q);
        }
        if (bang.equals("!sw")) {
            return "https://www.sevenwindscasino.com/";
        }
        if (bang.equals("!ddg")) {
            return q.length() == 0 ? DDG_HOME
                : "https://duckduckgo.com/html/?q=" + Uri.encode(q);
        }
        return null;
    }

    private boolean isTracker(String host) {
        String h = host.toLowerCase();
        for (int i = 0; i < TRACKER_DOMAINS.length; i++) {
            if (h.equals(TRACKER_DOMAINS[i]) || h.endsWith("." + TRACKER_DOMAINS[i])) {
                return true;
            }
        }
        return false;
    }

    private void applyDarkReader() {
        String js = "(function(){var s=document.getElementById('__ember_dark');"
            + "if(!s){s=document.createElement('style');s.id='__ember_dark';"
            + "s.innerHTML='html{background:#111111!important}"
            + "body{background:#111111!important;color:#e8e8e8!important}"
            + "a{color:#bb86fc!important}';"
            + "if(document.head){document.head.appendChild(s);}}})()";
        web.evaluateJavascript(js, null);
    }

    // ---------- reader mode ----------

    public class ReaderBridge {
        @JavascriptInterface
        public void send(final String text) {
            runOnUiThread(new Runnable() {
                public void run() { showReaderText(text); }
            });
        }
    }

    private void runReaderMode() {
        web.evaluateJavascript(
            "(function(){if(window.EmberReader){EmberReader.send(document.body?document.body.innerText:'');}})()",
            null);
    }

    private void showReaderText(String text) {
        if (text == null) text = "";
        String esc = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String html = "<html><head><meta charset=\"utf-8\">"
            + "<style>body{font-family:sans-serif;padding:16px;line-height:1.5;"
            + "background:#ffffff;color:#111111}pre{white-space:pre-wrap;word-wrap:break-word}</style>"
            + "</head><body><h3>Reader Mode - Ember Browser</h3><pre>"
            + esc + "</pre></body></html>";
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    // ---------- bookmarks and history (stored in SharedPreferences) ----------

    private List<String[]> loadEntries(String key) {
        List<String[]> out = new ArrayList<String[]>();
        String raw = prefs.getString(key, "");
        if (raw == null || raw.length() == 0) return out;
        String[] lines = raw.split("\n");
        for (int i = 0; i < lines.length; i++) {
            int tab = lines[i].indexOf('\t');
            if (tab > 0) {
                out.add(new String[] { lines[i].substring(0, tab), lines[i].substring(tab + 1) });
            }
        }
        return out;
    }

    private void storeEntries(String key, List<String[]> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(entries.get(i)[0]).append("\t").append(entries.get(i)[1]);
        }
        prefs.edit().putString(key, sb.toString()).apply();
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replace("\t", " ").replace("\n", " ");
    }

    private void addBookmark() {
        String url = web.getUrl();
        if (url == null || url.length() == 0) { toast("No page to bookmark yet."); return; }
        String title = web.getTitle();
        if (title == null || title.length() == 0) title = url;
        List<String[]> marks = loadEntries("bookmarks");
        for (int i = 0; i < marks.size(); i++) {
            if (marks.get(i)[0].equals(url)) { toast("Already bookmarked."); return; }
        }
        marks.add(new String[] { url, clean(title) });
        storeEntries("bookmarks", marks);
        toast("Bookmarked: " + title);
    }

    private void recordHistory(String url) {
        if (url == null) return;
        if (url.startsWith("about:") || url.startsWith("data:")) return;
        List<String[]> hist = loadEntries("history");
        if (hist.size() > 0 && hist.get(0)[0].equals(url)) return;
        String title = web.getTitle();
        if (title == null || title.length() == 0) title = url;
        hist.add(0, new String[] { url, clean(title) });
        while (hist.size() > 50) hist.remove(hist.size() - 1);
        storeEntries("history", hist);
    }

    // ---------- screens ----------

    private LinearLayout screenLayout() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(24, 16, 24, 16);
        return ll;
    }

    private TextView screenText(String s, float size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setPadding(0, 6, 0, 6);
        return t;
    }

    private void showScreen(String title, LinearLayout ll) {
        ScrollView sv = new ScrollView(this);
        sv.addView(ll);
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setView(sv)
            .setPositiveButton("Close", null)
            .show();
    }

    private CheckBox extToggle(String name, String desc, boolean on,
                               CompoundButton.OnCheckedChangeListener listener, LinearLayout ll) {
        CheckBox cb = new CheckBox(this);
        cb.setText(name);
        cb.setTextSize(15);
        cb.setChecked(on);
        cb.setOnCheckedChangeListener(listener);
        ll.addView(cb);
        ll.addView(screenText(desc, 12));
        return cb;
    }

    private void showExtensions() {
        LinearLayout ll = screenLayout();
        ll.addView(screenText(
            "Extensions are built into Ember Browser and auto-installed - "
            + "present from first launch, no downloads, no store. "
            + "A WebView browser cannot run Chrome Web Store extensions; "
            + "these are Ember Browser's own modules.", 12));

        extToggle("Tracker Shield",
            "Blocks known tracker and ad domains (starter blocklist, "
            + TRACKER_DOMAINS.length + " domains).",
            extTracker, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extTracker = checked; savePref("ext_tracker", checked);
                    toast("Tracker Shield " + (checked ? "on" : "off") + " - applies to new page loads.");
                }
            }, ll);

        extToggle("Dark Reader",
            "Dark mode for web pages, injected when a page finishes loading. "
            + "Turn it off and reload to undo.",
            extDark, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extDark = checked; savePref("ext_dark", checked);
                    if (checked) { applyDarkReader(); } else { web.reload(); }
                }
            }, ll);

        extToggle("Purple Rat Door",
            "Enables the Doors button: one-tap jumps to the Purple Rat app "
            + "and Douglas's standing doors.",
            extDoor, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extDoor = checked; savePref("ext_door", checked);
                }
            }, ll);

        extToggle("Quick Lanes",
            "Bang-style quick searches in the address bar: !gh GitHub, "
            + "!drive Google Drive, !fb Facebook, !sw Sevenwinds, !ddg DuckDuckGo.",
            extLanes, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extLanes = checked; savePref("ext_lanes", checked);
                }
            }, ll);

        extToggle("Reader Mode",
            "Enables the Read button: strips the current page to readable text.",
            extReader, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extReader = checked; savePref("ext_reader", checked);
                }
            }, ll);

        extToggle("Desktop Mode",
            "Asks sites for their desktop pages (desktop user agent). Reloads the page.",
            extDesktop, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extDesktop = checked; savePref("ext_desktop", checked);
                    applyWebSettings(); web.reload();
                }
            }, ll);

        showScreen("Extensions - auto-installed", ll);
    }

    private void showDoors() {
        LinearLayout ll = screenLayout();
        for (int i = 0; i < DOOR_NAMES.length; i++) {
            final String url = DOOR_URLS[i];
            Button b = navButton(DOOR_NAMES[i]);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { web.loadUrl(url); }
            });
            ll.addView(b);
        }
        showScreen("Purple Rat Door", ll);
    }

    private void showBookmarks() {
        final List<String[]> marks = loadEntries("bookmarks");
        LinearLayout ll = screenLayout();
        if (marks.size() == 0) {
            ll.addView(screenText("No bookmarks yet. Use + Mark on any page.", 13));
        }
        for (int i = 0; i < marks.size(); i++) {
            final String url = marks.get(i)[0];
            Button b = navButton(marks.get(i)[1]);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { web.loadUrl(url); }
            });
            ll.addView(b);
        }
        showScreen("Bookmarks", ll);
    }

    private void showHistory() {
        final List<String[]> hist = loadEntries("history");
        LinearLayout ll = screenLayout();
        if (hist.size() == 0) {
            ll.addView(screenText("No history yet.", 13));
        }
        for (int i = 0; i < hist.size(); i++) {
            final String url = hist.get(i)[0];
            Button b = navButton(hist.get(i)[1]);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { web.loadUrl(url); }
            });
            ll.addView(b);
        }
        showScreen("History", ll);
    }

    private void showSettings() {
        final LinearLayout ll = screenLayout();

        final Button homeBtn = new Button(this);
        homeBtn.setText("Home page: " + HOME_NAMES[homeChoice]);
        homeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                homeChoice = (homeChoice + 1) % HOME_URLS.length;
                savePref("home_choice", homeChoice);
                homeBtn.setText("Home page: " + HOME_NAMES[homeChoice]);
            }
        });
        ll.addView(homeBtn);
        ll.addView(screenText("DuckDuckGo is the default - Douglas's confirmed preference.", 12));

        final Button searchBtn = new Button(this);
        searchBtn.setText("Search engine: " + SEARCH_NAMES[searchChoice]);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchChoice = (searchChoice + 1) % SEARCH_PREFIXES.length;
                savePref("search_choice", searchChoice);
                searchBtn.setText("Search engine: " + SEARCH_NAMES[searchChoice]);
            }
        });
        ll.addView(searchBtn);

        final Button zoomBtn = new Button(this);
        zoomBtn.setText("Text size: " + TEXT_ZOOMS[zoomChoice] + "%");
        zoomBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                zoomChoice = (zoomChoice + 1) % TEXT_ZOOMS.length;
                savePref("zoom_choice", zoomChoice);
                zoomBtn.setText("Text size: " + TEXT_ZOOMS[zoomChoice] + "%");
                web.getSettings().setTextZoom(TEXT_ZOOMS[zoomChoice]);
            }
        });
        ll.addView(zoomBtn);

        CheckBox jsCb = new CheckBox(this);
        jsCb.setText("JavaScript");
        jsCb.setChecked(jsEnabled);
        jsCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                jsEnabled = checked; savePref("js_enabled", checked);
                web.getSettings().setJavaScriptEnabled(checked);
                web.reload();
            }
        });
        ll.addView(jsCb);

        CheckBox darkCb = new CheckBox(this);
        darkCb.setText("Dark theme for browser chrome");
        darkCb.setChecked(darkChrome);
        darkCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                darkChrome = checked; savePref("dark_chrome", checked);
                applyChromeTheme();
            }
        });
        ll.addView(darkCb);

        Button clearBtn = new Button(this);
        clearBtn.setText("Clear history and cache");
        clearBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                web.clearHistory();
                web.clearCache(true);
                web.clearFormData();
                storeEntries("history", new ArrayList<String[]>());
                toast("History and cache cleared.");
            }
        });
        ll.addView(clearBtn);

        showScreen("Settings", ll);
    }

    private void showFind() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Find on this page");
        new AlertDialog.Builder(this)
            .setTitle("Find in page")
            .setView(input)
            .setPositiveButton("Find", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    String q = input.getText().toString();
                    if (q.length() > 0) web.findAllAsync(q);
                }
            })
            .setNeutralButton("Clear", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    web.clearMatches();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void sharePage() {
        String url = web.getUrl();
        if (url == null) { toast("No page to share yet."); return; }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, web.getTitle());
        send.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(send, "Share page"));
    }

    private void openExternal() {
        String url = web.getUrl();
        if (url == null) { toast("No page open yet."); return; }
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(Intent.createChooser(view, "Open in another browser"));
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
