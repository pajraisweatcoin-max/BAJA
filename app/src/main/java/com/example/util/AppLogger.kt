package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { INFO, WARNING, ERROR, SUCCESS }

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

object AppLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun d(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARNING, tag, message)
    fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)
    fun s(tag: String, message: String) = log(LogLevel.SUCCESS, tag, message)

    fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = timeFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )
        val current = _logs.value.toMutableList()
        current.add(entry)
        if (current.size > 500) {
            current.removeAt(0)
        }
        _logs.value = current
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
