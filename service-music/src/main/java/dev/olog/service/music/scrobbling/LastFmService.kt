package dev.olog.service.music.scrobbling

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import de.umass.lastfm.Authenticator
import de.umass.lastfm.Caller
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import dev.olog.core.coroutines.DispatcherScope
import dev.olog.core.coroutines.autoDisposeJob
import dev.olog.domain.entity.UserCredentials
import dev.olog.domain.schedulers.Schedulers
import dev.olog.injection.dagger.ServiceLifecycle
import dev.olog.service.music.BuildConfig
import dev.olog.service.music.model.MediaEntity
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import timber.log.Timber
import java.io.File
import java.util.logging.Level
import javax.inject.Inject

internal class LastFmService @Inject constructor(
    @ServiceLifecycle lifecycle: Lifecycle,
    schedulers: Schedulers
): DefaultLifecycleObserver {

    companion object {
        const val SCROBBLE_DELAY = 10L * 1000 // millis
    }

    private var session: Session? = null
    private var userCredentials: UserCredentials? = null

    private val scope by DispatcherScope(schedulers.io)
    private var scrobbleJob by autoDisposeJob()

    init {
        lifecycle.addObserver(this)
        Caller.getInstance().userAgent = "dev.olog.msc"
        Caller.getInstance().logger.level = Level.OFF
    }

    override fun onDestroy(owner: LifecycleOwner) {
        scope.cancel()
    }

    fun tryAuthenticate(credentials: UserCredentials) {
        try {
            session = Authenticator.getMobileSession(
                credentials.username,
                credentials.password,
                BuildConfig.LAST_FM_KEY,
                BuildConfig.LAST_FM_SECRET
            )
            userCredentials = credentials
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    fun dispose(){
        scrobbleJob = null
    }

    fun scrobble(entity: MediaEntity){
        if (session == null || userCredentials == null){
            return
        }

        scrobbleJob = scope.launch {
            delay(SCROBBLE_DELAY)
            val scrobbleData = entity.toScrollData()
            Track.scrobble(scrobbleData, session)
            Track.updateNowPlaying(scrobbleData, session)
        }
    }

    private fun MediaEntity.toScrollData(): ScrobbleData {
        val musicBrainzId = try {
            val audioFile = AudioFileIO.read(File(this.path))
            audioFile.tagAndConvertOrCreateAndSetDefault
            val tag = audioFile.tag
            tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID)
        } catch (ex: Exception){
            Timber.e(ex)
            null
        }

        return ScrobbleData(
            this.artist,
            this.title,
            (System.currentTimeMillis() / 1000).toInt(),
            this.duration.toInt(),
            this.album,
            null,
            musicBrainzId,
            this.trackNumber,
            null,
            true
        )
    }

}