package org.purplerat.emberbrowser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import java.io.ByteArrayInputStream
import java.net.URLEncoder

/**
 * Ember Browser — a small, honest WebView browser made by Ember for Douglas.
 *
 * Douglas's spec, implemented:
 *  - "themed to work flawlessly with you": purple two-tone theme, white stripe,
 *    and a start page carrying his lanes (Muse, Gmail, GitHub, OakLeaf).
 *  - "enable all your own features": Mwmbl independent search by default
 *    (Google as the alternate), desktop-view toggle (his desktop-first rule),
 *    two tabs (a working tab and a spare, his two-tab rule), local bookmarks.
 *  - "keep everybody else's crap out of it": the [Blocker] host blocklist,
 *    third-party cookies off by default, and NO analytics/ads/telemetry
 *    libraries anywhere in this project — there is nothing here to phone home.
 *
 * Everything the browser remembers (bookmarks, toggles) lives in this app's
 * private SharedPreferences on the phone. Nothing is synced, sold, or sent.
 */
class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var container: FrameLayout
    private lateinit var tabButtons: List<Button>

    /** Two tabs, his rule: tab 0 is the working tab, tab 1 the spare. */
    private val webViews = arrayOfNulls<WebView>(2)
    private var activeTab = 0

    private val homeUrl = "file:///android_asset/startpage.html"

    private var desktopView: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP, false)
        set(v) = prefs.edit().putBoolean(KEY_DESKTOP, v).apply()

    private var useGoogleSearch: Boolean
        get() = prefs.getBoolean(KEY_GOOGLE_SEARCH, false)
        set(v) = prefs.edit().putBoolean(KEY_GOOGLE_SEARCH, v).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        Blocker.load(this)

        addressBar = findViewById(R.id.address_bar)
        progressBar = findViewById(R.id.progress_bar)
        container = findViewById(R.id.web_container)
        tabButtons = listOf(findViewById(R.id.tab_one), findViewById(R.id.tab_two))

        findViewById<Button>(R.id.btn_go).setOnClickListener { goFromAddressBar() }
        findViewById<Button>(R.id.btn_back).setOnClickListener { activeWebView().goBack() }
        findViewById<Button>(R.id.btn_forward).setOnClickListener { activeWebView().goForward() }
        findViewById<Button>(R.id.btn_reload).setOnClickListener { activeWebView().reload() }
        findViewById<Button>(R.id.btn_home).setOnClickListener { activeWebView().loadUrl(homeUrl) }
        tabButtons.forEachIndexed { i, b -> b.setOnClickListener { switchTab(i) } }

        addressBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { goFromAddressBar(); true } else false
        }

        // Both tabs are built now, so switching is instant and each keeps its own history.
        for (i in webViews.indices) {
            webViews[i] = buildWebView(i)
            container.addView(webViews[i])
        }

        if (savedInstanceState != null) {
            for (i in webViews.indices) {
                savedInstanceState.getBundle("tab_state_$i")?.let { webViews[i]!!.restoreState(it) }
            }
            activeTab = savedInstanceState.getInt("active_tab", 0)
            if (webViews.all { it?.url == null }) loadInitial()
        } else {
            loadInitial()
        }
        switchTab(activeTab)
        applyDesktopMode()
    }

    private fun loadInitial() {
        val incoming = intent?.dataString
        if (incoming != null && (incoming.startsWith("http://") || incoming.startsWith("https://"))) {
            webViews[0]!!.loadUrl(incoming)
        } else {
            webViews[0]!!.loadUrl(homeUrl)
        }
        webViews[1]!!.loadUrl(homeUrl) // the spare tab waits on the start page, ready
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView(index: Int): WebView {
        val wv = WebView(this)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = true
            // Third-party cookies are the usual tracking vector: off. First-party
            // cookies stay on so logins (Gmail, Muse, GitHub) actually work.
            setAcceptThirdPartyCookies(wv, false)
        }
        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url
                // Never block the page the user asked for; block the hangers-on.
                if (!request.isForMainFrame && Blocker.isBlocked(url.host)) {
                    return WebResourceResponse(
                        "text/plain", "utf-8",
                        ByteArrayInputStream(ByteArray(0))
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                // Non-web schemes (tel:, mailto:, intent:) go to the apps that own them.
                if (url.scheme != "http" && url.scheme != "https" && url.scheme != "file") {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                        true
                    } catch (e: Exception) {
                        true
                    }
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                if (index == activeTab && url != null) {
                    addressBar.setText(displayUrl(url))
                    progressBar.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (index == activeTab) {
                    progressBar.visibility = View.GONE
                    if (url != null) addressBar.setText(displayUrl(url))
                }
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (index == activeTab) progressBar.progress = newProgress
            }
        }
        wv.setDownloadListener { url, _, _, _, _ ->
            // Downloads are handed to the system viewer/downloader, never run silently.
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Can't open that download", Toast.LENGTH_SHORT).show()
            }
        }
        wv.visibility = View.GONE
        return wv
    }

    private fun setAcceptThirdPartyCookies(wv: WebView, accept: Boolean) {
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(wv, accept)
    }

    private fun activeWebView(): WebView = webViews[activeTab]!!

    private fun switchTab(i: Int) {
        activeTab = i
        webViews.forEachIndexed { idx, wv -> wv!!.visibility = if (idx == i) View.VISIBLE else View.GONE }
        tabButtons.forEachIndexed { idx, b ->
            b.setBackgroundColor(
                getColor(if (idx == i) R.color.ember_purple else R.color.ember_purple_light)
            )
        }
        val url = activeWebView().url
        addressBar.setText(if (url == null) "" else displayUrl(url))
    }

    private fun displayUrl(url: String): String =
        if (url == homeUrl) "" else url

    private fun goFromAddressBar() {
        val raw = addressBar.text.toString().trim()
        if (raw.isEmpty()) return
        activeWebView().loadUrl(resolveInput(raw))
        hideKeyboard()
    }

    /**
     * An address-bar entry is either a URL or a search. Searches go to Mwmbl —
     * the independent, privately run engine with its own index — unless Douglas
     * has flipped the toggle to Google. Nobody else's search is baked in.
     */
    private fun resolveInput(raw: String): String {
        val looksLikeUrl = !raw.contains(" ") &&
            (raw.contains(".") || raw.startsWith("localhost"))
        if (!looksLikeUrl) {
            val q = URLEncoder.encode(raw, "UTF-8")
            return if (useGoogleSearch) {
                "https://www.google.com/search?q=$q"
            } else {
                "https://mwmbl.org/search?q=$q"
            }
        }
        return if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:")) {
            raw
        } else {
            "https://$raw"
        }
    }

    /** Desktop-first is Douglas's standing rule for Google and everywhere else:
     *  one toggle presents this browser as a desktop browser on both tabs. */
    private fun applyDesktopMode() {
        val ua = if (desktopView) DESKTOP_UA else null // null = WebView's default mobile UA
        webViews.forEach { wv ->
            wv?.settings?.userAgentString = ua
            wv?.settings?.useWideViewPort = desktopView
            wv?.settings?.loadWithOverviewMode = desktopView
        }
    }

    // --- Bookmarks: stored locally only, in this app's private preferences. ---

    private fun bookmarks(): MutableList<String> {
        val raw = prefs.getString(KEY_BOOKMARKS, "") ?: ""
        return if (raw.isEmpty()) mutableListOf() else raw.split("\n").toMutableList()
    }

    private fun addBookmark() {
        val url = activeWebView().url ?: return
        if (url == homeUrl) {
            Toast.makeText(this, "The start page is already home", Toast.LENGTH_SHORT).show()
            return
        }
        val list = bookmarks()
        val entry = "${activeWebView().title ?: url}|$url"
        if (list.any { it.endsWith("|$url") }) {
            Toast.makeText(this, "Already bookmarked", Toast.LENGTH_SHORT).show()
            return
        }
        list.add(entry)
        prefs.edit().putString(KEY_BOOKMARKS, list.joinToString("\n")).apply()
        Toast.makeText(this, "Bookmarked, on this phone only", Toast.LENGTH_SHORT).show()
    }

    private fun showBookmarks() {
        val list = bookmarks()
        if (list.isEmpty()) {
            Toast.makeText(this, "No bookmarks yet — menu → Add bookmark", Toast.LENGTH_LONG).show()
            return
        }
        val titles = list.map { it.substringBefore("|") }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Bookmarks (stored on this phone only)")
            .setItems(titles) { _, which ->
                activeWebView().loadUrl(list[which].substringAfter("|"))
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // --- Menu ---

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.menu_desktop).isChecked = desktopView
        menu.findItem(R.id.menu_search_google).isChecked = useGoogleSearch
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_desktop -> {
                desktopView = !desktopView
                item.isChecked = desktopView
                applyDesktopMode()
                activeWebView().reload()
                Toast.makeText(
                    this,
                    if (desktopView) "Desktop view on" else "Desktop view off",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            R.id.menu_search_google -> {
                useGoogleSearch = !useGoogleSearch
                item.isChecked = useGoogleSearch
                Toast.makeText(
                    this,
                    if (useGoogleSearch) "Search: Google" else "Search: Mwmbl (independent)",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            R.id.menu_add_bookmark -> { addBookmark(); true }
            R.id.menu_bookmarks -> { showBookmarks(); true }
            R.id.menu_home -> { activeWebView().loadUrl(homeUrl); true }
            R.id.menu_about -> {
                AlertDialog.Builder(this)
                    .setTitle("About Ember Browser")
                    .setMessage(
                        "Made by Ember for Douglas Roy Schoonover.\n\n" +
                            "No analytics. No ads. No telemetry. No third-party libraries.\n" +
                            "Bookmarks and settings live on this phone only.\n" +
                            "A bundled blocklist keeps ad and tracker hosts from loading."
                    )
                    .setPositiveButton("Close", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // --- Lifecycle ---

    override fun onBackPressed() {
        val wv = activeWebView()
        if (wv.canGoBack()) {
            wv.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        for (i in webViews.indices) {
            val b = Bundle()
            webViews[i]?.saveState(b)
            outState.putBundle("tab_state_$i", b)
        }
        outState.putInt("active_tab", activeTab)
    }

    override fun onPause() {
        super.onPause()
        webViews.forEach { it?.onPause() }
    }

    override fun onResume() {
        super.onResume()
        webViews.forEach { it?.onResume() }
    }

    override fun onDestroy() {
        webViews.forEach { it?.destroy() }
        super.onDestroy()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(addressBar.windowToken, 0)
        addressBar.clearFocus()
    }

    companion object {
        private const val PREFS = "ember_browser_prefs"
        private const val KEY_DESKTOP = "desktop_view"
        private const val KEY_GOOGLE_SEARCH = "google_search"
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}
