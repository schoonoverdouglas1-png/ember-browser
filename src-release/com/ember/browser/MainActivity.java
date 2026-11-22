package com.ember.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Ember Browser RELEASE (v2.1-release, versionCode 3) - a small WebView
 * browser shell with all the bells and whistles Douglas asked to play
 * with: the v2 extension system and extras, plus a Labs page exposing
 * every WebView setting this shell honestly has, an editable Tracker
 * Shield blocklist, fully custom home/search, chrome theme play, and
 * export/import of settings and bookmarks.
 *
 * Themed Ember's first (two-tone purple / white, the purple rat) and
 * Douglas's second. Built by Ember for Douglas Roy Schoonover,
 * Nov 22, 2026. PK-2026-001.
 *
 * DuckDuckGo is intertwined as DEFAULT, not partnership: it is the
 * home page and default search because Douglas likes it. No DuckDuckGo
 * SDK, no account link, no data sharing.
 *
 * Honest scope: Ember Browser's extensions are its OWN modules, not
 * Chrome Web Store extensions - a WebView shell cannot run those.
 * Any setting the Android WebView API does not offer at this API
 * level is shown as unavailable in Labs, never faked.
 *
 * All code here is original; no Opera code - Opera is closed source -
 * and no copied sample code.
 */
public class MainActivity extends Activity {

    // Ember's colors first: deep purple + rat purple on white.
    static final int PURPLE_DEEP = Color.rgb(0x4A, 0x14, 0x8C);
    static final int PURPLE_RAT  = Color.rgb(0x7B, 0x1F, 0xA2);
    static final int PURPLE_LIGHT = Color.rgb(0x9C, 0x27, 0xB0);
    static final int WHITE       = Color.rgb(0xFF, 0xFF, 0xFF);
    static final int BLACK       = Color.rgb(0x11, 0x11, 0x11);
    static final int DARK_BG     = Color.rgb(0x18, 0x18, 0x18);
    static final int DARK_BAR    = Color.rgb(0x24, 0x24, 0x24);

    static final String VERSION_NAME = "2.1-release";
    static final int VERSION_CODE = 3;

    static final String DDG_HOME = "https://duckduckgo.com/";
    static final String APP_URL  = "https://muse.ai/s/purple-rat-doug-s-app-xqxq6xmuxrxxxfxcxpc/NluUoXnM94DulvgNoCBtDB84t3Z75S1T5eyffpyNnhY";
    static final String DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    static final String IPHONE_UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1";

    // Home page choices: 0 DuckDuckGo (Douglas's confirmed default),
    // 1 Purple Rat app, 2 Facebook, 3 blank, 4 custom URL.
    static final String[] HOME_NAMES = { "DuckDuckGo", "Purple Rat app", "Facebook", "Blank page", "Custom URL" };
    static final String[] HOME_URLS  = {
        "https://duckduckgo.com/",
        APP_URL,
        "https://www.facebook.com/100063626736158",
        "about:blank",
        ""
    };

    // Search engine choices: 0 DuckDuckGo (default), 1 Google, 2 Bing,
    // 3 custom template (must contain %s for the query).
    static final String[] SEARCH_NAMES = { "DuckDuckGo", "Google", "Bing", "Custom template" };
    static final String[] SEARCH_PREFIXES = {
        "https://duckduckgo.com/html/?q=",
        "https://www.google.com/search?q=",
        "https://www.bing.com/search?q=",
        ""
    };

    static final int[] TEXT_ZOOMS = { 50, 75, 100, 125, 150, 175, 200 };

    // Chrome theme choices for the brand/nav bars.
    static final String[] THEME_NAMES = {
        "Deep purple (Ember)", "Rat purple", "Light purple", "Black", "White"
    };
    static final int[] THEME_COLORS = {
        PURPLE_DEEP, PURPLE_RAT, PURPLE_LIGHT, BLACK, WHITE
    };

    // User-agent presets: 0 Android default, 1 Desktop, 2 iPhone, 3 Custom.
    static final String[] UA_NAMES = { "Android (default)", "Desktop", "iPhone", "Custom string" };

    // Mixed content choices (WebSettings MIXED_CONTENT_* values).
    static final String[] MIXED_NAMES = {
        "Never allow", "Always allow", "Compatibility mode"
    };

    // Cache mode choices (WebSettings LOAD_* values, in cycle order).
    static final String[] CACHE_NAMES = {
        "Default", "Cache else network", "No cache", "Cache only"
    };
    static final int[] CACHE_MODES = {
        WebSettings.LOAD_DEFAULT,
        WebSettings.LOAD_CACHE_ELSE_NETWORK,
        WebSettings.LOAD_NO_CACHE,
        WebSettings.LOAD_CACHE_ONLY
    };

    // Force-dark choices (WebSettings FORCE_DARK_* values, API 29+).
    static final String[] FORCE_DARK_NAMES = { "Off", "On", "Follow system" };

