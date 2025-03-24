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
        val value = logLevels[logger.name] ?: defaultLevel
        return level <= value
    }

    @JvmStatic
    fun isEnabled(logger: LoggerImpl, level: Int): Boolean {
        val value = logLevels[logger.name] ?: defaultLevel
        return level <= value.value
    }

    @JvmStatic
    fun disableLogger(logger: String?) {
        logLevels[logger] = Level.OFF
    }

    @JvmStatic
    fun disableLoggers(loggers: String) {
        for (logger in loggers.split(',')) {
            disableLogger(logger)
        }
    }

    @JvmStatic
    fun disableInfoLogs(loggers: String) {
        for (logger in loggers.split(',')) {
            logLevels[logger] = Level.WARN
        }
    }

    @JvmStatic
    @Suppress("unused")
    fun enableLogger(logger: String?) {
        define(logger, defaultLevel)
    }

    @JvmStatic
    @Suppress("unused")
    fun enableLogger(logger: LoggerImpl) {
        enableLogger(logger.name)
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
    private val loggers = HashMap<String, LoggerImpl>(256)

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
        val nameI = name ?: ""
        return loggers.getOrPut(nameI) { LoggerImpl(nameI) }
    }
}