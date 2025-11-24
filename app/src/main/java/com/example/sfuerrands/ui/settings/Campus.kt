package com.example.sfuerrands.ui.settings

enum class Campus {
    Burnaby, Surrey, Vancouver;

    companion object {
        fun fromDisplayName(name: String): Campus =
            values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Burnaby
    }
}