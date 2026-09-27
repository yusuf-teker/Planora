package com.yusufteker.planora.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.planora.shared.api.AdminUserSummaryDto
import com.yusufteker.planora.shared.api.TaskDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: AdminViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    if (!uiState.isAuthenticated) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            AdminLoginScreen(
                uiState = uiState,
                onServerUrlChange = viewModel::onServerUrlChange,
                onAdminSecretChange = viewModel::onAdminSecretChange,
                onIdentifierChange = viewModel::onLoginIdentifierChange,
                onPasswordChange = viewModel::onLoginPasswordChange,
                onModeChange = viewModel::onLoginModeChange,
                onConnectSecret = viewModel::connectWithSecret,
                onLoginCredentials = viewModel::loginWithCredentials,
                modifier = Modifier.padding(padding)
            )
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Planora Admin",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "SUPERUSER",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (uiState.currentTab == AdminTab.DASHBOARD) viewModel.loadDashboardStats()
                        else if (uiState.currentTab == AdminTab.USERS) viewModel.searchUsers()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Çıkış Yap")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.currentTab == AdminTab.DASHBOARD,
                    onClick = { viewModel.setTab(AdminTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Genel Bakış") }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == AdminTab.USERS,
                    onClick = { viewModel.setTab(AdminTab.USERS) },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("Kullanıcılar") }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == AdminTab.BROADCAST,
                    onClick = { viewModel.setTab(AdminTab.BROADCAST) },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                    label = { Text("Toplu Bildirim") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.currentTab) {
                AdminTab.DASHBOARD -> DashboardTabContent(viewModel, uiState)
                AdminTab.USERS -> UsersTabContent(viewModel, uiState)
                AdminTab.BROADCAST -> BroadcastTabContent(viewModel, uiState)
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // Seçili kullanıcının görevleri diyalogu
    if (uiState.selectedUser != null) {
        UserTasksDialog(
            user = uiState.selectedUser!!,
            tasks = uiState.selectedUserTasks,
            isLoading = uiState.isLoadingTasks,
            onDismiss = { viewModel.selectUser(null) }
        )
    }

    // Özel Push Bildirimi Gönderme Diyalogu
    if (uiState.pushDialogUser != null) {
        SendPushDialog(
            user = uiState.pushDialogUser!!,
            title = uiState.pushTitle,
            body = uiState.pushBody,
            isSending = uiState.isSendingPush,
            onTitleChange = viewModel::onPushTitleChanged,
            onBodyChange = viewModel::onPushBodyChanged,
            onSend = viewModel::sendPushToUser,
            onDismiss = viewModel::closePushDialog
        )
    }

    // Kullanıcı Kalıcı Silme Onay Diyalogu
    if (uiState.userToDelete != null) {
        ConfirmDeleteUserDialog(
            user = uiState.userToDelete!!,
            isDeleting = uiState.isDeletingUser,
            onConfirm = viewModel::confirmDeleteUser,
            onDismiss = viewModel::closeDeleteDialog
        )
    }
}

@Composable
fun AdminLoginScreen(
    uiState: AdminUiState,
    onServerUrlChange: (String) -> Unit,
    onAdminSecretChange: (String) -> Unit,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onModeChange: (AdminLoginMode) -> Unit,
    onConnectSecret: () -> Unit,
    onLoginCredentials: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Planora Admin Portal",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Yönetim paneline bağlanmak için sunucu ve kimlik bilgilerini doğrulayın",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // 1. Sunucu URL'i
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Hedef Sunucu Adresi",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.serverUrl,
                    onValueChange = onServerUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Base URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { onServerUrlChange("http://10.0.2.2:8080") },
                        label = { Text("Emülatör", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { onServerUrlChange("http://localhost:8080") },
                        label = { Text("Localhost", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { onServerUrlChange("https://pulse-7b4z.onrender.com") },
                        label = { Text("Render", fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Giriş Modu Sekmeleri
        TabRow(
            selectedTabIndex = if (uiState.loginMode == AdminLoginMode.SECRET_KEY) 0 else 1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = uiState.loginMode == AdminLoginMode.SECRET_KEY,
                onClick = { onModeChange(AdminLoginMode.SECRET_KEY) },
                text = { Text("🔑 Secret Key", fontSize = 13.sp) }
            )
            Tab(
                selected = uiState.loginMode == AdminLoginMode.CREDENTIALS,
                onClick = { onModeChange(AdminLoginMode.CREDENTIALS) },
                text = { Text("👤 Admin Hesabı", fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.loginMode == AdminLoginMode.SECRET_KEY) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Admin Gizli Anahtarı (X-Admin-Secret)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Henüz veritabanında hesabınız ADMIN olmasa bile bu anahtarla anında bağlanabilir ve kendinizi tek tıkla ADMIN yapabilirsiniz.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    OutlinedTextField(
                        value = uiState.adminSecretInput,
                        onValueChange = onAdminSecretChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Admin Secret Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onConnectSecret,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Doğrula ve Bağlan")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Admin Kullanıcı Girişi",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = uiState.loginIdentifierInput,
                        onValueChange = onIdentifierChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Kullanıcı Adı veya E-posta") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = uiState.loginPasswordInput,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Şifre") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onLoginCredentials,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Giriş Yap")
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTabContent(viewModel: AdminViewModel, uiState: AdminUiState) {
    val stats = uiState.stats
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Sistem Metrikleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (stats != null) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Toplam Kullanıcı",
                        value = "${stats.totalUsers}",
                        icon = Icons.Default.Group,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Premium Üye",
                        value = "${stats.totalPremiumUsers}",
                        icon = Icons.Default.Star,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Toplam Görev",
                        value = "${stats.totalTasks}",
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Plan Odaları",
                        value = "${stats.totalRooms}",
                        icon = Icons.Default.MeetingRoom,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Sosyal Postlar",
                        value = "${stats.totalPosts}",
                        icon = Icons.Default.Forum,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "FCM Cihazları",
                        value = "${stats.totalFcmDevices}",
                        icon = Icons.Default.NotificationsActive,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("İstatistik verileri yükleniyor veya boş.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadDashboardStats() }) {
                            Text("Tekrar Dene")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UsersTabContent(viewModel: AdminViewModel, uiState: AdminUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Kullanıcı adı, isim veya e-posta ara...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.usersList, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    onViewTasks = { viewModel.selectUser(user) },
                    onToggleRole = { viewModel.toggleRole(user) },
                    onTogglePremium = {
                        viewModel.setPremium(user, isPremium = !user.isPremium, days = if (!user.isPremium) 30 else null)
                    },
                    onSendPush = { viewModel.openPushDialog(user) },
                    onDeleteUser = { viewModel.openDeleteDialog(user) }
                )
            }
        }
    }
}

@Composable
private fun UserCard(
    user: AdminUserSummaryDto,
    onViewTasks: () -> Unit,
    onToggleRole: () -> Unit,
    onTogglePremium: () -> Unit,
    onSendPush: () -> Unit,
    onDeleteUser: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${user.username} • ${user.email}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Role Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (user.role == "ADMIN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = user.role,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (user.role == "ADMIN") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Premium Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (user.isPremium) Color(0xFFFFD700) else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (user.isPremium) "PRO" else "FREE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (user.isPremium) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // İstatistik satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Görev: ${user.taskCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Oda: ${user.roomCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Cihaz: ${user.fcmDeviceCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

            // Aksiyon Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewTasks,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("Görevler", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onToggleRole,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(if (user.role == "ADMIN") "USER Yap" else "ADMIN Yap", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onTogglePremium,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(if (user.isPremium) "PRO Al" else "PRO Ver", fontSize = 11.sp)
                }

                FilledTonalIconButton(
                    onClick = onSendPush,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Bildirim Gönder", modifier = Modifier.size(16.dp))
                }

                FilledTonalIconButton(
                    onClick = onDeleteUser,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Kullanıcıyı Sil", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BroadcastTabContent(viewModel: AdminViewModel, uiState: AdminUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Buradan göndereceğiniz bildirim, sistemde aktif FCM cihazı olan TÜM kullanıcılara anında ulaştırılır.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        OutlinedTextField(
            value = uiState.broadcastTitle,
            onValueChange = viewModel::onBroadcastTitleChanged,
            label = { Text("Duyuru Başlığı") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = uiState.broadcastBody,
            onValueChange = viewModel::onBroadcastBodyChanged,
            label = { Text("Duyuru Metni") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            maxLines = 6,
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = viewModel::sendBroadcast,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSendingBroadcast,
            shape = RoundedCornerShape(10.dp)
        ) {
            if (uiState.isSendingBroadcast) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gönderiliyor...")
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tüm Kullanıcılara Gönder")
            }
        }
    }
}

@Composable
private fun UserTasksDialog(
    user: AdminUserSummaryDto,
    tasks: List<TaskDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        },
        title = {
            Text("${user.name} (@${user.username}) Görevleri (${tasks.size})")
        },
        text = {
            Box(modifier = Modifier.sizeIn(maxHeight = 400.dp, minWidth = 280.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (tasks.isEmpty()) {
                    Text("Bu kullanıcıya ait sunucuda kayıtlı görev bulunmuyor.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tasks, key = { it.id }) { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (!task.description.isNullOrBlank()) {
                                        Text(task.description!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Durum: ${task.status}", fontSize = 11.sp)
                                        Text("Tür: ${task.type}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SendPushDialog(
    user: AdminUserSummaryDto,
    title: String,
    body: String,
    isSending: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onSend, enabled = !isSending) {
                if (isSending) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Gönder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSending) { Text("İptal") }
        },
        title = {
            Text("Bildirim Gönder: @${user.username}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Başlık") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = { Text("Mesaj") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )
            }
        }
    )
}

@Composable
private fun ConfirmDeleteUserDialog(
    user: AdminUserSummaryDto,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onError)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Kalıcı Olarak Sil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("İptal") }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kullanıcıyı Sil: @${user.username}", fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${user.name} kullanıcısını kalıcı olarak silmek istediğinize emin misiniz?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "• Kullanıcının tüm görevleri, etkinlikleri ve kişisel notları silinecek.\n" +
                            "• Dahil olduğu grup odalarından ve ortak task/eventlerden çıkarılacak.\n" +
                            "• Sosyal postları, yorumları ve profil verileri tamamen temizlenecek.\n\n" +
                            "⚠️ Bu işlem geri alınamaz!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

