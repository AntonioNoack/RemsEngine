package org.apache.logging.log4j

interface Logger {
    fun info(msg: String)
    fun info(msg: String, obj: Array<java.lang.Object>)
    fun info(marker: Marker, msg: String)
    fun info(msg: String, thrown: Throwable)
    fun warn(msg: String)
    fun warn(msg: String, obj: Array<java.lang.Object>)
    fun warn(marker: Marker, msg: String, obj: Array<java.lang.Object>)
    fun warn(msg: String, thrown: Throwable)
    fun error(msg: String)
    fun error(msg: String, obj: Array<java.lang.Object>)
    fun error(msg: String, thrown: Throwable)
    fun fatal(msg: String)
    fun fatal(msg: String, obj: Array<java.lang.Object>)
    fun fatal(msg: String, thrown: Throwable)
}