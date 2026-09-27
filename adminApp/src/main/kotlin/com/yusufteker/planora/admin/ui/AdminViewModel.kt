package com.yusufteker.planora.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.admin.data.AdminApiClient
import com.yusufteker.planora.shared.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminTab {
    DASHBOARD,
    USERS,
    BROADCAST
}

enum class AdminLoginMode {
    SECRET_KEY,
    CREDENTIALS
}

data class AdminUiState(
    val isAuthenticated: Boolean = false,
    val currentTab: AdminTab = AdminTab.DASHBOARD,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Login State
    val serverUrl: String = "https://pulse-7b4z.onrender.com",
    val adminSecretInput: String = "",
    val loginIdentifierInput: String = "",
    val loginPasswordInput: String = "",
    val loginMode: AdminLoginMode = AdminLoginMode.CREDENTIALS,

    // Dashboard
    val stats: AdminDashboardStatsDto? = null,

    // Users
    val searchQuery: String = "",
    val usersList: List<AdminUserSummaryDto> = emptyList(),
    val selectedUser: AdminUserSummaryDto? = null,

    // Tasks for selected user
    val selectedUserTasks: List<TaskDto> = emptyList(),
    val isLoadingTasks: Boolean = false,

    // Push dialog
    val pushDialogUser: AdminUserSummaryDto? = null,
    val pushTitle: String = "",
    val pushBody: String = "",
    val isSendingPush: Boolean = false,

    // Broadcast
    val broadcastTitle: String = "",
    val broadcastBody: String = "",
    val isSendingBroadcast: Boolean = false
)

