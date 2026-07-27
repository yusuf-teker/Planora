package com.yusufteker.planora.core.domain.repository

import com.yusufteker.planora.core.data.dto.AppVersionConfigDto

/**
 * Repository for managing application version configuration and remote update metadata.
 */
interface AppVersionRepository {

    /**
     * Retrieves the version configuration from the server or cache.
     *
     * @return [Result] containing [AppVersionConfigDto] on success.
     */
    suspend fun getAppVersionConfig(): Result<AppVersionConfigDto>
}
