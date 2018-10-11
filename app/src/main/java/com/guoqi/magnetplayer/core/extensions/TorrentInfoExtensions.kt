package com.guoqi.magnetplayer.core.extensions

import com.frostwire.jlibtorrent.TorrentInfo


/**
 * Get the largest file index of the [TorrentInfo].
 */
internal fun TorrentInfo.getLargestFileIndex(): Int = files().getLargestFileIndex()
