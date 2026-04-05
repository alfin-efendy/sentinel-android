package com.github.alfin_efendy.sentinel.domain.usecase

import com.github.alfin_efendy.sentinel.data.repository.AppConfigRepository
import com.github.alfin_efendy.sentinel.domain.model.AppConfig
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SaveConfigUseCaseTest {

    private val repository: AppConfigRepository = mockk(relaxed = true)
    private lateinit var useCase: SaveConfigUseCase

    @Before
    fun setUp() {
        useCase = SaveConfigUseCase(repository)
    }

    @Test
    fun `blank packageName returns Error`() = runTest {
        val result = useCase(AppConfig(packageName = ""))
        assertTrue(result is SaveConfigUseCase.Result.Error)
    }

    @Test
    fun `whitespace packageName returns Error`() = runTest {
        val result = useCase(AppConfig(packageName = "   "))
        assertTrue(result is SaveConfigUseCase.Result.Error)
    }

    @Test
    fun `valid packageName with no deepLink returns Success`() = runTest {
        val result = useCase(AppConfig(packageName = "com.roblox.client"))
        assertEquals(SaveConfigUseCase.Result.Success, result)
    }

    @Test
    fun `valid packageName with valid https deepLink returns Success`() = runTest {
        val result = useCase(AppConfig(packageName = "com.example", deepLinkUrl = "https://example.com/path"))
        assertEquals(SaveConfigUseCase.Result.Success, result)
    }

    @Test
    fun `valid packageName with valid custom scheme deepLink returns Success`() = runTest {
        val result = useCase(AppConfig(packageName = "com.roblox.client", deepLinkUrl = "roblox://placeId=123"))
        assertEquals(SaveConfigUseCase.Result.Success, result)
    }

    @Test
    fun `deepLink with no scheme returns Error`() = runTest {
        val result = useCase(AppConfig(packageName = "com.example", deepLinkUrl = "example.com/no-scheme"))
        assertTrue(result is SaveConfigUseCase.Result.Error)
        val error = result as SaveConfigUseCase.Result.Error
        assertTrue(error.message.contains("scheme", ignoreCase = true))
    }

    @Test
    fun `on success repository save is called`() = runTest {
        val config = AppConfig(packageName = "com.example")
        useCase(config)
        coVerify { repository.save(config) }
    }

    @Test
    fun `on blank packageName repository save is not called`() = runTest {
        useCase(AppConfig(packageName = ""))
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `on invalid deepLink repository save is not called`() = runTest {
        useCase(AppConfig(packageName = "com.example", deepLinkUrl = "no-scheme-here"))
        coVerify(exactly = 0) { repository.save(any()) }
    }
}
