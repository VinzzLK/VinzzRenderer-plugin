package com.fcl.plugin.mobileglues

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashLog(context, thread, throwable)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val report = buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("VinzzRenderer Crash Log")
            appendLine("Time   : $time")
            appendLine("Thread : ${thread.name}")
            appendLine("═══════════════════════════════════════")
            appendLine()
            appendLine("Exception:")
            appendLine(sw.toString())
            appendLine()
            appendLine("Cause:")
            var cause = throwable.cause
            while (cause != null) {
                val csw = StringWriter()
                cause.printStackTrace(PrintWriter(csw))
                appendLine(csw.toString())
                cause = cause.cause
            }
        }

        // Tulis ke /sdcard/Android/data/com.vinzz.vinzzrender/crash.txt
        try {
            val extDir = context.getExternalFilesDir(null)
                ?: context.filesDir
            val crashFile = File(extDir, "crash.txt")
            crashFile.appendText(report + "\n\n")
        } catch (_: Exception) {}

        // Juga tulis ke internal cache sebagai fallback
        try {
            val fallbackFile = File(context.cacheDir, "crash.txt")
            fallbackFile.appendText(report + "\n\n")
        } catch (_: Exception) {}
    }

    fun clearLog(context: Context) {
        try {
            val extDir = context.getExternalFilesDir(null) ?: context.filesDir
            File(extDir, "crash.txt").delete()
            File(context.cacheDir, "crash.txt").delete()
        } catch (_: Exception) {}
    }
}
