package org.lwjgl

import org.apache.logging.log4j.LogManager.getLogger
import org.apache.logging.log4j.Logger

/**
 * overriding the logger for LWJGL
 */
object LWJGLUtil {

    private val LOGGER: Logger = getLogger(LWJGLUtil::class.java)

    @JvmStatic
    fun log(msg: CharSequence) {
        LOGGER.info(msg.toString())
    }

    @JvmStatic
    fun log(msg: String?) {
        LOGGER.info(msg)
    }
}