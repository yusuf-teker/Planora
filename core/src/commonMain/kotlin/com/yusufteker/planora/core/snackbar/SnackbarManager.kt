package com.yusufteker.planora.core.snackbar

import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * Snackbar (Bilgi/Hata mesajı) türleri.
 */
enum class SnackbarType {
    SUCCESS,
    ERROR,
    INFO
}

/**
 * Gösterilecek mesajın model sınıfı.
 */
data class SnackbarMessage(
    val id: Long = Random.nextLong(),
    val message: String,
    val type: SnackbarType
)

/**
 * Uygulama genelinde uyarı mesajları (Snackbar) göstermek için kullanılan yönetici arayüzü.
 * Bunu [BaseViewModel] içine bağlayacağız ki her sayfadan kolayca "showSnackbar()" çağrılabilsin.
 */
interface SnackbarManager {
    /** Anlık gösterilen mesajları dinlemek için Flow */
    val messages: StateFlow<SnackbarMessage?>

    /**
     * Yeni bir mesaj göstermek için çağrılır.
     * @param message Gösterilecek metin
     * @param type Mesajın türü (SUCCESS, ERROR, INFO)
     */
    fun showMessage(message: String, type: SnackbarType = SnackbarType.INFO)

    /** Mesaj ekrandan kaybolduğunda veya temizlenmesi gerektiğinde çağrılır. */
    fun clearMessage(id: Long)
}
