package dev.olog.core.interactor.favorite

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import dev.olog.core.entity.favorite.FavoriteEntity
import dev.olog.core.entity.favorite.FavoriteState
import dev.olog.core.entity.favorite.FavoriteTrackType
import dev.olog.core.gateway.FavoriteGateway
import dev.olog.test.shared.MainCoroutineRule
import dev.olog.test.shared.runBlocking
import org.junit.Rule
import org.junit.Test

class UpdateFavoriteStateUseCaseTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun testInvoke() = coroutineRule.runBlocking {
        // given
        val gateway = mock<FavoriteGateway>()
        val sut = UpdateFavoriteStateUseCase(gateway)
        val entity = FavoriteEntity(1L, FavoriteState.FAVORITE, FavoriteTrackType.TRACK)

        // when
        sut(entity)

        // then
        verify(gateway).updateFavoriteState(entity)
    }

}