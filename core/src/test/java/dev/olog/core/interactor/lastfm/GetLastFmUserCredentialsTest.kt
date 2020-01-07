package dev.olog.core.interactor.lastfm

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import dev.olog.core.IEncrypter
import dev.olog.core.entity.UserCredentials
import dev.olog.core.prefs.AppPreferencesGateway
import dev.olog.test.shared.MainCoroutineRule
import dev.olog.test.shared.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetLastFmUserCredentialsTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun testInvoke() = coroutineRule.runBlocking {
        // given
        val user = UserCredentials("abc", "123")

        val gateway = mock<AppPreferencesGateway> {
            on { getLastFmCredentials() } doReturn user
        }
        val encrypter = mock<IEncrypter> {
            on { decrypt("abc") } doReturn "user"
            on { decrypt("123") } doReturn "pwd"
        }

        val sut = GetLastFmUserCredentials(gateway, encrypter)

        // when
        val actual = sut()

        // then
        verify(encrypter).decrypt("abc")
        verify(encrypter).decrypt("123")
        verify(gateway).getLastFmCredentials()
        assertEquals(
            UserCredentials("user", "pwd"),
            actual
        )
    }

}