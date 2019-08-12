package dev.olog.offlinelyrics

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.olog.core.entity.OfflineLyrics
import dev.olog.core.gateway.OfflineLyricsGateway
import dev.olog.intents.AppConstants
import dev.olog.offlinelyrics.domain.InsertOfflineLyricsUseCase
import dev.olog.offlinelyrics.domain.ObserveOfflineLyricsUseCase
import dev.olog.shared.android.extensions.dpToPx
import dev.olog.shared.clamp
import dev.olog.shared.indexOfClosest
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

sealed class Lyrics {
    data class Normal(val lyrics: String) : Lyrics()
    data class Synced(val lyrics: List<Pair<Millis, Spannable>>) : Lyrics()
}

private typealias Millis = Long

abstract class BaseOfflineLyricsPresenter constructor(
    private val context: Context,
    private val lyricsGateway: OfflineLyricsGateway,
    private val observeUseCase: ObserveOfflineLyricsUseCase,
    private val insertUseCase: InsertOfflineLyricsUseCase

) {

//    \[\d{2}:\d{2}.\d{2,3}\](.)*
    private val matcher = "\\[\\d{2}:\\d{2}.\\d{2,3}\\](.)*".toRegex()

    private val spannableBuilder = SpannableStringBuilder()

    private var insertLyricsJob: Job? = null
    private val currentTrackIdPublisher = ConflatedBroadcastChannel<Long>()
    private val syncAdjustmentPublisher = ConflatedBroadcastChannel<Long>(0)

    private val lyricsPublisher = ConflatedBroadcastChannel<Lyrics>()

    private var observeLyricsJob: Job? = null
    private var transformLyricsJob: Job? = null
    private var syncJob: Job? = null

    private var originalLyrics = MutableLiveData<CharSequence>()
    private val observedLyrics = MutableLiveData<CharSequence>()

    private var currentStartMillis = -1
    private var currentSpeed = 1f

    fun onStart() {
        observeLyricsJob = GlobalScope.launch(Dispatchers.Default) {
            currentTrackIdPublisher.asFlow()
                .switchMap { id -> observeUseCase(id) }
                .flowOn(Dispatchers.IO)
                .collect { onNextLyrics(it) }
        }
        transformLyricsJob = GlobalScope.launch {
            lyricsPublisher.asFlow()
                .switchMap {
                    when (it) {
                        is Lyrics.Normal -> {
                            flowOf(it.lyrics)
                        }
                        is Lyrics.Synced -> {
                            handleSyncedLyrics(it)
                        }
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect {
                    withContext(Dispatchers.Main) {
                        observedLyrics.value = it
                    }
                }
        }
        syncJob = GlobalScope.launch {
            currentTrackIdPublisher.asFlow()
                .switchMap { lyricsGateway.observeSyncAdjustment(it) }
                .collect { syncAdjustmentPublisher.offer(it) }
        }
    }

    fun onStop() {
        observeLyricsJob?.cancel()
        transformLyricsJob?.cancel()
        syncJob?.cancel()
    }

    fun onStateChanged(position: Int, speed: Float) {
        currentStartMillis = position
        currentSpeed = speed
    }

    fun observeLyrics(): LiveData<CharSequence> = observedLyrics

    private suspend fun onNextLyrics(lyrics: String) {
        withContext(Dispatchers.Main) {
            originalLyrics.value = lyrics
        }
        // add a newline to ensure that last word is matched correctly
        val sanitizedString = lyrics.trim() + "\n"
        val matches = matcher.findAll(sanitizedString)
            .map { it.value.trim() }
            .toList()

        if (matches.isEmpty()) {
            // not synced
            lyricsPublisher.offer(Lyrics.Normal(lyrics))
        } else {
            // synced lyrics
            val result = matches.map {

                val minutes = TimeUnit.MINUTES.toMillis(
                    it[1].toString().toLong() * 10L +
                            it[2].toString().toLong()
                )
                val seconds = TimeUnit.SECONDS.toMillis(
                    it[4].toString().toLong() * 10L +
                            it[5].toString().toLong()
                )
                val millis = it[7].toString().toLong() * 100L + it[8].toString().toLong() * 10
                val time = minutes + seconds + millis

                val textOnly = it.substring(10)

                time to SpannableStringBuilder().apply {
                    append(textOnly)
                    defaultSpan(this, 0, textOnly.length)
                }
            }
            lyricsPublisher.offer(Lyrics.Synced(result))
        }
    }

    private fun handleSyncedLyrics(syncedLyrics: Lyrics.Synced): Flow<Spannable> {
        spannableBuilder.clear()
        val words = mutableListOf<Pair<Int, Int>>()
        for (lyric in syncedLyrics.lyrics) {
            words.add(spannableBuilder.length to spannableBuilder.length + lyric.second.length)
            spannableBuilder.append(lyric.second)
            spannableBuilder.appendln()
        }

        var lastClosest = -1

        val interval = clamp(AppConstants.PROGRESS_BAR_INTERVAL, 250, Long.MAX_VALUE)

        return flow {
            var tick = 0
            emit(tick)
            while (true) {
                delay(interval)
                if (currentSpeed == 0f) {
                    tick = 0
                } else {
                    emit(++tick)
                }
            }
        }.map {
            val current = currentStartMillis + syncAdjustmentPublisher.value + // static
                    (it + 1L) * interval * currentSpeed // dynamic
            val closest = indexOfClosest(current.toLong(), syncedLyrics.lyrics.map { it.first })

            if (closest == -1) {
                // do nothing
                return@map spannableBuilder
            }

            if (lastClosest == closest) {
                // same
                return@map spannableBuilder
            }

            if (lastClosest != -1) {
                // set span to default
                val (from, to) = words[lastClosest]
                defaultSpan(spannableBuilder, from, to)
            }
            val (from, to) = words[closest]
            currentSpan(spannableBuilder, from, to)

            lastClosest = closest
            spannableBuilder
        }
    }

    fun updateCurrentTrackId(trackId: Long) {
        currentTrackIdPublisher.offer(trackId)
    }

    fun getLyrics(): String {
        return originalLyrics.value.toString()
    }

    fun updateSyncAdjustment(value: Long) {
        GlobalScope.launch {
            lyricsGateway.setSyncAdjustment(currentTrackIdPublisher.value, value)
        }
    }

    suspend fun getSyncAdjustment(): String = withContext(Dispatchers.IO) {
        "${lyricsGateway.getSyncAdjustment(currentTrackIdPublisher.value)}"
    }

    fun updateLyrics(lyrics: String) {
        if (currentTrackIdPublisher.valueOrNull == null) {
            return
        }
        insertLyricsJob?.cancel()
        insertLyricsJob = GlobalScope.launch {
            insertUseCase(OfflineLyrics(currentTrackIdPublisher.value, lyrics))
        }
    }

    private fun defaultSpan(builder: SpannableStringBuilder, from: Int, to: Int) {
        builder.setSpan(
            ForegroundColorSpan(0xFF_757575.toInt()),
            from,
            to,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE
        )
        builder.setSpan(
            AbsoluteSizeSpan(context.dpToPx(25f)),
            from,
            to,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun currentSpan(builder: SpannableStringBuilder, from: Int, to: Int) {
        builder.setSpan(
            ForegroundColorSpan(Color.WHITE),
            from,
            to,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE
        )
        builder.setSpan(
            AbsoluteSizeSpan(context.dpToPx(30f)),
            from,
            to,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.setSpan(
            StyleSpan(Typeface.BOLD),
            from,
            to,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

}