class AdminViewModel(
    val apiClient: AdminApiClient = AdminApiClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun onServerUrlChange(url: String) = _uiState.update { it.copy(serverUrl = url) }
    fun onAdminSecretChange(secret: String) = _uiState.update { it.copy(adminSecretInput = secret) }
    fun onLoginIdentifierChange(v: String) = _uiState.update { it.copy(loginIdentifierInput = v) }
    fun onLoginPasswordChange(v: String) = _uiState.update { it.copy(loginPasswordInput = v) }
    fun onLoginModeChange(mode: AdminLoginMode) = _uiState.update { it.copy(loginMode = mode) }

    fun connectWithSecret() {
        val url = _uiState.value.serverUrl.trim().trimEnd('/')
        val secret = _uiState.value.adminSecretInput.trim()
        if (url.isBlank() || secret.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Sunucu URL ve Admin Secret zorunludur.") }
            return
        }

        apiClient.baseUrl = url
        apiClient.adminSecretKey = secret
        apiClient.adminToken = null

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = apiClient.getStats()
            result.onSuccess { stats ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        stats = stats,
                        successMessage = "Admin paneline başarıyla bağlanıldı!"
                    )
                }
                searchUsers()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Bağlantı hatası: ${err.message}. Sunucu adresini veya sunucunun çalıştığını kontrol edin."
                    )
                }
            }
        }
    }

    fun loginWithCredentials() {
        val url = _uiState.value.serverUrl.trim().trimEnd('/')
        val id = _uiState.value.loginIdentifierInput.trim()
        val pass = _uiState.value.loginPasswordInput.trim()

        if (url.isBlank() || id.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Kullanıcı adı/e-posta ve şifre zorunludur.") }
            return
        }

        apiClient.baseUrl = url

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val loginResult = apiClient.login(id, pass)
            loginResult.onSuccess { auth ->
                if (auth.role != "ADMIN") {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "'${auth.username}' hesabınız henüz ADMIN rolüne sahip değil. Lütfen Admin Secret Key sekmesinden giriş yapıp kendinizi ADMIN yapın."
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        successMessage = "Hoş geldiniz, Admin ${auth.name}!"
                    )
                }
                loadDashboardStats()
                searchUsers()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Giriş başarısız: ${err.message}")
                }
            }
        }
    }

    fun logout() {
        apiClient.adminToken = null
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                stats = null,
                usersList = emptyList(),
                selectedUser = null
            )
        }
    }

    fun setTab(tab: AdminTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == AdminTab.DASHBOARD && _uiState.value.stats == null) {
            loadDashboardStats()
        } else if (tab == AdminTab.USERS && _uiState.value.usersList.isEmpty()) {
            searchUsers()
        }
    }

    fun loadDashboardStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = apiClient.getStats()
            result.onSuccess { stats ->
                _uiState.update { it.copy(isLoading = false, stats = stats) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "İstatistikler yüklenemedi") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchUsers(query)
    }

    fun searchUsers(query: String? = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = apiClient.searchUsers(query)
            result.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, usersList = list) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Kullanıcılar getirilemedi") }
            }
        }
    }

    fun selectUser(user: AdminUserSummaryDto?) {
        _uiState.update { it.copy(selectedUser = user, selectedUserTasks = emptyList()) }
        if (user != null) {
            loadUserTasks(user.id)
        }
    }

    private fun loadUserTasks(userId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTasks = true) }
            val result = apiClient.getUserTasks(userId)
            result.onSuccess { response ->
                _uiState.update { it.copy(isLoadingTasks = false, selectedUserTasks = response.tasks) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoadingTasks = false, errorMessage = "Görevler yüklenemedi: ${err.message}") }
            }
        }
    }

    fun toggleRole(user: AdminUserSummaryDto) {
        val newRole = if (user.role == "ADMIN") "USER" else "ADMIN"
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = apiClient.updateUserRole(user.id, newRole)
            result.onSuccess { updated ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        successMessage = "${updated.username} kullanıcısının rolü '$newRole' yapıldı.",
                        usersList = state.usersList.map { if (it.id == updated.id) updated else it },
                        selectedUser = if (state.selectedUser?.id == updated.id) updated else state.selectedUser
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Rol güncellenemedi: ${err.message}") }
            }
        }
    }

    fun setPremium(user: AdminUserSummaryDto, isPremium: Boolean, days: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = apiClient.setPremium(user.id, isPremium, days)
            result.onSuccess { res ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        successMessage = res.message,
                        usersList = state.usersList.map {
                            if (it.id == user.id) it.copy(isPremium = res.isPremium, premiumUntil = res.premiumUntil) else it
                        },
                        selectedUser = if (state.selectedUser?.id == user.id) {
                            state.selectedUser.copy(isPremium = res.isPremium, premiumUntil = res.premiumUntil)
                        } else state.selectedUser
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Premium güncellenemedi: ${err.message}") }
            }
        }
    }

    fun openPushDialog(user: AdminUserSummaryDto) {
        _uiState.update { it.copy(pushDialogUser = user, pushTitle = "", pushBody = "") }
    }

    fun closePushDialog() {
        _uiState.update { it.copy(pushDialogUser = null, pushTitle = "", pushBody = "") }
    }

    fun onPushTitleChanged(v: String) = _uiState.update { it.copy(pushTitle = v) }
    fun onPushBodyChanged(v: String) = _uiState.update { it.copy(pushBody = v) }

    fun sendPushToUser() {
        val user = _uiState.value.pushDialogUser ?: return
        val title = _uiState.value.pushTitle.trim()
        val body = _uiState.value.pushBody.trim()
        if (title.isBlank() || body.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Başlık ve mesaj zorunludur.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingPush = true) }
            val result = apiClient.sendPushToUser(user.id, title, body)
            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        isSendingPush = false,
                        pushDialogUser = null,
                        successMessage = res.message
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isSendingPush = false, errorMessage = err.message ?: "Bildirim iletilemedi") }
            }
        }
    }

    fun onBroadcastTitleChanged(v: String) = _uiState.update { it.copy(broadcastTitle = v) }
    fun onBroadcastBodyChanged(v: String) = _uiState.update { it.copy(broadcastBody = v) }

    fun sendBroadcast() {
        val title = _uiState.value.broadcastTitle.trim()
        val body = _uiState.value.broadcastBody.trim()
        if (title.isBlank() || body.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Duyuru başlığı ve metni zorunludur.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingBroadcast = true) }
            val result = apiClient.sendBroadcastPush(title, body)
            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        isSendingBroadcast = false,
                        broadcastTitle = "",
                        broadcastBody = "",
                        successMessage = res.message
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isSendingBroadcast = false, errorMessage = err.message ?: "Toplu bildirim başarısız") }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
