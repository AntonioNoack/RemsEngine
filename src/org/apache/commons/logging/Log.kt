package org.apache.commons.logging

interface Log {
    fun debug(o: Any?)
    fun debug(o: Any?, throwable: Throwable?)
    fun error(o: Any?)
    fun error(o: Any?, throwable: Throwable?)
    fun fatal(o: Any?)
    fun fatal(o: Any?, throwable: Throwable?)
    fun info(msg: Any?)
    fun info(o: Any?, throwable: Throwable?)
    fun isDebugEnabled(): Boolean
    fun isErrorEnabled(): Boolean
    fun isFatalEnabled(): Boolean
    fun isInfoEnabled(): Boolean
    fun isTraceEnabled(): Boolean
    fun isWarnEnabled(): Boolean
    fun trace(o: Any?)
    fun trace(o: Any?, throwable: Throwable?)
    fun warn(o: Any?)
    fun warn(o: Any?, throwable: Throwable?)
}
