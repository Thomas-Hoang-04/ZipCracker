package com.thomas.zipcracker.metadata

enum class AppState {
    NOT_INITIATED,
    VALIDATING,
    RUNNING,
    COMPLETED,
    OPTIONAL_DECOMPRESSING,
    CANCELLED,
}