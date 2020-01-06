package dev.olog.core.entity.track

import dev.olog.core.MediaId
import dev.olog.core.MediaIdCategory
import dev.olog.core.Mocks
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumTest {

    @Test
    fun testTrackGetMediaId() {
        val id = 1L
        val album = Mocks.album.copy(id = id)
        assertEquals(
            MediaId.createCategoryValue(MediaIdCategory.ALBUMS, id.toString()),
            album.getMediaId()
        )
    }

    @Test
    fun testPodcastGetMediaId() {
        val id = 1L
        val album = Mocks.podcastAlbum.copy(id = id)
        assertEquals(
            MediaId.createCategoryValue(MediaIdCategory.PODCASTS_ALBUMS, id.toString()),
            album.getMediaId()
        )
    }

    @Test
    fun testTrackGetArtistMediaId() {
        val id = 1L
        val album = Mocks.album.copy(artistId = id)
        assertEquals(
            MediaId.createCategoryValue(MediaIdCategory.ARTISTS, id.toString()),
            album.getArtistMediaId()
        )
    }

    @Test
    fun testPodcastGetArtistMediaId() {
        val id = 1L
        val album = Mocks.podcastAlbum.copy(artistId = id)
        assertEquals(
            MediaId.createCategoryValue(MediaIdCategory.PODCASTS_ARTISTS, id.toString()),
            album.getArtistMediaId()
        )
    }

}