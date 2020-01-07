package dev.olog.core.interactor.playlist

import dev.olog.core.entity.PlaylistType
import dev.olog.core.gateway.podcast.PodcastPlaylistGateway
import dev.olog.core.gateway.track.PlaylistGateway
import javax.inject.Inject

class RemoveFromPlaylistUseCase @Inject constructor(
    private val playlistGateway: PlaylistGateway,
    private val podcastGateway: PodcastPlaylistGateway

) {

    suspend operator fun invoke(input: Input) {
        return when (input.type) {
            PlaylistType.PODCAST -> podcastGateway.removeFromPlaylist(
                input.playlistId,
                input.idInPlaylist
            )
            PlaylistType.TRACK -> playlistGateway.removeFromPlaylist(
                input.playlistId,
                input.idInPlaylist
            )
            PlaylistType.AUTO -> throw IllegalArgumentException("invalid type ${input.type}")
        }
    }

    class Input(
        val playlistId: Long,
        val idInPlaylist: Long,
        val type: PlaylistType
    )

}