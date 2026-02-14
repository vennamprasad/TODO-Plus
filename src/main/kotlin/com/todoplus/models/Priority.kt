package com.todoplus.models

/**
 * Priority levels for TODO items
 */
enum class Priority {
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        /**
         * Parse priority from string (case-insensitive)
         * Returns null if the string doesn't match any priority
         */
        fun parse(value: String?): Priority? {
            if (value == null) return null
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
