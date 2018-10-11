package com.guoqi.magnetplayer.util

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import java.util.regex.Pattern

object MagnetUtils {

    val INFINITY_SYMBOL = "\u221e"
    val MAGNET_PREFIX = "magnet"
    val HTTP_PREFIX = "http"
    val HTTPS_PREFIX = "https"
    val UDP_PREFIX = "udp"
    val INFOHASH_PREFIX = "magnet:?xt=urn:btih:"
    val FILE_PREFIX = "file"
    val CONTENT_PREFIX = "content"
    val TRACKER_URL_PATTERN = "^(https?|udp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    val HASH_PATTERN = "\\b[0-9a-fA-F]{5,40}\\b"
    val MAX_HTTP_REDIRECTION = 10
    val MIME_TORRENT = "application/x-bittorrent"

    /*
     * Returns the link as "(http[s]|udp)://[www.]name.domain/...".
     */

    fun normalizeURL(url: String): String {
        return if (!url.startsWith(HTTP_PREFIX) && !url.startsWith(HTTPS_PREFIX) && !url.startsWith(UDP_PREFIX)) {
            "$HTTP_PREFIX://$url"
        } else {
            url
        }
    }

    /*
     * Returns the link as "magnet:?xt=urn:btih:hash".
     */

    fun normalizeMagnetHash(hash: String): String {
        return INFOHASH_PREFIX + hash
    }

    /**
     * 是否合法的Tracker Url
     *
     * @param url
     * @return
     */
    fun isValidTrackerUrl(url: String?): Boolean {
        if (url == null || TextUtils.isEmpty(url)) {
            return false
        }

        val pattern = Pattern.compile(TRACKER_URL_PATTERN)
        val matcher = pattern.matcher(url.trim { it <= ' ' })

        return matcher.matches()
    }

    /**
     * 是否是标准 hash
     *
     * @param hash
     * @return
     */
    fun isHash(hash: String?): Boolean {
        if (hash == null || TextUtils.isEmpty(hash)) {
            return false
        }

        val pattern = Pattern.compile(HASH_PATTERN)
        val matcher = pattern.matcher(hash.trim { it <= ' ' })

        return matcher.matches()
    }

    /*
     * 从剪切板返回第一个值
     */
    fun getClipboard(context: Context): String? {
        val clipboard = context.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager
                ?: return null

        if (!clipboard.hasPrimaryClip()) {
            return null
        }

        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) {
            return null
        }

        val text = clip.getItemAt(0).text ?: return null

        return text.toString()
    }

}
