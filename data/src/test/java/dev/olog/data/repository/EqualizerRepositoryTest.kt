package dev.olog.data.repository

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import dev.olog.core.entity.EqualizerPreset
import dev.olog.core.gateway.EqualizerGateway
import dev.olog.core.prefs.EqualizerPreferencesGateway
import dev.olog.data.db.EqualizerPresetsDao
import dev.olog.data.model.db.EqualizerPresetEntity
import dev.olog.data.test.asSchedulers
import dev.olog.test.shared.MainCoroutineRule
import dev.olog.test.shared.runBlocking
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class EqualizerRepositoryTest {

    private val presetsEntities = listOf(
        EqualizerPresetEntity(
            1,
            "eq 1",
            emptyList(),
            false
        ),
        EqualizerPresetEntity(
            2,
            "eq 2",
            emptyList(),
            true
        )
    )

    private val domainPresets = listOf(
        EqualizerPreset(1, "eq 1", emptyList(), false),
        EqualizerPreset(2, "eq 2", emptyList(), true)
    )

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    val dao = mock<EqualizerPresetsDao>()
    val prefs = mock<EqualizerPreferencesGateway>()
    private lateinit var sut: EqualizerGateway

    @Before
    fun setup() = coroutineRule.runBlocking {
        whenever(dao.getPresets()).thenReturn(presetsEntities)

        sut = EqualizerRepository(dao, prefs, coroutineRule.testDispatcher.asSchedulers())
    }

    @Test
    fun verifySetup() = coroutineRule.runBlocking {
        whenever(dao.getPresets()).thenReturn(emptyList())
        sut = EqualizerRepository(dao, prefs, coroutineRule.testDispatcher.asSchedulers())

        verify(dao).insertPresets(any())
    }

    @Test
    fun testGetPresets() {
        // given
        whenever(dao.getPresets()).thenReturn(presetsEntities)

        // when
        val actual = sut.getPresets()

        // then
        assertEquals(
            domainPresets,
            actual
        )
    }

    @Test
    fun testGetCurrentPreset() {
        // given
        val presetId = 1L
        whenever(prefs.getCurrentPresetId()).thenReturn(presetId)
        whenever(dao.getPresetById(presetId)).thenReturn(presetsEntities[0])

        // when
        val actual = sut.getCurrentPreset()

        // then
        assertEquals(
            domainPresets[0],
            actual
        )
    }

    @Test
    fun testAddPresetSuccess() = coroutineRule.runBlocking {
        // given
        val preset = EqualizerPreset(-1, "new name", emptyList(), true)

        // when
        sut.addPreset(preset)

        // then
        val expected = EqualizerPresetEntity(
            3,
            "new name",
            emptyList(),
            true
        )
        verify(dao).insertPreset(expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAddPresetShouldFailIllegalId() = coroutineRule.runBlocking {
        // given
        val preset = EqualizerPreset(1, "", emptyList(), true)

        // when
        sut.addPreset(preset)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAddPresetShouldFailNotCustom() = coroutineRule.runBlocking {
        // given
        val preset = EqualizerPreset(-1, "", emptyList(), false)

        // when
        sut.addPreset(preset)
    }

    @Test
    fun testUpdatePreset() = coroutineRule.runBlocking {
        // given
        val item = domainPresets[0]

        // when
        sut.updatePreset(item)

        // then
        verify(dao).insertPreset(presetsEntities[0])
    }

    @Test
    fun testDeletePreset() = coroutineRule.runBlocking {
        // given
        val item = domainPresets[0]

        // when
        sut.deletePreset(item)

        // then
        verify(dao).deletePreset(presetsEntities[0])
    }

    @Test
    fun testObserveCurrentPreset() = coroutineRule.runBlocking {
        // given
        val id = 1L
        whenever(prefs.observeCurrentPresetId()).thenReturn(flowOf(id))
        whenever(dao.observePresetById(id)).thenReturn(flowOf(presetsEntities[0]))

        // when
        val actual = sut.observeCurrentPreset().single()

        // then
        assertEquals(
            domainPresets[0],
            actual
        )
    }


}