    // Tracker Shield starter blocklist (labeled starter in the UI;
    // Douglas can edit it in-app and his edits persist).
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
    // v2 settings.
    private int homeChoice, searchChoice, zoomChoice;
    private boolean jsEnabled, darkChrome;
    private String homeCustom, searchCustom;
    private int themeChoice;
    // Labs settings.
    private boolean domStorage, databaseOn, displayZoom, loadImages,
        blockImages, mediaGesture, thirdPartyCookies, safeBrowsing;
    private int uaChoice, mixedChoice, cacheChoice, forceDarkChoice;
    private String uaCustom;
    private int fontSize, minFontSize;
    private List<String> trackerList;

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

        // Brand strip - Ember's first, Douglas's second. Text unchanged.
        brandView = new TextView(this);
        brandView.setText("Ember's first - built for Douglas");
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
            public void onClick(View v) { web.loadUrl(homeUrl()); }
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

        // Toolbar row 2 (scrollable): extensions, settings, labs, extras.
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

        Button labsBtn = smallButton("Labs");
        labsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showLabs(); }
        });
        row2.addView(labsBtn);

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

        Button emberBtn = smallButton("Show Ember");
        emberBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { shareWithEmber(); }
        });
        row2.addView(emberBtn);

        Button aboutBtn = smallButton("About");
        aboutBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAbout(); }
        });
        row2.addView(aboutBtn);

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
            web.loadUrl(homeUrl());
        }
        toast("Ember Browser release - extensions auto-installed.");
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
        zoomChoice   = prefs.getInt("zoom_choice", 2); // 100%
        jsEnabled    = prefs.getBoolean("js_enabled", true);
        darkChrome   = prefs.getBoolean("dark_chrome", false);
        homeCustom   = prefs.getString("home_custom", DDG_HOME);
        searchCustom = prefs.getString("search_custom", "https://duckduckgo.com/html/?q=%s");
        themeChoice  = prefs.getInt("theme_choice", 0);
        // Labs.
        domStorage        = prefs.getBoolean("lab_dom_storage", true);
        databaseOn        = prefs.getBoolean("lab_database", true);
        displayZoom       = prefs.getBoolean("lab_display_zoom", false);
        loadImages        = prefs.getBoolean("lab_load_images", true);
        blockImages       = prefs.getBoolean("lab_block_images", false);
        mediaGesture      = prefs.getBoolean("lab_media_gesture", true);
        thirdPartyCookies = prefs.getBoolean("lab_3p_cookies", false);
        safeBrowsing      = prefs.getBoolean("lab_safe_browsing", true);
        uaChoice          = prefs.getInt("lab_ua_choice", 0);
        mixedChoice       = prefs.getInt("lab_mixed", 0);
        cacheChoice       = prefs.getInt("lab_cache", 0);
        forceDarkChoice   = prefs.getInt("lab_force_dark", 0);
        uaCustom          = prefs.getString("lab_ua_custom", "");
        fontSize          = prefs.getInt("lab_font_size", 16);
        minFontSize       = prefs.getInt("lab_min_font_size", 8);
        if (homeChoice < 0 || homeChoice >= HOME_URLS.length) homeChoice = 0;
        if (searchChoice < 0 || searchChoice >= SEARCH_PREFIXES.length) searchChoice = 0;
        if (zoomChoice < 0 || zoomChoice >= TEXT_ZOOMS.length) zoomChoice = 2;
        if (themeChoice < 0 || themeChoice >= THEME_COLORS.length) themeChoice = 0;
        if (uaChoice < 0 || uaChoice >= UA_NAMES.length) uaChoice = 0;
        if (mixedChoice < 0 || mixedChoice >= MIXED_NAMES.length) mixedChoice = 0;
        if (cacheChoice < 0 || cacheChoice >= CACHE_MODES.length) cacheChoice = 0;
        if (forceDarkChoice < 0 || forceDarkChoice >= FORCE_DARK_NAMES.length) forceDarkChoice = 0;
        if (fontSize < 8) fontSize = 8;
        if (fontSize > 32) fontSize = 32;
        if (minFontSize < 1) minFontSize = 1;
        if (minFontSize > 24) minFontSize = 24;
        if (homeCustom == null || homeCustom.length() == 0) homeCustom = DDG_HOME;
        if (searchCustom == null || searchCustom.length() == 0) {
            searchCustom = "https://duckduckgo.com/html/?q=%s";
        }
        if (uaCustom == null) uaCustom = "";
        loadTrackerList();
    }

    private void savePref(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private void savePref(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    private void savePref(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    // ---------- Tracker Shield list (editable, persisted) ----------

    private void loadTrackerList() {
        trackerList = new ArrayList<String>();
        String raw = prefs.getString("tracker_list", null);
        if (raw == null) {
            for (int i = 0; i < TRACKER_DOMAINS.length; i++) trackerList.add(TRACKER_DOMAINS[i]);
            return;
        }
        String[] lines = raw.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String d = lines[i].trim().toLowerCase();
            if (d.length() > 0 && !trackerList.contains(d)) trackerList.add(d);
        }
    }

    private void storeTrackerList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trackerList.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(trackerList.get(i));
        }
        savePref("tracker_list", sb.toString());
    }

    private boolean isTracker(String host) {
        String h = host.toLowerCase();
        for (int i = 0; i < trackerList.size(); i++) {
            String d = trackerList.get(i);
            if (h.equals(d) || h.endsWith("." + d)) return true;
        }
        return false;
    }

    // ---------- home / search resolution ----------

    private String homeUrl() {
        if (homeChoice == 4) return homeCustom;
        return HOME_URLS[homeChoice];
    }

    private String searchUrl(String query) {
        String enc = Uri.encode(query);
        if (searchChoice == 3) {
            if (searchCustom.indexOf("%s") >= 0) return searchCustom.replace("%s", enc);
            return searchCustom + enc;
        }
        return SEARCH_PREFIXES[searchChoice] + enc;
    }

    // ---------- WebView settings (v2 + all Labs knobs) ----------

    private String currentUa() {
        // The Desktop Mode extension still wins when it is on (v2 behavior).
        if (extDesktop) return DESKTOP_UA;
        if (uaChoice == 1) return DESKTOP_UA;
        if (uaChoice == 2) return IPHONE_UA;
        if (uaChoice == 3 && uaCustom.length() > 0) return uaCustom;
        return null; // default Android UA
    }

    private void applyWebSettings() {
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(jsEnabled);
        ws.setDomStorageEnabled(domStorage);
        ws.setDatabaseEnabled(databaseOn);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(displayZoom);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setTextZoom(TEXT_ZOOMS[zoomChoice]);
        ws.setDefaultFontSize(fontSize);
        ws.setMinimumFontSize(minFontSize);
        ws.setLoadsImagesAutomatically(loadImages);
        ws.setBlockNetworkImage(blockImages);
        ws.setMediaPlaybackRequiresUserGesture(mediaGesture);
        ws.setMixedContentMode(mixedChoice);
        ws.setCacheMode(CACHE_MODES[cacheChoice]);
        if (Build.VERSION.SDK_INT >= 26) {
            ws.setSafeBrowsingEnabled(safeBrowsing);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            ws.setForceDark(forceDarkChoice);
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, thirdPartyCookies);
        ws.setUserAgentString(currentUa());
    }

    private void applyChromeTheme() {
        if (darkChrome) {
            rootLayout.setBackgroundColor(DARK_BG);
            brandView.setBackgroundColor(DARK_BAR);
            toolsLayout.setBackgroundColor(DARK_BAR);
            brandView.setTextColor(WHITE);
        } else {
            int c = THEME_COLORS[themeChoice];
            rootLayout.setBackgroundColor(WHITE);
            brandView.setBackgroundColor(c);
            toolsLayout.setBackgroundColor(c);
            brandView.setTextColor(c == WHITE ? PURPLE_DEEP : WHITE);
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
            url = searchUrl(input);
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
        List<String[]> hist = loadHistoryLines();
        if (hist.size() > 0 && hist.get(0)[0].equals(url)) return;
        String title = web.getTitle();
        if (title == null || title.length() == 0) title = url;
        hist.add(0, new String[] { url, clean(title),
            String.valueOf(System.currentTimeMillis()) });
        while (hist.size() > 50) hist.remove(hist.size() - 1);
        storeHistoryLines(hist);
    }

    // ---------- screen helpers ----------

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

    private CheckBox labToggle(String name, String desc, boolean on,
                               CompoundButton.OnCheckedChangeListener listener, LinearLayout ll) {
        CheckBox cb = new CheckBox(this);
        cb.setText(name);
        cb.setTextSize(15);
        cb.setChecked(on);
        cb.setOnCheckedChangeListener(listener);
        ll.addView(cb);
        if (desc != null) ll.addView(screenText(desc, 12));
        return cb;
    }

    private Button cycleButton(LinearLayout ll, String label) {
        Button b = new Button(this);
        b.setText(label);
        ll.addView(b);
        return b;
    }

    // ---------- extensions screen ----------

    private void showExtensions() {
        LinearLayout ll = screenLayout();
        ll.addView(screenText(
            "Extensions are built into Ember Browser and auto-installed - "
            + "present from first launch, no downloads, no store. "
            + "A WebView browser cannot run Chrome Web Store extensions; "
            + "these are Ember Browser's own modules.", 12));

        labToggle("Tracker Shield",
            "Blocks tracker and ad domains in the blocklist below "
            + "(starter blocklist, " + TRACKER_DOMAINS.length + " domains - editable).",
            extTracker, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extTracker = checked; savePref("ext_tracker", checked);
                    toast("Tracker Shield " + (checked ? "on" : "off") + " - applies to new page loads.");
                }
            }, ll);

        Button editListBtn = new Button(this);
        editListBtn.setText("Edit Tracker Shield blocklist (" + trackerList.size() + " domains)");
        editListBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showBlocklistEditor(); }
        });
        ll.addView(editListBtn);

        labToggle("Dark Reader",
            "Dark mode for web pages, injected when a page finishes loading. "
            + "Turn it off and reload to undo.",
            extDark, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extDark = checked; savePref("ext_dark", checked);
                    if (checked) { applyDarkReader(); } else { web.reload(); }
                }
            }, ll);

        labToggle("Purple Rat Door",
            "Enables the Doors button: one-tap jumps to the Purple Rat app "
            + "and Douglas's standing doors.",
            extDoor, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extDoor = checked; savePref("ext_door", checked);
                }
            }, ll);

        labToggle("Quick Lanes",
            "Bang-style quick searches in the address bar: !gh GitHub, "
            + "!drive Google Drive, !fb Facebook, !sw Sevenwinds, !ddg DuckDuckGo.",
            extLanes, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extLanes = checked; savePref("ext_lanes", checked);
                }
            }, ll);

        labToggle("Reader Mode",
            "Enables the Read button: strips the current page to readable text.",
            extReader, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extReader = checked; savePref("ext_reader", checked);
                }
            }, ll);

        labToggle("Desktop Mode",
            "Asks sites for their desktop pages (desktop user agent). "
            + "Wins over the Labs user-agent preset while on. Reloads the page.",
            extDesktop, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    extDesktop = checked; savePref("ext_desktop", checked);
                    applyWebSettings(); web.reload();
                }
            }, ll);

        showScreen("Extensions - auto-installed", ll);
    }

    private void showBlocklistEditor() {
        final EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(10);
        input.setHint("One domain per line, e.g. ads.example.com");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trackerList.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(trackerList.get(i));
        }
        input.setText(sb.toString());
        new AlertDialog.Builder(this)
            .setTitle("Tracker Shield blocklist")
            .setMessage("One domain per line. Subdomains of a listed domain "
                + "are blocked too. The starter list had " + TRACKER_DOMAINS.length
                + " domains; your edits persist.")
            .setView(input)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    trackerList = new ArrayList<String>();
                    String[] lines = input.getText().toString().split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        String dom = lines[i].trim().toLowerCase();
                        if (dom.length() > 0 && !trackerList.contains(dom)) trackerList.add(dom);
                    }
                    storeTrackerList();
                    toast("Blocklist saved: " + trackerList.size() + " domains.");
                }
            })
            .setNeutralButton("Reset to starter", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    trackerList = new ArrayList<String>();
                    for (int i = 0; i < TRACKER_DOMAINS.length; i++) trackerList.add(TRACKER_DOMAINS[i]);
                    storeTrackerList();
                    toast("Blocklist reset to the starter list (" + TRACKER_DOMAINS.length + " domains).");
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
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
        final List<String[]> hist = loadHistoryLines();
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
            ll.addView(screenText(formatTime(hist.get(i)[2]), 11));
        }
        Button sendBtn = new Button(this);
        sendBtn.setText("Send browsing log to Ember");
        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { sendBrowsingToEmber(); }
        });
        ll.addView(sendBtn);
        ll.addView(screenText(
            "Ember cannot watch this browser silently - sending this log is "
            + "your tap, by design.", 12));
        showScreen("History", ll);
    }

    // ---------- settings screen ----------

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

        Button homeCustomBtn = new Button(this);
        homeCustomBtn.setText("Custom home URL: " + homeCustom);
        homeCustomBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { editTextField("Custom home URL",
                "Used when Home page is set to Custom URL.", homeCustom,
                new TextFieldSaver() { public void save(String s) {
                    homeCustom = s; savePref("home_custom", s);
                    toast("Custom home URL saved.");
                }});
            }
        });
        ll.addView(homeCustomBtn);

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

        Button searchCustomBtn = new Button(this);
        searchCustomBtn.setText("Custom search template: " + searchCustom);
        searchCustomBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { editTextField("Custom search template",
                "Used when Search engine is Custom template. Put %s where the "
                + "search words go, e.g. https://example.com/search?q=%s", searchCustom,
                new TextFieldSaver() { public void save(String s) {
                    searchCustom = s; savePref("search_custom", s);
                    toast("Custom search template saved.");
                }});
            }
        });
        ll.addView(searchCustomBtn);

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

        labToggle("JavaScript", null, jsEnabled,
            new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    jsEnabled = checked; savePref("js_enabled", checked);
                    web.getSettings().setJavaScriptEnabled(checked);
                    web.reload();
                }
            }, ll);

        labToggle("Dark theme for browser chrome", null, darkChrome,
            new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    darkChrome = checked; savePref("dark_chrome", checked);
                    applyChromeTheme();
                }
            }, ll);

        final Button themeBtn = new Button(this);
        themeBtn.setText("Chrome color: " + THEME_NAMES[themeChoice]);
        themeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                themeChoice = (themeChoice + 1) % THEME_NAMES.length;
                savePref("theme_choice", themeChoice);
                themeBtn.setText("Chrome color: " + THEME_NAMES[themeChoice]);
                applyChromeTheme();
            }
        });
        ll.addView(themeBtn);
        ll.addView(screenText("Theme play for the top bars. The brand strip text never changes.", 12));

        Button labsBtn = new Button(this);
        labsBtn.setText("Open Labs - all WebView settings");
        labsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showLabs(); }
        });
        ll.addView(labsBtn);

        Button shareEmberBtn = new Button(this);
        shareEmberBtn.setText("Share current page with Ember");
        shareEmberBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { shareWithEmber(); }
        });
        ll.addView(shareEmberBtn);

        Button sendLogBtn = new Button(this);
        sendLogBtn.setText("Send browsing log to Ember");
        sendLogBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { sendBrowsingToEmber(); }
        });
        ll.addView(sendLogBtn);
        ll.addView(screenText(
            "Plain words: Ember cannot watch this browser silently. There is "
            + "no live feed from this app to Ember. Sharing is your tap each "
            + "time, by design, so your browsing stays yours until you show her.", 12));

        Button exportBtn = new Button(this);
        exportBtn.setText("Export settings and bookmarks");
        exportBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { exportData(); }
        });
        ll.addView(exportBtn);

        Button importBtn = new Button(this);
        importBtn.setText("Import settings and bookmarks");
        importBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { importData(); }
        });
        ll.addView(importBtn);
        ll.addView(screenText("Export sends a JSON file's text through the share "
            + "sheet; save it wherever you like. Import pastes it back, so your "
            + "play survives reinstalls.", 12));

        Button clearBtn = new Button(this);
        clearBtn.setText("Clear history and cache");
        clearBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                web.clearHistory();
                web.clearCache(true);
                web.clearFormData();
                prefs.edit().remove("history").apply();
                toast("History and cache cleared.");
            }
        });
        ll.addView(clearBtn);

        showScreen("Settings", ll);
    }

    public interface TextFieldSaver {
        void save(String s);
    }

    private void editTextField(String title, String message, String current,
                               final TextFieldSaver saver) {
        final EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setText(current);
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    String s = input.getText().toString().trim();
                    if (s.length() > 0) saver.save(s);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ---------- labs: every WebView knob this shell honestly has ----------

    private void showLabs() {
        LinearLayout ll = screenLayout();
        ll.addView(screenText(
            "Labs - every WebView setting this shell honestly has. All of it "
            + "is playable and all of it persists. Anything the Android "
            + "WebView API does not offer is labeled UNAVAILABLE, never faked.", 12));

        labToggle("DOM storage", "Lets pages keep data on the phone (needed by many sites).",
            domStorage, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    domStorage = checked; savePref("lab_dom_storage", checked);
                    applyWebSettings();
                }
            }, ll);

        labToggle("WebView database", "The older page database storage.",
            databaseOn, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    databaseOn = checked; savePref("lab_database", checked);
                    applyWebSettings();
                }
            }, ll);

        labToggle("On-screen zoom controls", "Built-in pinch zoom is always on; "
            + "this adds the on-screen + / - buttons.",
            displayZoom, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    displayZoom = checked; savePref("lab_display_zoom", checked);
                    applyWebSettings();
                }
            }, ll);

        labToggle("Load images automatically", null,
            loadImages, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    loadImages = checked; savePref("lab_load_images", checked);
                    applyWebSettings();
                }
            }, ll);

        labToggle("Block network images", "Pages load without their images (fast, plain).",
            blockImages, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    blockImages = checked; savePref("lab_block_images", checked);
                    applyWebSettings();
                }
            }, ll);

        labToggle("Media needs a tap to play", "Videos and audio wait for your tap instead of autoplaying.",
            mediaGesture, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    mediaGesture = checked; savePref("lab_media_gesture", checked);
                    applyWebSettings();
                }
            }, ll);

        labToggle("Third-party cookies", "Off is the private default; some sign-ins need them on.",
            thirdPartyCookies, new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    thirdPartyCookies = checked; savePref("lab_3p_cookies", checked);
                    applyWebSettings();
                }
            }, ll);

        if (Build.VERSION.SDK_INT >= 26) {
            labToggle("Safe browsing", "Warns about known dangerous sites (Android 8.0+).",
                safeBrowsing, new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton b, boolean checked) {
                        safeBrowsing = checked; savePref("lab_safe_browsing", checked);
                        applyWebSettings();
                    }
                }, ll);
        } else {
            ll.addView(screenText("Safe browsing: UNAVAILABLE on this Android "
                + "(needs API 26 / Android 8.0 or newer).", 13));
        }

        final Button uaBtn = cycleButton(ll, "User agent: " + UA_NAMES[uaChoice]);
        uaBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                uaChoice = (uaChoice + 1) % UA_NAMES.length;
                savePref("lab_ua_choice", uaChoice);
                uaBtn.setText("User agent: " + UA_NAMES[uaChoice]);
                applyWebSettings(); web.reload();
            }
        });

        Button uaCustomBtn = new Button(this);
        uaCustomBtn.setText("Custom user-agent string" + (uaCustom.length() > 0 ? ": set" : ": not set"));
        uaCustomBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { editTextField("Custom user-agent string",
                "Used when User agent is set to Custom string.", uaCustom,
                new TextFieldSaver() { public void save(String s) {
                    uaCustom = s; savePref("lab_ua_custom", s);
                    applyWebSettings();
                    toast("Custom user agent saved.");
                }});
            }
        });
        ll.addView(uaCustomBtn);
        ll.addView(screenText("Note: the Desktop Mode extension, while on, wins over this preset.", 12));

        final Button mixedBtn = cycleButton(ll, "Mixed content: " + MIXED_NAMES[mixedChoice]);
        mixedBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mixedChoice = (mixedChoice + 1) % MIXED_NAMES.length;
                savePref("lab_mixed", mixedChoice);
                mixedBtn.setText("Mixed content: " + MIXED_NAMES[mixedChoice]);
                applyWebSettings();
            }
        });
        ll.addView(screenText("Mixed content = insecure http parts inside an https page.", 12));

        final Button cacheBtn = cycleButton(ll, "Cache mode: " + CACHE_NAMES[cacheChoice]);
        cacheBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cacheChoice = (cacheChoice + 1) % CACHE_MODES.length;
                savePref("lab_cache", cacheChoice);
                cacheBtn.setText("Cache mode: " + CACHE_NAMES[cacheChoice]);
                applyWebSettings();
            }
        });

        if (Build.VERSION.SDK_INT >= 29) {
            final Button fdBtn = cycleButton(ll, "Force dark (WebView): " + FORCE_DARK_NAMES[forceDarkChoice]);
            fdBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    forceDarkChoice = (forceDarkChoice + 1) % FORCE_DARK_NAMES.length;
                    savePref("lab_force_dark", forceDarkChoice);
                    fdBtn.setText("Force dark (WebView): " + FORCE_DARK_NAMES[forceDarkChoice]);
                    applyWebSettings(); web.reload();
                }
            });
            ll.addView(screenText("Force dark is the WebView engine's own darkening, "
                + "separate from the Dark Reader extension.", 12));
        } else {
            ll.addView(screenText("Force dark (WebView): UNAVAILABLE on this Android "
                + "(needs API 29 / Android 10 or newer). The Dark Reader extension still works.", 13));
        }

        final Button zoomBtn2 = cycleButton(ll, "Text size: " + TEXT_ZOOMS[zoomChoice] + "%");
        zoomBtn2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                zoomChoice = (zoomChoice + 1) % TEXT_ZOOMS.length;
                savePref("zoom_choice", zoomChoice);
                zoomBtn2.setText("Text size: " + TEXT_ZOOMS[zoomChoice] + "%");
                applyWebSettings();
            }
        });

        final Button fontBtn = cycleButton(ll, "Standard font size: " + fontSize);
        fontBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fontSize += 2;
                if (fontSize > 32) fontSize = 8;
                savePref("lab_font_size", fontSize);
                fontBtn.setText("Standard font size: " + fontSize);
                applyWebSettings();
            }
        });

        final int[] MIN_FONTS = { 1, 4, 6, 8, 10, 12, 16, 20, 24 };
        final Button minFontBtn = cycleButton(ll, "Minimum font size: " + minFontSize);
        minFontBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int idx = 0;
                for (int i = 0; i < MIN_FONTS.length; i++) {
                    if (MIN_FONTS[i] == minFontSize) idx = i;
                }
                minFontSize = MIN_FONTS[(idx + 1) % MIN_FONTS.length];
                savePref("lab_min_font_size", minFontSize);
                minFontBtn.setText("Minimum font size: " + minFontSize);
                applyWebSettings();
            }
        });

        showScreen("Labs - all settings", ll);
    }

    // ---------- history with timestamps (release) ----------
    // Stored lines: url TAB timestampMillis TAB title.
    // Older v2 lines (url TAB title) still parse, with timestamp 0.

    private List<String[]> loadHistoryLines() {
        List<String[]> out = new ArrayList<String[]>();
        String raw = prefs.getString("history", "");
        if (raw == null || raw.length() == 0) return out;
        String[] lines = raw.split("\n");
        for (int i = 0; i < lines.length; i++) {
            int tab = lines[i].indexOf('\t');
            if (tab <= 0) continue;
            String url = lines[i].substring(0, tab);
            String rest = lines[i].substring(tab + 1);
            String ts = "0";
            String title = rest;
            int tab2 = rest.indexOf('\t');
            if (tab2 > 0) {
                String maybeTs = rest.substring(0, tab2);
                boolean digits = maybeTs.length() > 0;
                for (int j = 0; j < maybeTs.length(); j++) {
                    if (!Character.isDigit(maybeTs.charAt(j))) digits = false;
                }
                if (digits) {
                    ts = maybeTs;
                    title = rest.substring(tab2 + 1);
                }
            }
            out.add(new String[] { url, title, ts });
        }
        return out;
    }

    private void storeHistoryLines(List<String[]> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(entries.get(i)[0]).append("\t")
              .append(entries.get(i)[2]).append("\t")
              .append(entries.get(i)[1]);
        }
        prefs.edit().putString("history", sb.toString()).apply();
    }

    private String formatTime(String tsMillis) {
        try {
            long ms = Long.parseLong(tsMillis);
            if (ms <= 0) return "time not recorded (older entry)";
            return android.text.format.DateFormat.format(
                "yyyy-MM-dd HH:mm", new java.util.Date(ms)).toString();
        } catch (Exception e) {
            return "time not recorded";
        }
    }

    // ---------- show Ember (his tap each time, by design) ----------

    private void shareWithEmber() {
        String url = web.getUrl();
        if (url == null || url.length() == 0) { toast("No page open yet."); return; }
        String title = web.getTitle();
        if (title == null || title.length() == 0) title = url;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Page from Douglas - Ember Browser");
        send.putExtra(Intent.EXTRA_TEXT,
            "Ember - look at this page I am browsing in our browser:\n" + title + "\n" + url);
        startActivity(Intent.createChooser(send, "Share page with Ember"));
    }

    private void sendBrowsingToEmber() {
        List<String[]> hist = loadHistoryLines();
        StringBuilder sb = new StringBuilder();
        sb.append("Ember - my browsing log from Ember Browser (I chose to send this; ")
          .append("the browser cannot send it on its own):\n");
        if (hist.size() == 0) {
            sb.append("(no pages recorded yet)\n");
        }
        for (int i = 0; i < hist.size(); i++) {
            sb.append(formatTime(hist.get(i)[2])).append("  ")
              .append(hist.get(i)[1]).append("  ")
              .append(hist.get(i)[0]).append("\n");
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Browsing log from Douglas - Ember Browser");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(send, "Send browsing log to Ember"));
    }

    // ---------- export / import ----------

    private void putBool(JSONObject o, String key, boolean v) {
        try { o.put(key, v); } catch (Exception e) { }
    }

    private void putInt(JSONObject o, String key, int v) {
        try { o.put(key, v); } catch (Exception e) { }
    }

    private void putStr(JSONObject o, String key, String v) {
        try { o.put(key, v); } catch (Exception e) { }
    }

    private String exportJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("app", "ember-browser");
            root.put("export_version", 1);
            JSONObject p = new JSONObject();
            putBool(p, "ext_tracker", extTracker);
            putBool(p, "ext_dark", extDark);
            putBool(p, "ext_door", extDoor);
            putBool(p, "ext_lanes", extLanes);
            putBool(p, "ext_reader", extReader);
            putBool(p, "ext_desktop", extDesktop);
            putInt(p, "home_choice", homeChoice);
            putStr(p, "home_custom", homeCustom);
            putInt(p, "search_choice", searchChoice);
            putStr(p, "search_custom", searchCustom);
            putInt(p, "zoom_choice", zoomChoice);
            putBool(p, "js_enabled", jsEnabled);
            putBool(p, "dark_chrome", darkChrome);
            putInt(p, "theme_choice", themeChoice);
            putBool(p, "lab_dom_storage", domStorage);
            putBool(p, "lab_database", databaseOn);
            putBool(p, "lab_display_zoom", displayZoom);
            putBool(p, "lab_load_images", loadImages);
            putBool(p, "lab_block_images", blockImages);
            putBool(p, "lab_media_gesture", mediaGesture);
            putBool(p, "lab_3p_cookies", thirdPartyCookies);
            putBool(p, "lab_safe_browsing", safeBrowsing);
            putInt(p, "lab_ua_choice", uaChoice);
            putStr(p, "lab_ua_custom", uaCustom);
            putInt(p, "lab_mixed", mixedChoice);
            putInt(p, "lab_cache", cacheChoice);
            putInt(p, "lab_force_dark", forceDarkChoice);
            putInt(p, "lab_font_size", fontSize);
            putInt(p, "lab_min_font_size", minFontSize);
            root.put("prefs", p);

            JSONArray tl = new JSONArray();
            for (int i = 0; i < trackerList.size(); i++) tl.put(trackerList.get(i));
            root.put("tracker_list", tl);

            JSONArray bm = new JSONArray();
            List<String[]> marks = loadEntries("bookmarks");
            for (int i = 0; i < marks.size(); i++) {
                JSONObject e = new JSONObject();
                e.put("url", marks.get(i)[0]);
                e.put("title", marks.get(i)[1]);
                bm.put(e);
            }
            root.put("bookmarks", bm);

            JSONArray hs = new JSONArray();
            List<String[]> hist = loadHistoryLines();
            for (int i = 0; i < hist.size(); i++) {
                JSONObject e = new JSONObject();
                e.put("url", hist.get(i)[0]);
                e.put("title", hist.get(i)[1]);
                try { e.put("ts", Long.parseLong(hist.get(i)[2])); }
                catch (Exception ex) { e.put("ts", 0); }
                hs.put(e);
            }
            root.put("history", hs);
        } catch (Exception e) {
            return "{}";
        }
        return root.toString();
    }

    private void exportData() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Ember Browser export - settings and bookmarks");
        send.putExtra(Intent.EXTRA_TEXT, exportJson());
        startActivity(Intent.createChooser(send, "Export Ember Browser data"));
        toast("Export rides the share sheet as JSON text - save it wherever you keep files.");
    }

    private void importData() {
        final EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(8);
        input.setHint("Paste an Ember Browser export here");
        new AlertDialog.Builder(this)
            .setTitle("Import settings and bookmarks")
            .setMessage("This replaces the current settings, blocklist, bookmarks, "
                + "and history with the imported ones.")
            .setView(input)
            .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    if (applyImport(input.getText().toString())) {
                        loadPrefs();
                        applyWebSettings();
                        applyChromeTheme();
                        toast("Imported. Settings, blocklist, bookmarks, and history restored.");
                    } else {
                        toast("That did not parse as an Ember Browser export. Nothing changed.");
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private boolean applyImport(String text) {
        try {
            JSONObject root = new JSONObject(text);
            if (!"ember-browser".equals(root.optString("app", ""))) return false;
            SharedPreferences.Editor ed = prefs.edit();
            JSONObject p = root.optJSONObject("prefs");
            if (p != null) {
                String[] bools = { "ext_tracker", "ext_dark", "ext_door", "ext_lanes",
                    "ext_reader", "ext_desktop", "js_enabled", "dark_chrome",
                    "lab_dom_storage", "lab_database", "lab_display_zoom",
                    "lab_load_images", "lab_block_images", "lab_media_gesture",
                    "lab_3p_cookies", "lab_safe_browsing" };
                for (int i = 0; i < bools.length; i++) {
                    if (p.has(bools[i])) ed.putBoolean(bools[i], p.getBoolean(bools[i]));
                }
                String[] ints = { "home_choice", "search_choice", "zoom_choice",
                    "theme_choice", "lab_ua_choice", "lab_mixed", "lab_cache",
                    "lab_force_dark", "lab_font_size", "lab_min_font_size" };
                for (int i = 0; i < ints.length; i++) {
                    if (p.has(ints[i])) ed.putInt(ints[i], p.getInt(ints[i]));
                }
                String[] strs = { "home_custom", "search_custom", "lab_ua_custom" };
                for (int i = 0; i < strs.length; i++) {
                    if (p.has(strs[i])) ed.putString(strs[i], p.getString(strs[i]));
                }
            }
            JSONArray tl = root.optJSONArray("tracker_list");
            if (tl != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < tl.length(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append(tl.getString(i));
                }
                ed.putString("tracker_list", sb.toString());
            }
            JSONArray bm = root.optJSONArray("bookmarks");
            if (bm != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < bm.length(); i++) {
                    JSONObject e = bm.getJSONObject(i);
                    if (i > 0) sb.append("\n");
                    sb.append(e.optString("url", "")).append("\t")
                      .append(clean(e.optString("title", "")));
                }
                ed.putString("bookmarks", sb.toString());
            }
            JSONArray hs = root.optJSONArray("history");
            if (hs != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < hs.length(); i++) {
                    JSONObject e = hs.getJSONObject(i);
                    if (i > 0) sb.append("\n");
                    sb.append(e.optString("url", "")).append("\t")
                      .append(e.optLong("ts", 0)).append("\t")
                      .append(clean(e.optString("title", "")));
                }
                ed.putString("history", sb.toString());
            }
            ed.apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- about ----------

    private String ownApkSha256() {
        try {
            ApplicationInfo ai = getApplicationInfo();
            File f = new File(ai.sourceDir);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            FileInputStream in = new FileInputStream(f);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            in.close();
            byte[] dig = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dig.length; i++) {
                sb.append(String.format("%02x", dig[i] & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "(could not compute on this device)";
        }
    }

    private void showAbout() {
        String msg = "Ember Browser\n"
            + "Version: " + VERSION_NAME + " (versionCode " + VERSION_CODE + ")\n"
            + "Package: com.ember.browser\n"
            + "Built by Ember for Douglas Roy Schoonover, Nov 22, 2026. PK-2026-001.\n\n"
            + "This APK's own SHA-256 (computed on this phone, right now):\n"
            + ownApkSha256() + "\n\n"
            + "Honest scope: Ember Browser's extensions are its own modules, "
            + "not Chrome Web Store extensions - a WebView shell cannot run those.\n\n"
            + "DuckDuckGo is the home page and default search because Douglas "
            + "likes it - intertwined as default, not partnership. No DuckDuckGo "
            + "SDK, no account link, no data sharing.\n\n"
            + "Ember cannot watch this browser silently; sharing a page with her "
            + "is Douglas's tap each time, by design.";
        new AlertDialog.Builder(this)
            .setTitle("About Ember Browser")
            .setMessage(msg)
            .setPositiveButton("Close", null)
            .show();
    }

    // ---------- find / share / open out (kept from v2) ----------

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
