package org.apache.logging.log4j

import me.anno.Time
import me.anno.io.Streams.writeString
import me.anno.io.VoidOutputStream
import me.anno.io.config.ConfigBasics
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.utils.OSFeatures
import me.anno.utils.types.Strings.indexOf2
import org.apache.commons.logging.Log
import java.io.IOException
import java.io.OutputStream
import java.util.Calendar
import java.util.Locale
import java.util.logging.Level

open class LoggerImpl(val name: String) : Logger, Log {

    private fun interleaveImpl(msg: String, args: Array<out Any?>): String {
        val builder = StringBuilder(msg.length)
        var i = 0
        var j = 0
        while (i < msg.length) {
            val li = i
            i = msg.indexOf("{}", i)
            if (i < 0 || j >= args.size) {
                builder.append(msg, li, msg.length)
                break
            } else {
                builder.append(msg, li, i)
                builder.append(args[j++])
                i += 2 // skip over {}
            }
        }
        return builder.toString()
    }

    private fun interleave(msg: String, args: Array<out Any?>): String {
        if (args.isEmpty()) return msg
        return if (msg.contains("{}")) {
            interleaveImpl(msg, args)
        } else if ('%' in msg) {
            msg.format(Locale.ENGLISH, *args)
        } else msg
    }

    private fun interleave(msg: String, arg0: Any?): String {
        val index = msg.indexOf("{}")
        return if (index >= 0) {
            val arg = arg0.toString()
            val builder = StringBuilder(msg.length + arg.length - 2)
            builder.append(msg, 0, index)
            builder.append(arg)
            builder.append(msg, index + 2, msg.length)
            builder.toString()
        } else msg
    }

    private fun printRaw(prefix: String, line2: String) {
        if (prefix == "ERR!" || prefix == "WARN") {
            System.err.println(line2)
        } else {
            println(line2)
        }
    }

    private fun printLine(prefix: String, time: CharSequence, line: String, logFile: OutputStream?) {
        val line2 = "[$time,$prefix:$name] $line"
        printRaw(prefix, line2)
        if (logFile != null) {
            try {
                logFile.writeString(line2)
                logFile.writeString(System.lineSeparator())
            } catch (_: IOException) {
            }
        }
    }

    private fun print(prefix: String, msg: String) {
        // should not be synchronized!
        val logFile = getLogFileStream()
        val time = getTimeStamp()
        // printing should be synchronized to not mix messages
        synchronized(LogManager) {
            var i = 0
            while (i < msg.length) {
                val ni = msg.indexOf2('\n', i)
                printLine(prefix, time, msg.substring(i, ni), logFile)
                i = ni + 1
            }
            try {
                logFile?.flush()
            } catch (_: IOException) {
            }
        }
    }

    override fun info(msg: String) {
        if (isInfoEnabled()) {
            print("INFO", msg)
        }
    }

    override fun info(msg: String, vararg obj: Any?) {
        if (isInfoEnabled()) {
            info(interleave(msg, obj))
        }
    }

    override fun info(marker: Marker, msg: String) {
        info(msg)
    }

    override fun info(msg: String, thrown: Throwable) {
        if (isInfoEnabled()) {
            info(msg)
            thrown.printStackTrace()
        }
    }

    override fun debug(msg: String) {
        if (isDebugEnabled()) {
            print("DEBUG", msg)
        }
    }

    override fun debug(msg: String, e: Throwable) {
        if (isDebugEnabled()) {
            print("DEBUG", msg)
            e.printStackTrace()
        }
    }

    override fun debug(o: Any?) {
        if (isDebugEnabled()) {
            debug(o.toString())
        }
    }

    override fun debug(o: Any?, throwable: Throwable?) {
        if (isDebugEnabled()) {
            if (throwable == null) debug(o)
            else debug(o.toString(), throwable)
        }
    }

    open fun debug(msg: String, obj: Any?) {
        if (isDebugEnabled()) {
            debug(interleave(msg, obj))
        }
    }

