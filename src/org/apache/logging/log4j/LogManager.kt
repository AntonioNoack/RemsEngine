package org.apache.logging.log4j

import kotlin.reflect.KClass

/**
 * the main logging manager, which should be used all over the engine;
 * @see [me.anno.ui.debug.ConsoleOutputPanel]
 */
object LogManager {

    @JvmStatic
    private val logLevels = HashMap<String?, Level?>()

    @JvmStatic
    private var defaultLevel = Level.INFO

    @JvmStatic
    fun isEnabled(logger: LoggerImpl, level: Level): Boolean {
        val value = logLevels[logger.prefix] ?: defaultLevel
        return level <= value
    }

    @JvmStatic
    fun isEnabled(logger: LoggerImpl, level: Int): Boolean {
        val value = logLevels[logger.prefix] ?: defaultLevel
        return level <= value.value
    }

    @JvmStatic
    fun disableLogger(logger: String?) {
        logLevels[logger] = Level.OFF
    }

    @JvmStatic
    @Suppress("unused")
    fun enableLogger(logger: String?) {
        define(logger, defaultLevel)
    }

    @JvmStatic
    @Suppress("unused")
    fun enableLogger(logger: LoggerImpl) {
        enableLogger(logger.prefix)
    }

    @JvmStatic
    fun define(logger: String?, level: Level) {
        logLevels[logger] = level
    }

    @JvmStatic
    fun default(level: Level) {
        defaultLevel = level
    }

    @JvmStatic
    fun logAll() {
        default(Level.ALL)
    }

    @JvmStatic
    private val logger = LoggerImpl(null)

    @JvmStatic
    private val loggers = HashMap<String, LoggerImpl>()

    @JvmStatic
    fun getLogger(clazz: Class<*>): LoggerImpl {
        return getLogger(clazz.simpleName)
    }

    @JvmStatic
    fun getLogger(clazz: KClass<*>): LoggerImpl {
        return getLogger(clazz.simpleName)
    }

    @JvmStatic
    fun getLogger(name: String?): LoggerImpl {
        if (name == null) return logger
        var logger = loggers[name]
        if (logger == null) {
            logger = LoggerImpl(name)
            loggers[name] = logger
        }
        return logger
    }
}