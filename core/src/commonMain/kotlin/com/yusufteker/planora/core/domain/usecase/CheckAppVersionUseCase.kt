package com.yusufteker.planora.core.domain.usecase

import com.yusufteker.planora.core.data.dto.AppVersionConfigDto
import com.yusufteker.planora.core.domain.repository.AppVersionRepository
import com.yusufteker.planora.core.version.AppVersionProvider

/**
 * Sealed status representing application version update evaluation result.
 */
sealed interface UpdateStatus {
    /**
     * Application is up to date or version check failed.
     */
    data object UpToDate : UpdateStatus

    /**
     * An optional non-blocking update is available.
     *
     * @property config Remote version configuration.
     * @property currentVersion Current installed app version string.
     */
    data class OptionalUpdateAvailable(
        val config: AppVersionConfigDto,
        val currentVersion: String
    ) : UpdateStatus

    /**
     * Mandatory update required. App usage is blocked until updated.
     *
     * @property config Remote version configuration.
     * @property currentVersion Current installed app version string.
     */
    data class ForceUpdateRequired(
        val config: AppVersionConfigDto,
        val currentVersion: String
    ) : UpdateStatus
}

/**
 * UseCase that compares the local application version against minimum supported and latest available versions.
 *
 * @property repository [AppVersionRepository] for retrieving remote version config.
 * @property versionProvider [AppVersionProvider] for obtaining current local app version.
 */
class CheckAppVersionUseCase(
    private val repository: AppVersionRepository,
    private val versionProvider: AppVersionProvider
) {

    /**
     * Executes the version check evaluation.
     *
     * @return [UpdateStatus] indicating whether force update, optional update, or no update is needed.
     */
    suspend operator fun invoke(): UpdateStatus {
        val currentVersion = versionProvider.getAppVersion()
        val result = repository.getAppVersionConfig()

        val config = result.getOrNull() ?: return UpdateStatus.UpToDate

        return when {
            isVersionLower(currentVersion, config.minSupportedVersion) -> {
                UpdateStatus.ForceUpdateRequired(config = config, currentVersion = currentVersion)
            }
            isVersionLower(currentVersion, config.latestVersion) -> {
                UpdateStatus.OptionalUpdateAvailable(config = config, currentVersion = currentVersion)
            }
            else -> {
                UpdateStatus.UpToDate
            }
        }
    }

    /**
     * Compares two semantic version strings (e.g., "1.0.2" vs "1.1.0").
     *
     * @param current Target version to check (e.g. local version).
     * @param target Required or reference version.
     * @return `true` if current version is strictly less than target version.
     */
    fun isVersionLower(current: String, target: String): Boolean {
        val currentParts = current.split("-").first().split(".").mapNotNull { it.toIntOrNull() }
        val targetParts = target.split("-").first().split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(currentParts.size, targetParts.size)
        for (i in 0 until maxLength) {
            val curr = currentParts.getOrElse(i) { 0 }
            val tgt = targetParts.getOrElse(i) { 0 }
            if (curr < tgt) return true
            if (curr > tgt) return false
        }
        return false
    }
}
