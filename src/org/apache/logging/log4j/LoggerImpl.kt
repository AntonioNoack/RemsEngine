package org.apache.logging.log4j

import java.lang.RuntimeException
import java.util.*

class LoggerImpl(prefix: String?): Logger {

    fun interleave(msg: String, args: Array<out Object>): String {
        if(args.isEmpty()) return msg
        return if(msg.contains("{}")){
            val parts = msg.split("{}")
            parts.take(parts.size-1).mapIndexed { index, s -> "$s${args.getOrNull(index)}" }.joinToString("") + parts.last()
        } else {
            msg.format(Locale.ENGLISH, *args)
            /*val funFormat = String::class.java.getMethod("format", Locale::class.java, Array<Object>(0){ throw RuntimeException() }::class.java)
            return funFormat.invoke(msg, Locale.ENGLISH, args) as String
            return msg.format(Locale.ENGLISH, args)*/
        }
    }

    val suffix = if(prefix == null) "" else ":$prefix"

    fun print(prefix: String, msg: String){
        msg.split('\n').forEach { line ->
            println("[$prefix$suffix] $line")
        }
    }

    override fun info(msg: String) {
        print("INFO", msg)
    }

    override fun info(msg: String, vararg obj: java.lang.Object) {
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

    override fun error(msg: String, vararg obj: java.lang.Object) {
        error(interleave(msg, obj))
    }

    override fun error(msg: String, thrown: Throwable) {
        error(msg)
        thrown.printStackTrace()
    }

    override fun severe(msg: String) {
        print("SEVERE", msg)
    }

    override fun severe(msg: String, vararg obj: java.lang.Object) {
        error(interleave(msg, obj))
    }

    override fun severe(msg: String, thrown: Throwable) {
        error(msg)
        thrown.printStackTrace()
    }

    override fun fatal(msg: String) {
        print("FATAL", msg)
    }

    override fun fatal(msg: String, vararg obj: java.lang.Object) {
        fatal(interleave(msg, obj))
    }

    override fun fatal(msg: String, thrown: Throwable) {
        fatal(msg)
        thrown.printStackTrace()
    }

    override fun warn(msg: String) {
        print("WARN", msg)
    }

    override fun warn(msg: String, vararg obj: java.lang.Object) {
        warn(interleave(msg, obj))
    }

    override fun warn(msg: String, thrown: Throwable) {
        warn(msg)
        thrown.printStackTrace()
    }

    // override fun warn(marker: Marker, msg: String, vararg obj: java.lang.Object): Unit = warn(msg, obj)


}