package dev.olog.image.provider.fetcher

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import dev.olog.core.MediaId
import dev.olog.core.MediaIdCategory
import dev.olog.core.entity.LastFmTrack
import dev.olog.core.gateway.ImageRetrieverGateway
import dev.olog.test.shared.MainCoroutineRule
import dev.olog.test.shared.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GlideSongFetcherTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val context = mock<Context>()
    private val songId = 10L
    private val mediaId = MediaId.playableItem(
        MediaId.createCategoryValue(MediaIdCategory.ALBUMS, ""), songId
    )
    private val gateway = mock<ImageRetrieverGateway>()
    private val sut = GlideSongFetcher(context, mediaId, gateway, mock())

    @Test
    fun testExecute() = coroutineRule.runBlocking {
        // given
        val expectedImage = "image"
        val lastFmTrack = LastFmTrack(
            id = songId,
            title = "",
            artist = "",
            album = "",
            image = expectedImage,
            mbid = "",
            albumMbid = "",
            artistMbid = ""
        )
        whenever(gateway.getTrack(songId)).thenReturn(lastFmTrack)

        // when
        val image = sut.execute()

        // then
        verify(gateway).getTrack(songId)
        assertEquals(
            expectedImage,
            image
        )
    }

    @Test
    fun testMustFetchTrue() = coroutineRule.runBlocking {
        // given
        whenever(gateway.mustFetchTrack(songId)).thenReturn(true)

        // when
        val actual = sut.mustFetch()

        // then
        verify(gateway).mustFetchTrack(songId)
        assertEquals(
            true,
            actual
        )
    }

    @Test
    fun testMustFetchFalse() = coroutineRule.runBlocking {
        // given
        whenever(gateway.mustFetchTrack(songId)).thenReturn(false)

        // when
        val actual = sut.mustFetch()

        // then
        verify(gateway).mustFetchTrack(songId)
        assertEquals(
            false,
            actual
        )
    }

}