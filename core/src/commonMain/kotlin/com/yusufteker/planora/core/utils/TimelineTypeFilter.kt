package com.yusufteker.planora.core.utils

/**
 * Ana ekran akışında ve takvimde öğelerin türüne göre filtrelenmesini sağlayan seçenekler.
 *
 * - [ALL]: Hem etkinlikleri hem de görevleri gösterir.
 * - [EVENTS_ONLY]: Yalnızca takvim etkinliklerini gösterir.
 * - [TASKS_ONLY]: Yalnızca yapılacak işleri (görevleri) gösterir.
 */
enum class TimelineTypeFilter {
    ALL,
    EVENTS_ONLY,
    TASKS_ONLY
}
