Bu aşamada feature geliştirmeye başlama.

Amaç:

Sağlam ve genişlemeye açık temel mimariyi oluşturmak.

Kullanılacak teknolojiler:

- Compose Multiplatform
- Koin
- Navigation 3
- Ktor Client
- SQLDelight
- DataStore
- kotlinx.serialization
- Coroutines
- Flow

Kurulacak modüller:

composeApp

core

feature-auth

feature-home

shared

server

Her feature içerisinde:

presentation

domain

data

paketleri oluştur.

MVI pattern kullanılacak.

Ortak base sınıfları oluştur:

UiState

UiEvent

UiEffect

BaseViewModel

Navigation yapısını kur:

Splash

Onboarding

AuthGraph

HomeGraph

Placeholder ekranlar oluştur:

Splash

Onboarding

Login

Register

Home

Profile

Settings

Dark mode desteğini ekle.

Theme yapısını oluştur.

Henüz backend yazma.

Henüz repository implementasyonu yazma.

Henüz business logic yazma.

Henüz Firebase ekleme.

Henüz AI ekleme.

Amaç:

Sadece production seviyesinde genişlemeye açık temel mimariyi oluşturmak.