package com.yusufteker.planora.feature.home.di

import com.yusufteker.planora.feature.home.presentation.home.HomeViewModel
import com.yusufteker.planora.feature.home.presentation.profile.ProfileViewModel
import com.yusufteker.planora.feature.home.presentation.settings.SettingsViewModel
import com.yusufteker.planora.feature.home.presentation.social.SocialViewModel
import com.yusufteker.planora.feature.home.presentation.aichat.AiChatViewModel
import com.yusufteker.planora.feature.home.presentation.focus.FocusViewModel
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.feature.home.data.api.CommentApi
import com.yusufteker.planora.feature.home.domain.repository.CommentRepository
import com.yusufteker.planora.feature.home.data.repository.ProfileRepositoryImpl
import com.yusufteker.planora.feature.home.data.api.FeedApi
import com.yusufteker.planora.feature.home.domain.repository.FeedRepository
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

import com.yusufteker.planora.feature.home.domain.sync.PostSyncManager
import com.yusufteker.planora.feature.home.domain.sync.DefaultPostSyncManager
import com.yusufteker.planora.feature.home.domain.repository.PostRepository
import com.yusufteker.planora.feature.home.presentation.create_post.CreatePostViewModel

import com.yusufteker.planora.feature.home.presentation.pending_posts.PendingPostsViewModel
import com.yusufteker.planora.feature.home.presentation.search.SearchUsersViewModel
import com.yusufteker.planora.feature.home.data.api.PlanApi
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.data.repository.PlanRepositoryImpl
import com.yusufteker.planora.feature.home.presentation.event_detail.EventDetailViewModel
import com.yusufteker.planora.feature.home.presentation.event_editor.EventEditorViewModel
import com.yusufteker.planora.feature.home.presentation.follow_list.FollowListViewModel
import com.yusufteker.planora.feature.home.presentation.note_editor.NoteEditorViewModel
import com.yusufteker.planora.feature.home.presentation.plan_rooms.PlanRoomsViewModel

import com.yusufteker.planora.feature.home.presentation.plan_room_detail.PlanRoomDetailViewModel

import com.yusufteker.planora.feature.home.presentation.notes.NotesViewModel
import com.yusufteker.planora.feature.home.presentation.task_editor.TaskEditorViewModel
import com.yusufteker.planora.feature.home.presentation.task_detail.TaskDetailViewModel

/**
 * (Dependency Injection - DI) ayarlarının yapıldığı yerdir.
 * Uygulamanın farklı parçalarının (ViewModel, Repository, API) birbirini nasıl 
 * bulacağını ve oluşturulacağını burada tanımlıyoruz.
 */
val homeModule = module {
    // "single" anahtar kelimesi, nesnenin Singleton (Tekil) olacağını belirtir.
    // Yani uygulama boyunca bu nesneden sadece bir tane oluşturulur ve her yere o verilir.
    // "get()" fonksiyonu ise, o nesnenin ihtiyaç duyduğu diğer bağımlılıkları (Örn: HttpClient) otomatik bulur.
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    single { FeedApi(get()) }
    single { CommentApi(get()) }
    single { CommentRepository(get()) }
    single<com.yusufteker.planora.feature.home.domain.repository.FeedPagingSourceFactory> { 
        com.yusufteker.planora.feature.home.domain.repository.DefaultFeedPagingSourceFactory(get()) 
    }
    single { FeedRepository(get(), get(), get(), get()) }
    
    // Sync & Post
    single<PostSyncManager> { DefaultPostSyncManager(get(), get(), get()) }
    single { PostRepository(get(), get(), get()) }
    
    // Plan Room & Task
    single { PlanApi(get()) }
    single { com.yusufteker.planora.feature.home.data.api.CalendarApi(get()) }
    single<PlanRepository> { PlanRepositoryImpl(get(), get(), get(), get(), get()) }
    
    // Use Cases
    factory { com.yusufteker.planora.feature.home.domain.use_case.GetFilteredTasksUseCase() }
    factory { com.yusufteker.planora.feature.home.domain.use_case.SubmitSmartInputUseCase(get(), get()) }
    factory { com.yusufteker.planora.feature.home.domain.use_case.ProcessDeepLinkUseCase(get()) }
    
    viewModelOf(::HomeViewModel)
    viewModelOf(::FocusViewModel)
    viewModelOf(::SocialViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { AiChatViewModel(get(), get(), get(), get(), get()) }
    factory { params -> CreatePostViewModel(params.getOrNull(), get(), get()) }
    viewModelOf(::PendingPostsViewModel)
    viewModelOf(::SearchUsersViewModel)
    viewModelOf(::FollowListViewModel)
    viewModelOf(::PlanRoomsViewModel)
    viewModelOf(::PlanRoomDetailViewModel)
    viewModelOf(::NotesViewModel)
    viewModel { params -> 
        TaskEditorViewModel(
            planRepository = get(),
            profileRepository = get(),
            sessionPreferences = get()
        )
    }
    viewModel { params ->
        TaskDetailViewModel(
            planRepository = get(),
            profileRepository = get(),
            sessionPreferences = get()
        )
    }
    viewModel { params ->
        val (noteId: String?, planRoomId: String?, parentId: String?) = params
        
        NoteEditorViewModel(
            noteId = noteId,
            planRoomId = planRoomId,
            parentId = parentId,
            planRepository = get(),
            sessionPreferences = get(),
            cloudAiManager = get()
        )
    }
    viewModel { params ->
        EventDetailViewModel(
            planRepository = get(),
            profileRepository = get(),
            sessionPreferences = get()
        )
    }
    viewModel { params ->
        EventEditorViewModel(
            planRepository = get(),
            profileRepository = get(),
            sessionPreferences = get()
        )
    }
}
