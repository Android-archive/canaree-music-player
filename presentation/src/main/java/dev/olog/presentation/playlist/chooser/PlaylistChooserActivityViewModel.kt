package dev.olog.presentation.playlist.chooser

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import dev.olog.domain.entity.track.Playlist
import dev.olog.domain.gateway.track.PlaylistGateway
import dev.olog.domain.schedulers.Schedulers
import dev.olog.presentation.R
import dev.olog.presentation.model.DisplayableAlbum
import dev.olog.presentation.model.DisplayableItem
import dev.olog.presentation.presentationId
import dev.olog.core.ApplicationContext
import dev.olog.core.mapListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class PlaylistChooserActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    playlistGateway: PlaylistGateway,
    schedulers: Schedulers
) : ViewModel() {

    val data: Flow<List<DisplayableItem>> = playlistGateway.observeAll()
        .mapListItem { it.toDisplayableItem(context.resources) }
        .flowOn(schedulers.io)

    private fun Playlist.toDisplayableItem(resources: Resources): DisplayableItem {
        return DisplayableAlbum(
            type = R.layout.item_playlist_chooser,
            mediaId = presentationId,
            title = title,
            subtitle = DisplayableAlbum.readableSongCount(resources, size)
        )
    }

}