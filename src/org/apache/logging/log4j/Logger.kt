package org.apache.logging.log4j

import java.util.logging.Level

interface Logger {
    fun info(msg: String)
    fun info(msg: String, vararg obj: Any)
    fun info(marker: Marker, msg: String)
    fun info(msg: String, thrown: Throwable)
    fun warn(msg: String)
    fun warn(msg: String, vararg obj: Any)
    fun warn(msg: String, thrown: Throwable)
    fun warning(msg: String) = warn(msg)
    fun error(msg: String)
    fun error(msg: String, vararg obj: Any)
    fun error(msg: String, thrown: Throwable)
    fun fatal(msg: String)
    fun fatal(msg: String, vararg obj: Any)
    fun fatal(msg: String, thrown: Throwable)
    fun severe(msg: String)
    fun severe(msg: String, vararg obj: Any)
    fun severe(msg: String, thrown: Throwable)
    fun fine(msg: String) = info(msg)
    fun fine(msg: String, e: Throwable) = info(msg, e)
    fun debug(msg: String) = info(msg)
    fun debug(msg: String, e: Throwable) = info(msg, e)
    fun finer(msg: String) = info(msg)
    fun finer(msg: String, e: Throwable) = info(msg, e)
    fun finest(msg: String) = info(msg)
    fun finest(msg: String, e: Throwable) = info(msg, e)
    fun isLoggable(level: Level) = true
    fun log(level: Level, msg: String) = when(level){
        Level.FINE, Level.FINER, Level.FINEST -> fine(msg)
        Level.WARNING -> warn(msg)
        Level.SEVERE -> severe(msg)
        else -> info("$level: $msg")
    }
    fun log(level: Level, msg: String, e: Throwable) = when(level){
        Level.FINE, Level.FINER, Level.FINEST -> fine(msg, e)
        Level.WARNING -> warn(msg, e)
        Level.SEVERE -> severe(msg, e)
        else -> info("$level: $msg", e)
    }
}