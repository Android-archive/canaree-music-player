package dev.olog.data.db

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.olog.core.gateway.track.SongGateway
import dev.olog.data.MocksIntegration
import dev.olog.data.model.db.MostTimesPlayedSongEntity
import dev.olog.data.model.db.PlaylistMostPlayedEntity
import dev.olog.test.shared.MainCoroutineRule
import dev.olog.test.shared.runBlocking
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PlaylistMostPlayedDaoInterationTest {

    @get:Rule
    val coroutinesRule = MainCoroutineRule()

    private lateinit var db: AppDatabase
    private lateinit var sut: PlaylistMostPlayedDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .setQueryExecutor(coroutinesRule.testDispatcher.asExecutor())
            .build()
        sut = db.playlistMostPlayedDao()
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        db.close()
    }

    @Test
    fun testInsertAndQuery() = coroutinesRule.runBlocking {
        // given
        val songId = 10L
        val playlistId = 1L
        assertTrue("should be empty", sut.query(playlistId).first().isEmpty())

        // when
        val item = PlaylistMostPlayedEntity(1, songId, playlistId)
        sut.insert(
            item.copy(id = 1),
            item.copy(id = 2),
            item.copy(id = 3),
            item.copy(id = 4),
            item.copy(id = 5)
        )

        // then
        assertEquals(
            listOf(MostTimesPlayedSongEntity(songId = songId, timesPlayed = 5)),
            sut.query(playlistId).first()
        )
    }

    @Test
    fun testGetAll() = coroutinesRule.runBlocking {
        // given
        val songGateway = mock<SongGateway>()
        whenever(songGateway.getAll()).thenReturn(
            listOf(
                MocksIntegration.song.copy(id = 1),
                MocksIntegration.song.copy(id = 2),
                MocksIntegration.song.copy(id = 3)
            )
        )

        val playlistId = 1L

        val item = PlaylistMostPlayedEntity(1, 10, playlistId)
        sut.insert(
            // in song gateway
            item.copy(id = 1, songId = 1),
            item.copy(id = 2, songId = 1),
            item.copy(id = 3, songId = 1),
            item.copy(id = 4, songId = 1),
            item.copy(id = 5, songId = 1),
            // not enough plays
            item.copy(id = 6, songId = 2),
            item.copy(id = 7, songId = 2),
            item.copy(id = 8, songId = 2),
            item.copy(id = 9, songId = 2),
            // not in song gateway
            item.copy(id = 10, songId = 100)
        )

        // when
        val actual = sut.getAll(playlistId, songGateway).first()

        // then
        val expected = listOf(MocksIntegration.song.copy(id = 1))
        assertEquals(
            expected,
            actual
        )
    }

}