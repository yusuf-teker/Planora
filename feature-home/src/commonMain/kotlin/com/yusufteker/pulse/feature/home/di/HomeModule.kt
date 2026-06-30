package com.yusufteker.pulse.feature.home.di

import com.yusufteker.pulse.feature.home.presentation.home.HomeViewModel
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileViewModel
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsViewModel
import com.yusufteker.pulse.feature.home.presentation.social.SocialViewModel
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import com.yusufteker.pulse.feature.home.data.api.CommentApi
import com.yusufteker.pulse.feature.home.domain.repository.CommentRepository
import com.yusufteker.pulse.feature.home.data.repository.ProfileRepositoryImpl
import com.yusufteker.pulse.feature.home.data.api.FeedApi
import com.yusufteker.pulse.feature.home.domain.repository.FeedRepository
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

import com.yusufteker.pulse.feature.home.domain.sync.PostSyncManager
import com.yusufteker.pulse.feature.home.domain.sync.DefaultPostSyncManager
import com.yusufteker.pulse.feature.home.domain.repository.PostRepository
import com.yusufteker.pulse.feature.home.presentation.create_post.CreatePostViewModel

import com.yusufteker.pulse.feature.home.presentation.pending_posts.PendingPostsViewModel
import com.yusufteker.pulse.feature.home.presentation.search.SearchUsersViewModel
import com.yusufteker.pulse.feature.home.data.api.PlanApi
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.feature.home.data.repository.PlanRepositoryImpl

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
    single { FeedRepository(get(), get(), get()) }
    
    // Sync & Post
    single<PostSyncManager> { DefaultPostSyncManager(get(), get(), get()) }
    single { PostRepository(get(), get(), get()) }
    
    // Plan Room & Task
    single { PlanApi(get()) }
    single<PlanRepository> { PlanRepositoryImpl(get(), get()) }
    
    viewModelOf(::HomeViewModel)
    viewModelOf(::SocialViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
    factory { params -> CreatePostViewModel(params.getOrNull(), get()) }
    viewModelOf(::PendingPostsViewModel)
    viewModelOf(::SearchUsersViewModel)
}
