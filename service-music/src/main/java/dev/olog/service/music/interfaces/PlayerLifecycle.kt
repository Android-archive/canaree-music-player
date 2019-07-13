package dev.olog.service.music.interfaces

import android.support.v4.media.session.PlaybackStateCompat
import dev.olog.service.music.model.MetadataEntity

internal interface PlayerLifecycle {

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    interface Listener {
        fun onPrepare(metadata: MetadataEntity) {}
        fun onMetadataChanged(metadata: MetadataEntity) {}
        fun onStateChanged(state: PlaybackStateCompat) {}
        fun onSeek(where: Long) {}
    }

}