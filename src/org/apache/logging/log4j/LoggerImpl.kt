package org.apache.logging.log4j

import me.anno.Engine
import me.anno.maths.Maths.MILLIS_TO_NANOS
import org.apache.commons.logging.Log
import java.util.*
import java.util.logging.Level

open class LoggerImpl(val prefix: String?) : Logger, Log {

    private val lastWarned = HashMap<String, Long>()
    private val warningTimeoutNanos = 10e9.toLong()

    private fun interleave(msg: String, args: Array<out Any?>): String {
        if (args.isEmpty()) return msg
        return if (msg.contains("{}")) {
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
            builder.toString()
        } else {
            msg.format(Locale.ENGLISH, *args)
            /*val funFormat = String::class.java.getMethod("format", Locale::class.java, Array<Object>(0){ throw RuntimeException() }::class.java)
            return funFormat.invoke(msg, Locale.ENGLISH, args) as String
            return msg.format(Locale.ENGLISH, args)*/
        }
    }

    private val suffix = if (prefix == null) "" else ":$prefix"

    private var lastTime = 0L
    private var lastString = ""
    private fun getTimeStamp(): String {
        val updateInterval = 500 * MILLIS_TO_NANOS
        val time = Engine.nanoTime / updateInterval
        synchronized(Unit) {
            if (time == lastTime && lastString.isNotEmpty())
                return lastString
            val calendar = Calendar.getInstance()
            val seconds = calendar.get(Calendar.SECOND)
            val minutes = calendar.get(Calendar.MINUTE)
            val hours = calendar.get(Calendar.HOUR_OF_DAY)
            lastTime = time
            lastString = "%2d:%2d:%2d".format(hours, minutes, seconds)
                .replace(' ', '0')
            return lastString
        }
    }

    fun print(prefix: String, msg: String) {
        if (LogManager.isEnabled(this)) {
            for (line in msg.split('\n')) {
                val line2 = "[${getTimeStamp()},$prefix$suffix] $line"
                if (prefix == "ERR!" || prefix == "WARN") {
                    System.err.println(line2)
                } else {
                    println(line2)
                }
            }
        }
    }

    override fun info(msg: String) {
        print("INFO", msg)
    }

    override fun info(msg: String, vararg obj: Any?) {
        if (LogManager.isEnabled(this))
            info(interleave(msg, obj))
    }

    override fun info(marker: Marker, msg: String) {
        info(msg)
    }

    override fun info(msg: String, thrown: Throwable) {
        info(msg)
        thrown.printStackTrace()
    }

    override fun debug(msg: String) {
        print("DEBUG", msg)
    }

    override fun debug(msg: String, e: Throwable) {
        print("DEBUG", msg)
        e.printStackTrace()
    }

    override fun error(msg: String) {
        print("ERR!", msg)
    }

    override fun error(msg: String, vararg obj: Any?) {
        if (LogManager.isEnabled(this))
            error(interleave(msg, obj))
    }

    override fun error(msg: String, thrown: Throwable) {
        error(msg)
        thrown.printStackTrace()
    }

    override fun severe(msg: String) {
        print("SEVERE", msg)
    }

    override fun severe(msg: String, vararg obj: Any?) {
        if (LogManager.isEnabled(this))
            error(interleave(msg, obj))
    }

    override fun severe(msg: String, thrown: Throwable) {
        error(msg)
        thrown.printStackTrace()
    }

    override fun fatal(msg: String) {
        print("FATAL", msg)
    }

    override fun fatal(msg: String, vararg obj: Any?) {
        if (LogManager.isEnabled(this))
            fatal(interleave(msg, obj))
    }

    override fun fatal(msg: String, thrown: Throwable) {
        fatal(msg)
        thrown.printStackTrace()
    }

    override fun warn(msg: String) {
        synchronized(lastWarned) {
            val time = Engine.gameTime
            if (msg !in lastWarned || (lastWarned[msg]!! - time) > warningTimeoutNanos) {
                lastWarned[msg] = time
                print("WARN", msg)
            }
        }
    }

    override fun warn(msg: String, vararg obj: Any?) {
        if (LogManager.isEnabled(this))
            warn(interleave(msg, obj))
    }

    override fun warn(msg: String, thrown: Throwable) {
        warn(msg)
        thrown.printStackTrace()
    }

    override fun warn(o: Any?) {
        if (o is Throwable) {
            warn("", o)
        } else {
            warn(o.toString())
        }
    }

    override fun warn(o: Any?, throwable: Throwable?) {
        if (throwable == null) warn(o.toString())
        else warn(o.toString(), throwable)
    }

    override fun fatal(o: Any?) {
        fatal(o.toString())
    }

    override fun fatal(o: Any?, throwable: Throwable?) {
        if (throwable == null) fatal(o.toString())
        else fatal(o.toString(), throwable)
    }

    override fun info(msg: Any?) {
        info(msg.toString())
    }

    override fun info(o: Any?, throwable: Throwable?) {
        if (throwable == null) info(o)
        else info(o.toString(), throwable)
    }

    override fun error(o: Any?) {
        error(o.toString())
    }

    override fun error(o: Any?, throwable: Throwable?) {
        if (throwable == null) error(o)
        else error(o.toString(), throwable)
    }

    override fun debug(o: Any?) {
        debug(o.toString())
    }

    override fun debug(o: Any?, throwable: Throwable?) {
        if (throwable == null) debug(o)
        else debug(o.toString(), throwable)
    }

    override fun trace(o: Any?) {
        error(o)
    }

    override fun trace(o: Any?, throwable: Throwable?) {
        error(o, throwable)
    }

    override fun isLoggable(level: Level): Boolean {
        return LogManager.isEnabled(this)
    }

    override fun isTraceEnabled(): Boolean {
        return LogManager.isEnabled(this)
    }

    override fun isDebugEnabled(): Boolean {
        return LogManager.isEnabled(this)
    }

    override fun isInfoEnabled(): Boolean {
        return LogManager.isEnabled(this)
    }

    override fun isWarnEnabled(): Boolean {
        return LogManager.isEnabled(this)
    }

    override fun isFatalEnabled(): Boolean {
        return LogManager.isEnabled(this)
    }

    override fun isErrorEnabled(): Boolean {
        return LogManager.isEnabled(this)
    }


    // override fun warn(marker: Marker, msg: String, vararg obj: java.lang.Object): Unit = warn(msg, obj)


}