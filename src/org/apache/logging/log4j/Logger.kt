package org.apache.logging.log4j

import java.util.logging.Level

interface Logger {
    fun info() = info("")
    fun info(msg: Any?) = info(msg.toString())
    fun info(msg: String)
    fun info(msg: String, vararg obj: Any?)
    fun info(marker: Marker, msg: String)
    fun info(msg: String, thrown: Throwable)
    fun warn(msg: String)
    fun warn(msg: String, vararg obj: Any?)
    fun warn(msg: String, thrown: Throwable)
    fun warning(msg: String) = warn(msg)
    fun error(msg: String)
    fun error(msg: String, vararg obj: Any?)
    fun error(msg: String, thrown: Throwable)
    fun fatal(msg: String)
    fun fatal(msg: String, vararg obj: Any?)
    fun fatal(msg: String, thrown: Throwable)
    fun severe(msg: String)
    fun severe(msg: String, vararg obj: Any?)
    fun severe(msg: String, thrown: Throwable)
    fun fine(msg: String) = info(msg)
    fun fine(msg: String, thrown: Throwable) = info(msg, thrown)
    fun debug(msg: String)
    fun debug(msg: String, thrown: Throwable)
    fun debug(msg: String, vararg obj: Any?)
    fun finer(msg: String) = info(msg)
    fun finer(msg: String, thrown: Throwable) = info(msg, thrown)
    fun finest(msg: String) = info(msg)
    fun finest(msg: String, thrown: Throwable) = info(msg, thrown)
    fun isLoggable(level: Level) = true
    fun log(level: Level, msg: String) = when(level){
        Level.FINE, Level.FINER, Level.FINEST -> fine(msg)
        Level.WARNING -> warn(msg)
        Level.SEVERE -> severe(msg)
        else -> info("$level: $msg")
    }
    fun log(level: Level, msg: String, thrown: Throwable) = when(level){
        Level.FINE, Level.FINER, Level.FINEST -> fine(msg, thrown)
        Level.WARNING -> warn(msg, thrown)
        Level.SEVERE -> severe(msg, thrown)
        else -> info("$level: $msg", thrown)
    }
}