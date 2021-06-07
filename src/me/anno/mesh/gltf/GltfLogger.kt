package me.anno.mesh.gltf

import de.javagl.jgltf.logging.Logger
import org.apache.logging.log4j.LogManager
import java.util.logging.Level

object GltfLogger {

    fun setup() {
        Logger.global = object : Logger() {

            override fun log(logger: Logger, lvl: Level, msg: String) {
                when (lvl) {
                    Level.INFO -> info(logger, msg)
                    Level.WARNING -> warning(logger, msg)
                    Level.SEVERE -> severe(logger, msg)
                    Level.FINE -> fine(logger, msg)
                    Level.FINEST -> finest(logger, msg)
                    else -> super.log(logger, lvl, msg)
                }
            }

            override fun log(logger: Logger, lvl: Level, msg: String, exception: Exception) {
                exception.printStackTrace()
                log(logger, lvl, "$msg, $exception")
            }

            override fun info(logger: Logger, msg: String) {
                LogManager.getLogger(logger.clazz).info(msg)
            }

            override fun warning(logger: Logger, msg: String) {
                LogManager.getLogger(logger.clazz).warn(msg)
            }

            override fun severe(logger: Logger, msg: String) {
                LogManager.getLogger(logger.clazz).severe(msg)
            }

            override fun fine(logger: Logger, msg: String) {
                LogManager.getLogger(logger.clazz).fine(msg)
            }

            override fun finest(logger: Logger, msg: String) {
                LogManager.getLogger(logger.clazz).finest(msg)
            }

        }
    }

}