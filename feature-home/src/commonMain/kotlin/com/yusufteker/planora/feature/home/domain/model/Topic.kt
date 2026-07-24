package com.yusufteker.planora.feature.home.domain.model

enum class Topic(val id: String, val displayName: String) {
    GENERAL("GENERAL", "Genel"),
    TECHNOLOGY("TECHNOLOGY", "Teknoloji"),
    SOFTWARE("SOFTWARE", "Yazılım"),
    DESIGN("DESIGN", "Tasarım"),
    NEWS("NEWS", "Gündem"),
    SPORTS("SPORTS", "Spor"),
    ENTERTAINMENT("ENTERTAINMENT", "Eğlence"),
    FINANCE("FINANCE", "Finans"),
    HEALTH("HEALTH", "Sağlık"),
    ART("ART", "Sanat"),
    SCIENCE("SCIENCE", "Bilim"),
    TRAVEL("TRAVEL", "Seyahat"),
    FOOD("FOOD", "Yemek"),
    MUSIC("MUSIC", "Müzik"),
    GAMING("GAMING", "Oyun");

    companion object {
        fun fromId(id: String): Topic = entries.find { it.id == id } ?: GENERAL
    }
}
