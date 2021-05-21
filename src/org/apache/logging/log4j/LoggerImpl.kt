package org.apache.logging.log4j

import org.apache.commons.logging.Log
import java.util.*

class LoggerImpl(prefix: String?) : Logger, Log {

    fun interleave(msg: String, args: Array<out Any>): String {
        if (args.isEmpty()) return msg
        return if (msg.contains("{}")) {
            val parts = msg.split("{}")
            parts.take(parts.size - 1).mapIndexed { index, s -> "$s${args.getOrNull(index)}" }
                .joinToString("") + parts.last()
        } else {
            msg.format(Locale.ENGLISH, *args)
            /*val funFormat = String::class.java.getMethod("format", Locale::class.java, Array<Object>(0){ throw RuntimeException() }::class.java)
            return funFormat.invoke(msg, Locale.ENGLISH, args) as String
            return msg.format(Locale.ENGLISH, args)*/
        }
    }

    val suffix = if (prefix == null) "" else ":$prefix"

    fun getTimeStamp(): String {
        val calendar = Calendar.getInstance()
        val seconds = calendar.get(Calendar.SECOND)
        val minutes = calendar.get(Calendar.MINUTE)
        val hours= calendar.get(Calendar.HOUR_OF_DAY)
        return "%2d:%2d:%2d".format(hours, minutes, seconds)
            .replace(' ', '0')
    }

    fun print(prefix: String, msg: String) {
        msg.split('\n').forEach { line ->
            val line2 = "[${getTimeStamp()},$prefix$suffix] $line"
            if(prefix == "ERR!"){
                System.err.println(line2)
            } else {
                println(line2)
            }
        }
    }

    override fun info(msg: String) {
        when (suffix) {
            ":ScratchFileBuffer", ":PDFObjectStreamParser",
            ":FontFileFinder", ":FontMapperImpl", ":FileSystemFontProvider" -> Unit
            else -> print("INFO", msg)
        }
    }

    override fun info(msg: String, vararg obj: Any) {
        info(interleave(msg, obj))
    }

    override fun info(marker: Marker, msg: String) {
        info(msg)
    }

    override fun info(msg: String, thrown: Throwable) {
        info(msg)
        thrown.printStackTrace()
    }

    override fun error(msg: String) {
        print("ERR!", msg)
    }

    override fun error(msg: String, vararg obj: Any) {
        error(interleave(msg, obj))
    }

    override fun error(msg: String, thrown: Throwable) {
        error(msg)
        thrown.printStackTrace()
    }

    override fun severe(msg: String) {
        print("SEVERE", msg)
    }

    override fun severe(msg: String, vararg obj: Any) {
        error(interleave(msg, obj))
    }

    override fun severe(msg: String, thrown: Throwable) {
        error(msg)
        thrown.printStackTrace()
    }

    override fun fatal(msg: String) {
        print("FATAL", msg)
    }

    override fun fatal(msg: String, vararg obj: Any) {
        fatal(interleave(msg, obj))
    }

    override fun fatal(msg: String, thrown: Throwable) {
        fatal(msg)
        thrown.printStackTrace()
    }

    override fun warn(msg: String) {
        print("WARN", msg)
    }

    override fun warn(msg: String, vararg obj: Any) {
        warn(interleave(msg, obj))
    }

    override fun warn(msg: String, thrown: Throwable) {
        warn(msg)
        thrown.printStackTrace()
    }

    override fun warn(o: Any?) {
        warn(o.toString())
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

    override fun info(o: Any?) {
        info(o.toString())
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

    override fun isTraceEnabled(): Boolean {
        return true
    }

    override fun isDebugEnabled(): Boolean {
        return true
    }

    override fun isInfoEnabled(): Boolean {
        return true
    }

    override fun isWarnEnabled(): Boolean {
        return true
    }

    override fun isFatalEnabled(): Boolean {
        return true
    }

    override fun isErrorEnabled(): Boolean {
        return true
    }


    // override fun warn(marker: Marker, msg: String, vararg obj: java.lang.Object): Unit = warn(msg, obj)


}