package org.apache.logging.log4j

import java.util.logging.Level

interface Logger {
    fun info(msg: String)
    fun info(msg: String, obj: Array<java.lang.Object>)
    fun info(marker: Marker, msg: String)
    fun info(msg: String, thrown: Throwable)
    fun warn(msg: String)
    fun warn(msg: String, obj: Array<java.lang.Object>)
    fun warn(marker: Marker, msg: String, obj: Array<java.lang.Object>)
    fun warn(msg: String, thrown: Throwable)
    fun warning(msg: String) = warn(msg)
    fun error(msg: String)
    fun error(msg: String, obj: Array<java.lang.Object>)
    fun error(msg: String, thrown: Throwable)
    fun fatal(msg: String)
    fun fatal(msg: String, obj: Array<java.lang.Object>)
    fun fatal(msg: String, thrown: Throwable)
    fun severe(msg: String)
    fun severe(msg: String, obj: Array<java.lang.Object>)
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