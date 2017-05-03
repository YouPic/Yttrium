package com.rimmer.yttrium

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

enum class LogLevel {
    Message,
    Warning,
    Error
}

val logFormat: DateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss")

fun log(level: LogLevel, where: Any?, text: String) {
    val s = StringBuilder()
    s.append("(")
    s.append(logFormat.print(DateTime.now().withZone(DateTimeZone.UTC)))
    s.append(") ")

    if(level != LogLevel.Message) { s.append(level.name); s.append(' ') }
    if(where != null) { s.append("at "); s.append(where.javaClass.simpleName); s.append(" - ") }

    s.append(text)
    println(s)
}

fun logError(text: String) = log(LogLevel.Error, null, text)
fun logWarning(text: String) = log(LogLevel.Warning, null, text)
fun logMessage(text: String) = log(LogLevel.Message, null, text)

fun Any.logError(text: String) = log(LogLevel.Error, this, text)
fun Any.logWarning(text: String) = log(LogLevel.Warning, this, text)
fun Any.logMessage(text: String) = log(LogLevel.Message, this, text)
