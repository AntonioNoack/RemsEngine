package org.apache.logging.log4j

import kotlin.reflect.KClass

/**
 * the main logging manager, which should be used
 */
object LogManager {

    private val disabled = HashSet<String?>()
    fun isEnabled(logger: LoggerImpl): Boolean {
        return !disabled.contains(logger.prefix)
    }

    fun disableLogger(logger: String?) {
        disabled.add(logger)
    }

    fun enableLogger(logger: String?) {
        disabled.remove(logger)
    }

    private val logger = LoggerImpl(null)
    private val loggers = HashMap<String, LoggerImpl>()

    @JvmStatic
    fun getLogger(clazz: Class<*>): LoggerImpl {
        return getLogger(clazz.simpleName)
    }

    @JvmStatic
    fun getLogger(clazz: KClass<*>): LoggerImpl {
        // System.out.println("Inited " + clazz.getSimpleName());
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