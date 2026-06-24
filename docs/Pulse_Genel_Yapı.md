# PULSE

Offline-first, Kotlin Multiplatform tabanlı sosyal üretkenlik uygulaması.

Amaç:

Full Kotlin Product Engineer seviyesinde modern bir ürün geliştirmek.

Platformlar:

- Android
- iOS

Backend:

- Ktor

Architecture:

- Clean Architecture
- MVI

Katmanlar:

presentation
domain
data

## Teknolojiler

UI

- Compose Multiplatform

DI

- Koin

Navigation

- Navigation 3

Networking

- Ktor Client

Backend

- Ktor Server

Database

- PostgreSQL

ORM

- Exposed

Migration

- Flyway

Local Database

- SQLDelight

Preferences

- DataStore

Serialization

- kotlinx.serialization

Image Loading

- Coil

Async

- Coroutines
- Flow

Realtime

- Ktor WebSocket

Push

- Firebase FCM

Analytics

- Firebase Analytics

Crash Reporting

- Firebase Crashlytics

Testing

- JUnit
- MockK
- Turbine

Pattern

- MVI

## Modüller

composeApp

core

feature-auth

feature-home

feature-post

feature-comment

feature-profile

feature-search

feature-bookmark

feature-notification

feature-chat

feature-settings

feature-ai

shared

server

## Feature List

Splash

Onboarding

Login

Register

Home Feed

Create Post

Post Detail

Comment

Profile

Edit Profile

Bookmark

Search

Notification

Chat

Settings

AI Assistant

Analytics

## Base Yapısı

Her feature:

presentation

domain

data

içerir.

Her ekran:

State

Event

Effect

ViewModel

Screen

şeklinde tasarlanır.

MVI kullanılır.

## Base Sınıflar

UiState

UiEvent

UiEffect

BaseViewModel

## Navigation

Splash

Onboarding

AuthGraph

HomeGraph

AuthGraph

- Login
- Register

HomeGraph

- Home
- Profile
- Settings

## Kurallar

Bir iş için yalnızca bir kütüphane kullanılmalı.

Kod mümkün olduğunca KMP compatible olmalı.

Dependency Injection için sadece Koin kullanılmalı.

Networking için sadece Ktor Client kullanılmalı.

Room kullanılmayacak.

Retrofit kullanılmayacak.

Hilt kullanılmayacak.

Glide kullanılmayacak.

RxJava kullanılmayacak.

Kod SOLID prensiplerine uygun olmalı.

Feature-based architecture kullanılmalı.

Kod genişlemeye açık olmalı.

Business logic UI'dan bağımsız olmalı.