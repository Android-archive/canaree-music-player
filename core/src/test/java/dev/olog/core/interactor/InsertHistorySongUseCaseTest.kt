package dev.olog.core.interactor

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import dev.olog.core.gateway.podcast.PodcastPlaylistGateway
import dev.olog.core.gateway.track.PlaylistGateway
import dev.olog.test.shared.MainCoroutineRule
import dev.olog.test.shared.runBlocking
import org.junit.Rule
import org.junit.Test

class InsertHistorySongUseCaseTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val gateway = mock<PlaylistGateway>()
    private val podcastGateway = mock<PodcastPlaylistGateway>()
    private val sut = InsertHistorySongUseCase(
        gateway, podcastGateway
    )

    @Test
    fun testInvokeTrack() = coroutineRule.runBlocking {
        // given
        val id = 1L
        val isPodcast = false
        val input = InsertHistorySongUseCase.Input(id, isPodcast)

        // when
        sut(input)

        // then
        verify(gateway).insertSongToHistory(id)
        verifyNoMoreInteractions(gateway)
        verifyZeroInteractions(podcastGateway)
    }

    @Test
    fun testInvokePodcast() = coroutineRule.runBlocking {
        // given
        val id = 1L
        val isPodcast = true
        val input = InsertHistorySongUseCase.Input(id, isPodcast)

        // when
        sut(input)

        // then
        verify(podcastGateway).insertPodcastToHistory(id)
        verifyNoMoreInteractions(podcastGateway)
        verifyZeroInteractions(gateway)
    }

}