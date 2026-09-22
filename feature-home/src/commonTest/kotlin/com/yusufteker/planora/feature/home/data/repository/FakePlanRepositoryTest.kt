package com.yusufteker.planora.feature.home.data.repository

import app.cash.turbine.test
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 4. KONU: Repository Testleri & Fake vs Mock Deseni
 *
 * NEDEN MOCK (MockK, Mockito) YERİNE FAKE TERCİH EDİLMELİDİR?
 * -----------------------------------------------------------
 * 1. Mocking kırılganlığa yol açar (Fragile Tests):
 *    - MockK ile `every { repo.observeAllTasks() } returns flowOf(...)` yazdığınızda,
 *      repository'nin iç implementasyonunu testin içine taşımış olursunuz.
 *    - Kodun nasıl çalıştığı değiştiğinde (örneğin repository bir parametre daha aldığında
 *      veya farklı bir sıralama yaptığında) tüm mock'lar tek tek patlar.
 *
 * 2. Kotlin Multiplatform (KMP) Uyumluluğu:
 *    - Mockito sadece JVM'de çalışır, KMP commonTest veya iOS (Kotlin/Native) hedeflerinde çalışmaz.
 *    - MockK ise Native'de sınırlıdır ve derleme hatalarına neden olabilir.
 *    - Fake nesneler %100 saf Kotlin'dir; sıfır harici mock kütüphanesi bağımlılığı gerektirir.
 *
 * 3. Gerçekçi Veri Akışı ve Durum Takibi (State Verification):
 *    - Fake InMemory Repository, gerçek bir yerel veritabanı gibi davranır.
 *    - Görev eklendiğinde `observeAllTasks()` StateFlow'u kendiliğinden yeni listeyi yayar.
 *    - Silindiğinde liste kendiliğinden boşalır. Bu sayede testleriniz gerçek dünya senaryolarını
 *      birebir simüle eder.
 */
class FakePlanRepositoryTest {

    private lateinit var repository: FakePlanRepository

    @BeforeTest
    fun setUp() {
        repository = FakePlanRepository()
    }

    private fun createSampleRequest(title: String = "Test Görevi"): CreateTaskRequest {
        return CreateTaskRequest(
            title = title,
            description = "Açıklama",
            startTime = 1000L,
            endTime = 2000L,
            type = TaskType.TASK,
            status = TaskStatus.PENDING,
            visibility = TaskVisibility.PRIVATE
        )
    }

    @Test
    fun `InMemory Fake uzerinde gorev olusturuldugunda observeAllTasks akisi yeni listeyi yaymalidir`() = runTest {
        // Turbine ile repository'nin Flow akışını dinlemeye başlıyoruz:
        repository.observeAllTasks().test {
            // 1. Başlangıçta InMemory depo boştur:
            val initialList = awaitItem()
            assertTrue(initialList.isEmpty())

            // 2. Act: Yeni bir görev oluşturalım
            val createResult = repository.createTask(createSampleRequest("Pulse Mimarisi Çalış"))
            assertTrue(createResult.isSuccess)

            // 3. Assert: Fake depo gerçek bir veritabanı gibi çalıştı ve
            // Flow üzerinden eklenen yeni görevi anında yayınladı!
            val updatedList = awaitItem()
            assertEquals(1, updatedList.size)
            assertEquals("Pulse Mimarisi Çalış", updatedList.first().title)
            assertEquals(TaskStatus.PENDING, updatedList.first().status)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Gorev tamamlandiginda status COMPLETED olmali ve Flow yeni durumu yansitmalidir`() = runTest {
        // Arrange: Bir görev ekle
        val task = repository.createTask(createSampleRequest("Pomodoro Odaklanması")).getOrThrow()

        repository.observeAllTasks().test {
            val currentTasks = awaitItem()
            assertEquals(TaskStatus.PENDING, currentTasks.first().status)

            // Act: Görevi tamamlandı olarak işaretle
            val completeResult = repository.completeTaskInstance(
                taskId = task.id,
                dateMs = 1500L,
                isCompleted = true
            )
            assertTrue(completeResult.isSuccess)

            // Assert: Flow tetiklendi ve görevin durumu COMPLETED oldu
            val updatedTasks = awaitItem()
            assertEquals(TaskStatus.COMPLETED, updatedTasks.first().status)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Gorev silindiginde Flow listeden o gorevi kaldirmali ve liste bosalmalidir`() = runTest {
        val task = repository.createTask(createSampleRequest("Silinecek Görev")).getOrThrow()

        repository.observeAllTasks().test {
            val withTask = awaitItem()
            assertEquals(1, withTask.size)

            // Act: Görevi sil
            val deleteResult = repository.deleteTask(task.id)
            assertTrue(deleteResult.isSuccess)

            // Assert: Silme işlemi sonrası Flow boş listeyi yayınladı
            val emptyList = awaitItem()
            assertTrue(emptyList.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Hata simulasyonu aktif edildiginde repository failure donmeli ve veri degismemelidir`() = runTest {
        // Arrange: Ağ hatası simülasyonunu aç
        repository.shouldFailNetwork = true
        repository.networkErrorMessage = "Sunucuya erişilemiyor (503)"

        // Act: Görev oluşturmayı dene
        val result = repository.createTask(createSampleRequest("Başarısız Görev"))

        // Assert:
        // 1. İşlem başarısız olmalıdır
        assertTrue(result.isFailure)
        assertEquals("Sunucuya erişilemiyor (503)", result.exceptionOrNull()?.message)

        // 2. InMemory veritabanına hiçbir veri eklenmemiş olmalıdır
        repository.observeAllTasks().test {
            val list = awaitItem()
            assertTrue(list.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `seedTasks ile test oncesi hizlica on yukleme yapilabilmelidir`() = runTest {
        // Arrange: Test için başlangıç verisi hazırla
        val seeded = listOf(
            repository.createTask(createSampleRequest("Ön Görev 1")).getOrThrow(),
            repository.createTask(createSampleRequest("Ön Görev 2")).getOrThrow()
        )

        // Act: seedTasks ile depoyu sıfırlayıp sadece bu verileri yükle
        repository.seedTasks(seeded)

        // Assert:
        repository.observeAllTasks().test {
            val tasks = awaitItem()
            assertEquals(2, tasks.size)
            assertEquals("Ön Görev 1", tasks[0].title)
            assertEquals("Ön Görev 2", tasks[1].title)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
