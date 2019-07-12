package dev.olog.service.music.scrobbling

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import dev.olog.core.interactor.ObserveLastFmUserCredentials
import dev.olog.injection.dagger.ServiceLifecycle
import dev.olog.service.music.interfaces.PlayerLifecycle
import dev.olog.service.music.model.MetadataEntity
import dev.olog.shared.extensions.unsubscribe
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class LastFmScrobbling @Inject constructor(
    @ServiceLifecycle lifecycle: Lifecycle,
    observeLastFmUserCredentials: ObserveLastFmUserCredentials,
    playerLifecycle: PlayerLifecycle,
    private val lastFmService: LastFmService

) : DefaultLifecycleObserver, PlayerLifecycle.Listener {

    private val credendialsDisposable = observeLastFmUserCredentials.execute()
            .observeOn(Schedulers.io())
            .filter { it.username.isNotBlank() }
            .subscribe(lastFmService::tryAutenticate, Throwable::printStackTrace)

    init {
        lifecycle.addObserver(this)
        playerLifecycle.addListener(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        credendialsDisposable.unsubscribe()
        lastFmService.dispose()
    }

    override fun onMetadataChanged(metadata: MetadataEntity) {
        lastFmService.scrobble(metadata.entity)
    }

}