    override fun debug(msg: String, vararg obj: Any?) {
        if (isDebugEnabled()) {
            debug(interleave(msg, obj))
        }
    }

    override fun error(msg: String) {
        if (isErrorEnabled()) {
            print("ERR!", msg)
        }
    }

    override fun error(msg: String, vararg obj: Any?) {
        if (isErrorEnabled()) {
            error(interleave(msg, obj))
        }
    }

    override fun error(msg: String, thrown: Throwable) {
        if (isErrorEnabled()) {
            error(msg)
            thrown.printStackTrace()
        }
    }

    override fun error(o: Any?) {
        if (isErrorEnabled()) {
            error(o.toString())
        }
    }

    override fun error(o: Any?, throwable: Throwable?) {
        if (isErrorEnabled()) {
            if (throwable == null) error(o)
            else error(o.toString(), throwable)
        }
    }

    override fun severe(msg: String) {
        if (isSevereEnabled()) {
            print("SEVERE", msg)
        }
    }

    override fun severe(msg: String, vararg obj: Any?) {
        if (isSevereEnabled()) {
            error(interleave(msg, obj))
        }
    }

    override fun severe(msg: String, thrown: Throwable) {
        if (isSevereEnabled()) {
            error(msg)
            thrown.printStackTrace()
        }
    }

    override fun fatal(msg: String) {
        if (isFatalEnabled()) {
            print("FATAL", msg)
        }
    }

    override fun fatal(msg: String, vararg obj: Any?) {
        if (isFatalEnabled()) {
            fatal(interleave(msg, obj))
        }
    }

    override fun fatal(msg: String, thrown: Throwable) {
        if (isFatalEnabled()) {
            fatal(msg)
            thrown.printStackTrace()
        }
    }

    override fun warn(msg: String) {
        if (isWarnEnabled() && shouldWarnAgain(msg)) {
            print("WARN", msg)
        }
    }

    private fun shouldWarnAgain(msg: String): Boolean {
        val lastWarned = lastWarned
        synchronized(lastWarned) {
            val time = Time.nanoTime
            val lastWarnedI = lastWarned[msg]
            if (lastWarnedI == null || (lastWarnedI - time) > WARNING_TIMEOUT_NANOS) {
                lastWarned[msg] = time
                return true
            } else return false
        }
    }

    override fun warn(msg: String, vararg obj: Any?) {
        if (isWarnEnabled()) {
            warn(interleave(msg, obj))
        }
    }

    override fun warn(msg: String, thrown: Throwable) {
        if (isWarnEnabled()) {
            warn(msg)
            thrown.printStackTrace()
        }
    }

    override fun warn(o: Any?) {
        if (isWarnEnabled()) {
            if (o is Throwable) {
                @Suppress("KotlinPlaceholderCountMatchesArgumentCount")
                warn("", o)
            } else {
                warn(o.toString())
            }
        }
    }

    override fun warn(o: Any?, throwable: Throwable?) {
        if (isWarnEnabled()) {
            if (throwable == null) warn(o.toString())
            else warn(o.toString(), throwable)
        }
    }

    override fun fatal(o: Any?) {
        if (isFatalEnabled()) {
            fatal(o.toString())
        }
    }

    override fun fatal(o: Any?, throwable: Throwable?) {
        if (isFatalEnabled()) {
            if (throwable == null) fatal(o.toString())
            else fatal(o.toString(), throwable)
        }
    }

    override fun info(msg: Any?) {
        if (isInfoEnabled()) {
            info(msg.toString())
        }
    }

    override fun info(o: Any?, throwable: Throwable?) {
        if (isInfoEnabled()) {
            if (throwable == null) info(o)
            else info(o.toString(), throwable)
        }
    }

    override fun trace(o: Any?) {
        error(o)
    }

    override fun trace(o: Any?, throwable: Throwable?) {
        error(o, throwable)
    }

    override fun isLoggable(level: Level): Boolean {
        return LogManager.isEnabled(this, level.intValue())
    }

