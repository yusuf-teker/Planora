package com.yusufteker.planora.core.data.repository

import com.yusufteker.planora.core.data.api.AppVersionApi
import com.yusufteker.planora.core.data.dto.AppVersionConfigDto
import com.yusufteker.planora.core.domain.repository.AppVersionRepository

/**
 * Implementation of [AppVersionRepository] using [AppVersionApi].
 *
 * @property api Remote [AppVersionApi] instance.
 */
class AppVersionRepositoryImpl(
    private val api: AppVersionApi
) : AppVersionRepository {

    override suspend fun getAppVersionConfig(): Result<AppVersionConfigDto> {
        return api.fetchAppVersionConfig()
    }
}
