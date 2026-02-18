package com.todoplus.models

/**
 * Priority levels for TODO items
 */
data class Priority(val name: String) : Comparable<Priority> {
    
    companion object {
        val HIGH = Priority("HIGH")
        val MEDIUM = Priority("MEDIUM")
        val LOW = Priority("LOW")

        /**
         * Parse priority from string (case-insensitive)
         */
        fun parse(value: String?): Priority? {
            if (value == null) return null
            return Priority(value.uppercase())
        }
    }

    override fun compareTo(other: Priority): Int {
        // We can't implement meaningful comparison here without access to settings
        // This will be handled by the UI sorter which has access to Project/Settings
        return this.name.compareTo(other.name)
    }
    
    override fun toString(): String = name
}
