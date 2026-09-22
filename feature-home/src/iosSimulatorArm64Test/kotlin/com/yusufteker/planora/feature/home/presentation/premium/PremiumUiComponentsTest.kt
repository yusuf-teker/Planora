package com.yusufteker.planora.feature.home.presentation.premium

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 9. KONU: Compose Multiplatform UI Testleri (iOS Simulator Native Runner)
 *
 * Premium ve Paywall Ekranı Arayüz Durum Testleri:
 * 1. LOADING STATE UI TESTİ: Ödeme sürerken butonun kilitlenmesi (disabled) ve CircularProgressIndicator gösterimi.
 * 2. IDLE STATE UI TESTİ: Normal durumda butonun tıklanabilir olması ve eylem metninin görünmesi.
 * 3. PREMIUM ACTIVE STATE UI TESTİ: Kullanıcı zaten Premium iken yeşil onay rozeti ve aktif durum metninin sunulması.
 * 4. INTERACTION TESTİ: Kullanıcı butona tıkladığında callback fonksiyonunun tetiklenmesi.
 */
@OptIn(ExperimentalTestApi::class)
class PremiumUiComponentsTest {

    @Composable
    private fun MockPaymentButton(
        isProcessing: Boolean,
        actionText: String = "Satın Alımı Tamamla",
        onConfirm: () -> Unit
    ) {
        MaterialTheme {
            Button(
                onClick = onConfirm,
                enabled = !isProcessing,
                modifier = Modifier.testTag("payment_cta_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("payment_progress_indicator")
                    )
                } else {
                    Text(text = actionText, modifier = Modifier.testTag("payment_cta_text"))
                }
            }
        }
    }

    @Composable
    private fun MockPremiumStatusBadge(
        isPremium: Boolean,
        activeText: String = "Planora Premium Aktif",
        upgradeText: String = "Premium'a Yükselt"
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.testTag("premium_status_container")) {
                if (isPremium) {
                    Row(modifier = Modifier.testTag("premium_active_row")) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active Check",
                            modifier = Modifier.testTag("premium_active_icon")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = activeText, modifier = Modifier.testTag("premium_active_text"))
                    }
                } else {
                    Text(text = upgradeText, modifier = Modifier.testTag("premium_upgrade_text"))
                }
            }
        }
    }

    // ========================================================================
    // 1. LOADING VE PROGRESS INDICATOR UI TESTİ
    // ========================================================================

    @Test
    fun `odeme islenirken buton kilitli olmali ve loading progress indicator gorunmelidir`() = runComposeUiTest {
        setContent {
            MockPaymentButton(
                isProcessing = true,
                onConfirm = {}
            )
        }

        // Assert 1: Buton devre dışı (disabled / not enabled) olmalıdır (çift tıklama önleme)
        onNodeWithTag("payment_cta_button")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        // Assert 2: Loading animasyon bileşeni ekranda görünür olmalıdır
        onNodeWithTag("payment_progress_indicator")
            .assertIsDisplayed()
    }

    // ========================================================================
    // 2. IDLE VE ETKİLEŞİM UI TESTİ
    // ========================================================================

    @Test
    fun `bosta iken buton tiklanabilir olmali ve tiklandiginda aksiyonu tetiklemelidir`() = runComposeUiTest {
        var wasClicked = false

        setContent {
            MockPaymentButton(
                isProcessing = false,
                actionText = "Şimdi Abone Ol",
                onConfirm = { wasClicked = true }
            )
        }

        // Assert 1: Buton etkin ve eylem metni yazıyor
        onNodeWithTag("payment_cta_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()

        onNodeWithText("Şimdi Abone Ol")
            .assertIsDisplayed()

        // Act: Kullanıcı butona tıklar
        onNodeWithTag("payment_cta_button").performClick()

        // Assert 2: Lambda callback başarıyla çalıştı
        assertTrue(wasClicked)
    }

    // ========================================================================
    // 3. PREMIUM ACTIVE DURUMU UI TESTİ
    // ========================================================================

    @Test
    fun `kullanici premium oldugunda onay rozeti ve aktiflik metni gosterilmelidir`() = runComposeUiTest {
        setContent {
            MockPremiumStatusBadge(isPremium = true)
        }

        // Assert: Onay ikonu ve aktif metin görünür
        onNodeWithTag("premium_active_icon")
            .assertIsDisplayed()

        onNodeWithText("Planora Premium Aktif")
            .assertIsDisplayed()
    }

    @Test
    fun `kullanici ucretsiz planda iken yukseltme secenegi gosterilmelidir`() = runComposeUiTest {
        setContent {
            MockPremiumStatusBadge(isPremium = false)
        }

        // Assert: Yükseltme seçeneği görünür
        onNodeWithText("Premium'a Yükselt")
            .assertIsDisplayed()
    }
}
