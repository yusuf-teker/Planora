package com.yusufteker.planora.core.domain.usecase

import com.yusufteker.planora.core.data.dto.AppVersionConfigDto
import com.yusufteker.planora.core.domain.repository.AppVersionRepository
import com.yusufteker.planora.core.version.AppVersionProvider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeAppVersionProvider(var version: String) : AppVersionProvider {
    override fun getAppVersion(): String = version
}

class FakeAppVersionRepository(var configResult: Result<AppVersionConfigDto>) : AppVersionRepository {
    override suspend fun getAppVersionConfig(): Result<AppVersionConfigDto> = configResult
}

class CheckAppVersionUseCaseTest {

    @Test
    fun testIsVersionLower() {
        val provider = FakeAppVersionProvider("1.0.0")
        val repo = FakeAppVersionRepository(Result.success(AppVersionConfigDto()))
        val useCase = CheckAppVersionUseCase(repo, provider)

        assertTrue(useCase.isVersionLower("1.0.0", "1.1.0"))
        assertTrue(useCase.isVersionLower("1.0.9", "1.1.0"))
        assertTrue(useCase.isVersionLower("1.9.9", "2.0.0"))
        assertFalse(useCase.isVersionLower("1.1.0", "1.1.0"))
        assertFalse(useCase.isVersionLower("2.0.0", "1.9.9"))
    }

    @Test
    fun testForceUpdateRequiredWhenCurrentIsLowerThanMinSupported() = runBlocking {
        val provider = FakeAppVersionProvider("1.0.0")
        val repo = FakeAppVersionRepository(
            Result.success(
                AppVersionConfigDto(
                    minSupportedVersion = "1.2.0",
                    latestVersion = "1.3.0"
                )
            )
        )
        val useCase = CheckAppVersionUseCase(repo, provider)

        val status = useCase()
        assertTrue(status is UpdateStatus.ForceUpdateRequired)
        assertEquals("1.0.0", status.currentVersion)
    }

    @Test
    fun testOptionalUpdateAvailableWhenCurrentIsBetweenMinAndLatest() = runBlocking {
        val provider = FakeAppVersionProvider("1.2.0")
        val repo = FakeAppVersionRepository(
            Result.success(
                AppVersionConfigDto(
                    minSupportedVersion = "1.1.0",
                    latestVersion = "1.3.0"
                )
            )
        )
        val useCase = CheckAppVersionUseCase(repo, provider)

        val status = useCase()
        assertTrue(status is UpdateStatus.OptionalUpdateAvailable)
    }

    @Test
    fun testUpToDateWhenCurrentIsEqualToLatest() = runBlocking {
        val provider = FakeAppVersionProvider("1.3.0")
        val repo = FakeAppVersionRepository(
            Result.success(
                AppVersionConfigDto(
                    minSupportedVersion = "1.1.0",
                    latestVersion = "1.3.0"
                )
            )
        )
        val useCase = CheckAppVersionUseCase(repo, provider)

        val status = useCase()
        assertTrue(status is UpdateStatus.UpToDate)
    }

    @Test
    fun testUpToDateWhenRepositoryFails() = runBlocking {
        val provider = FakeAppVersionProvider("1.0.0")
        val repo = FakeAppVersionRepository(Result.failure(Exception("Network error")))
        val useCase = CheckAppVersionUseCase(repo, provider)

        val status = useCase()
        assertTrue(status is UpdateStatus.UpToDate)
    }
}
