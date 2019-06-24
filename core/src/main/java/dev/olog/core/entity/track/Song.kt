package dev.olog.core.entity.track

import dev.olog.core.MediaId
import dev.olog.core.MediaIdCategory
import java.io.File

data class Song(
    val id: Long,
    val artistId: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val duration: Long,
    val dateAdded: Long,
    val path: String,
    val folder: String,
    val trackColumn: Int,
    val idInPlaylist: Int,
    val isPodcast: Boolean

) {


    val discNumber: Int
        get() {
            if (trackColumn >= 1000){
                return trackColumn / 1000
            }
            return 0
        }
    val trackNumber: Int
        get() {
            if (trackColumn >= 1000){
                return trackColumn % 1000
            }
            return trackColumn
        }

    val folderPath: String
        get() = path.substring(0, path.lastIndexOf(File.separator))

    val hasAlbumNameAsFolder: Boolean
        get() = album == folderPath

}

fun Song.getMediaId(): MediaId {
    val category = if (isPodcast) MediaIdCategory.PODCASTS else MediaIdCategory.SONGS
    val mediaId = MediaId.createCategoryValue(category, "")
    return MediaId.playableItem(mediaId, id)
}

fun Song.getAlbumMediaId(): MediaId {
    val category = if (isPodcast) MediaIdCategory.PODCASTS_ALBUMS else MediaIdCategory.ALBUMS
    return MediaId.createCategoryValue(category, this.albumId.toString())
}

fun Song.getArtistMediaId(): MediaId {
    val category = if (isPodcast) MediaIdCategory.PODCASTS_ARTISTS else MediaIdCategory.ARTISTS
    return MediaId.createCategoryValue(category, this.artistId.toString())
}