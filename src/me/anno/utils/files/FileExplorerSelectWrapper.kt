package me.anno.utils.files

import org.apache.logging.log4j.LogManager
import java.io.File

object FileExplorerSelectWrapper {

    private fun notAvailable(e: Throwable): Nothing? {
        LOGGER.info("JavaFX is not available, ${e.message}")
        return null
    }

    private val method by lazy {
        try {
            val clazz = javaClass.classLoader.loadClass("me.anno.utils.FileExplorerSelect")
            clazz?.getMethod("selectFileOrFolder", File::class.java, Boolean::class.java, Function1::class.java)
        } catch (e: NoClassDefFoundError) {
            notAvailable(e)
        } catch (e: ClassNotFoundException) {
            notAvailable(e)
        } catch (e: NoSuchMethodError) {
            notAvailable(e)
        } catch (e: SecurityException) {
            notAvailable(e)
        }
    }

    fun selectFile(lastFile: File?, callback: (File?) -> Unit) {
        // FileExplorerSelect.selectFile(lastFile, callback)
        val method = method
        if (method != null) {
            method.invoke(null, lastFile, false, callback)
        } else {
            // todo use JFileChooser
            // or via OpenFileDialog
            LOGGER.info("JavaFX is not available")
        }
    }

    fun selectFolder(lastFile: File?, callback: (File?) -> Unit) {
        // FileExplorerSelect.selectFolder(lastFile, callback)
        method?.invoke(null, lastFile, true, callback) ?: LOGGER.info("JavaFX is not available")
    }

    fun selectFileOrFolder(lastFile: File?, isDirectory: Boolean, callback: (File?) -> Unit) {
        // FileExplorerSelect.selectFileOrFolder(lastFile, isDirectory, callback)
        method?.invoke(null, lastFile, isDirectory, callback) ?: LOGGER.info("JavaFX is not available")
    }

    private val LOGGER = LogManager.getLogger(FileExplorerSelectWrapper::class)

}