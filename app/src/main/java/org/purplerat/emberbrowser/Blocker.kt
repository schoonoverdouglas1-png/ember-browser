package org.purplerat.emberbrowser

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Ember Browser's "keep everybody else's crap out" layer — Douglas's words, as a feature.
 *
 * A bundled host blocklist (assets/blocklist.txt, one host per line) of advertising and
 * tracking networks. When a page tries to load a sub-resource (script, image, iframe,
 * fetch) from a listed host, the request is answered locally with an empty response
 * and never leaves the phone. The page the user actually asked for (the main frame)
 * is NEVER blocked — only the hangers-on are.
 *
 * The list is a starter list, seeded from tracker hosts actually observed in
 * Douglas's own browsing profile on 2026-11-13, plus the well-known ad networks.
 * It is plain text on purpose: anyone can read it, extend it, or empty it.
 * No list is ever downloaded, updated, or reported on behind the user's back.
 */
object Blocker {

    private var hosts: Set<String> = emptySet()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        hosts = try {
            context.assets.open("blocklist.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .map { it.lowercase() }
                    .toSet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    /** True if [host] is the listed host itself or a subdomain of it. */
    fun isBlocked(host: String?): Boolean {
        if (host == null || hosts.isEmpty()) return false
        val h = host.lowercase()
        if (hosts.contains(h)) return true
        // suffix match: ads.example-tracker.com matches example-tracker.com
        var dot = h.indexOf('.')
        while (dot >= 0 && dot < h.length - 1) {
            if (hosts.contains(h.substring(dot + 1))) return true
            dot = h.indexOf('.', dot + 1)
        }
        return false
    }

    /** An empty 200 response served locally in place of the blocked request. */
    fun emptyResponse(): InputStream = ByteArrayInputStream(ByteArray(0))
}
