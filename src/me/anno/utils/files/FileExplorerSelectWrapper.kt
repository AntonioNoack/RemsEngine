package me.anno.utils.files

import org.apache.logging.log4j.LogManager
import java.io.File
import java.lang.reflect.Method
import javax.swing.JFileChooser
import kotlin.concurrent.thread

object FileExplorerSelectWrapper {

    private fun notAvailable(e: Throwable): Nothing? {
        LOGGER.info("JavaFX is not available, ${e.message}")
        return null
    }

    private fun getMethod(name: String): Method? {
        return try {
            val clazz = javaClass.classLoader.loadClass("me.anno.utils.FileExplorerSelect")
            clazz?.getMethod(name, File::class.java, Function1::class.java)
        } catch (e: NoClassDefFoundError) {
            notAvailable(e)
        } catch (e: ClassNotFoundException) {
            notAvailable(e)
        } catch (e: NoSuchMethodError) {
            notAvailable(e)
        } catch (e: NoSuchMethodException) {
            notAvailable(e)
        } catch (e: SecurityException) {
            notAvailable(e)
        }
    }

    // private val fileOrFolder by lazy { getMethod("selectFileOrFolder") }
    private val file by lazy { getMethod("selectFile") }
    private val folder by lazy { getMethod("selectFolder") }

    fun selectFile(lastFile: File?, callback: (File?) -> Unit) {
        selectFileOrFolder(lastFile, false, callback)
    }

    fun selectFolder(lastFile: File?, callback: (File?) -> Unit) {
        selectFileOrFolder(lastFile, true, callback)
    }

    fun selectFileOrFolder(lastFile: File?, isDirectory: Boolean, callback: (File?) -> Unit) {
        thread(name = "Select File/Folder") {
            val method = if (isDirectory) folder else file
            if (method != null) {
                method.invoke(null, lastFile, callback)
            } else {
                // use JFileChooser
                // or via OpenFileDialog...
                val jfc = if (lastFile != null) JFileChooser(lastFile) else JFileChooser()
                jfc.fileSelectionMode = if (isDirectory) JFileChooser.DIRECTORIES_ONLY else JFileChooser.FILES_ONLY
                val retCode = jfc.showOpenDialog(null)
                if (retCode == JFileChooser.APPROVE_OPTION) {
                    val sf = jfc.selectedFile
                    if (sf.isDirectory == isDirectory) {
                        callback(sf)
                    } else callback(null) // mmh
                } else callback(null)
                LOGGER.info("JavaFX is not available")
            }
        }
    }

    private val LOGGER = LogManager.getLogger(FileExplorerSelectWrapper::class)

}