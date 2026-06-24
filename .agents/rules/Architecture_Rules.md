# ARCHITECTURE RULES

Bu proje uzun ömürlü ve ölçeklenebilir olacak şekilde tasarlanmıştır.

Bu kurallar bozulmamalıdır.

---

# General Principles

Kod okunabilir olmalı.

Basit çözümler tercih edilmeli.

SOLID prensipleri uygulanmalı.

Feature-based architecture kullanılmalı.

Business logic UI katmanından bağımsız olmalı.

Her feature bağımsız geliştirilebilir olmalı.

Kod KMP uyumlu olacak şekilde yazılmalı.

Bir problem için yalnızca bir kütüphane kullanılmalı.

---

# Architecture

Clean Architecture kullanılacak.

Katmanlar:

presentation

domain

data

Domain katmanı diğer katmanları bilmemeli.

Presentation sadece domain ile konuşmalı.

Data katmanı repository implementasyonlarını içermeli.

---

# MVI

Her ekran aşağıdaki yapıya sahip olmalı:

State

Event

Effect

ViewModel

Screen

State immutable olmalı.

StateFlow kullanılmalı.

ViewModel state'i expose etmeli.

UI business logic içermemeli.

---

# Dependency Injection

Sadece Koin kullanılacak.

Service locator kullanılmayacak.

Singleton kullanımı minimumda tutulmalı.

---

# Navigation

Navigation 3 kullanılacak.

Her feature kendi graph'ına sahip olmalı.

Navigation kodları merkezi tutulmalı.

---

# Networking

Sadece Ktor Client kullanılacak.

Retrofit kullanılmayacak.

Network modelleri domain modellerinden ayrı tutulmalı.

DTO -> Domain dönüşümü mapper ile yapılmalı.

---

# Database

Remote database:

PostgreSQL

Local database:

SQLDelight

Room kullanılmayacak.

Repository cache katmanını yönetecek.

Offline-first yaklaşımı uygulanacak.

---

# Serialization

kotlinx.serialization kullanılacak.

---

# Concurrency

Coroutines kullanılacak.

Flow kullanılacak.

RxJava kullanılmayacak.

GlobalScope kullanılmayacak.

---

# Error Handling

Result pattern kullanılmalı.

Exception UI katmanına taşınmamalı.

Repository hata dönüşümlerinden sorumlu olmalı.

---

# UI

Compose Multiplatform kullanılacak.

Composable'lar mümkün olduğunca stateless olmalı.

UI içerisinde network çağrısı yapılmamalı.

---

# ViewModel

BaseViewModel kullanılacak.

StateFlow kullanılacak.

SharedFlow effect için kullanılacak.

---

# Dependency Direction

presentation

↓

domain

↓

data

Ters bağımlılık oluşturulmamalı.

---

# Feature Structure

feature-name

presentation

domain

data

Her feature kendi içerisinde bağımsız olmalı.

---

# Naming

Repository interface

UserRepository

Repository implementation

UserRepositoryImpl

UseCase

GetUserUseCase

State

HomeState

Event

HomeEvent

Effect

HomeEffect

ViewModel

HomeViewModel

Screen

HomeScreen

---

# Future Compatibility

Yeni feature eklemek mevcut yapıyı bozmamalı.

AI

Chat

Notification

Analytics

Payment

modülleri sonradan eklenebilir olmalı.

Kod production seviyesinde tutulmalı.