    override fun isTraceEnabled(): Boolean {
        return LogManager.isEnabled(this, org.apache.logging.log4j.Level.TRACE)
    }

    override fun isDebugEnabled(): Boolean {
        return LogManager.isEnabled(this, org.apache.logging.log4j.Level.DEBUG)
    }

    override fun isInfoEnabled(): Boolean {
        return LogManager.isEnabled(this, org.apache.logging.log4j.Level.INFO)
    }

    override fun isWarnEnabled(): Boolean {
        return LogManager.isEnabled(this, org.apache.logging.log4j.Level.WARN)
    }

    override fun isFatalEnabled(): Boolean {
        return LogManager.isEnabled(this, org.apache.logging.log4j.Level.FATAL)
    }

    override fun isErrorEnabled(): Boolean {
        return LogManager.isEnabled(this, org.apache.logging.log4j.Level.ERROR)
    }

    fun isSevereEnabled(): Boolean {
        return LogManager.isEnabled(this, org.apache.logging.log4j.Level.SEVERE)
    }

    // override fun warn(marker: Marker, msg: String, vararg obj: java.lang.Object): Unit = warn(msg, obj)
    companion object {

        private const val WARNING_TIMEOUT_NANOS = 10L * SECONDS_TO_NANOS

        private val lastWarned = HashMap<String, Long>()

        private var lastTime = 0L
        private val lastTimeStr = StringBuilder(16).append("hh:mm:ss.sss")
        private var logFileStream: OutputStream? = null

        private fun getLogFileStream(): OutputStream? {
            if (logFileStream != null || !OSFeatures.supportsContinuousLogFiles) return null

            logFileStream = VoidOutputStream
            createFileLogStream()
            return logFileStream
        }

        private fun createFileLogStream(): OutputStream? {
            val logFolder = ConfigBasics.cacheFolder.getChild("logs")
            logFolder.tryMkdirs()
            // to do pack all existing .log files into .zip files? -> no, the file size shouldn't be THAT big
            // delete log files older than 14 days
            val time = System.currentTimeMillis()
            val deleteTime = time - 14L * 24L * 3600L * 1000L
            for (child in logFolder.listChildren()) {
                val childCreated = child.nameWithoutExtension.toLongOrNull() ?: continue
                if (childCreated < deleteTime) child.delete()
            }
            val logFile = logFolder.getChild("$time.log")
            logFileStream = try {
                logFile.outputStream(true).apply {
                    // must use println, or we would create an infinite loop
                    println("[${getTimeStamp()},INFO:Logger] Writing log to $logFile")
                }
            } catch (e: IOException) {
                VoidOutputStream.apply {
                    println("[${getTimeStamp()},ERR:Logger] Failed creating log file, $e")
                }
            }
            return logFileStream
        }

        fun getTimeStamp(): CharSequence {
            val updateInterval = MILLIS_TO_NANOS shr 1 // half a millisecond
            val time = Time.nanoTime / updateInterval
            if (time != lastTime) {
                val calendar = Calendar.getInstance()
                val millis = calendar.get(Calendar.MILLISECOND)
                val seconds = calendar.get(Calendar.SECOND)
                val minutes = calendar.get(Calendar.MINUTE)
                val hours = calendar.get(Calendar.HOUR_OF_DAY)
                formatTime(hours, minutes, seconds, millis)
                lastTime = time
            }
            return lastTimeStr
        }

        private fun formatTime(hours: Int, minutes: Int, seconds: Int, millis: Int) {
            format10s(hours, 0)
            format10s(minutes, 3)
            format10s(seconds, 6)
            format100s(millis, 9)
        }

        private fun format10s(h: Int, i: Int) {
            lastTimeStr[i] = '0' + (h / 10)
            lastTimeStr[i + 1] = '0' + (h % 10)
        }

        @Suppress("SameParameterValue")
        private fun format100s(h: Int, i: Int) {
            format10s(h / 10, i)
            lastTimeStr[i + 2] = '0' + (h % 10)
        }
    }
}