package com.yusufteker.planora.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * =========================================================================================
 * STARTUP & SCROLL BENCHMARK (Öncesi ve Sonrası Karşılaştırma Testi)
 * =========================================================================================
 *
 * 🎯 BU TEST NE İŞE YARAR?
 * -----------------------------------------------------------------------------------------
 * Baseline Profile'ın projemize ne kadar fayda sağladığını bilimsel ve milisaniye bazında
 * kanıtlayan benchmark testidir.
 *
 * İki farklı durumu aynı cihaz üzerinde kıyaslar:
 * 1. startupWithoutBaselineProfile: Baseline Profile OLMADAN (saf JIT derleme) açılış süresi ve kare atlamaları.
 * 2. startupWithBaselineProfile: Baseline Profile İLE (AOT derlenmiş) açılış süresi ve akıcılık.
 *
 * Test bittiğinde terminalde:
 * - timeToInitialDisplayMs (İlk ekrana ulaşma süresi: min, median, max)
 * - frameDurationCpuMs (Kare süreleri ve akıcılık)
 * tablosu basılır.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * 1. SENARYO: Baseline Profile OLMADAN açılış ve kaydırma testi (Ham JIT hali)
     */
    @Test
    fun startupWithoutBaselineProfile() = startup(CompilationMode.None())

    /**
     * 2. SENARYO: Baseline Profile İLE açılış ve kaydırma testi (Önceden derlenmiş hali)
     */
    @Test
    fun startupWithBaselineProfile() = startup(
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require)
    )

    private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = "com.yusufteker.planora",
        metrics = listOf(
            StartupTimingMetric(), // Açılış hızını ölçer (ms cinsinden timeToInitialDisplayMs)
            FrameTimingMetric()    // Kare atlamalarını ve ekran takılmalarını ölçer (jank)
        ),
        compilationMode = compilationMode,
        iterations = 5, // Doğru ve tutarlı bir ortalama için 5 tekrar
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()

        // Kaydırma hareketini de ölçüme dahil et
        val displayWidth = device.displayWidth
        val displayHeight = device.displayHeight

        device.swipe(
            displayWidth / 2,
            (displayHeight * 0.70).toInt(),
            displayWidth / 2,
            (displayHeight * 0.30).toInt(),
            20
        )
        device.waitForIdle()
    }
}
