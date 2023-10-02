package me.anno.utils.files

import me.anno.io.ResourceHelper.loadResource
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import org.apache.logging.log4j.LogManager
import java.awt.Component
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileFilter
import javax.swing.plaf.nimbus.NimbusLookAndFeel
import kotlin.concurrent.thread


// to do create custom file chooser instead of using JFileChooser?
object FileChooser {

    @JvmStatic
    private fun notAvailable(e: Throwable): Nothing? {
        LOGGER.info("JavaFX is not available, ${e::class.simpleName}: ${e.message}")
        return null
    }

    @JvmStatic
    private val method by lazy {
        try {
            val clazz = javaClass.classLoader.loadClass("me.anno.utils.files.FileExplorerSelect")
            clazz?.getMethod(
                "select",
                Boolean::class.java,
                Boolean::class.java,
                Boolean::class.java,
                File::class.java,
                Boolean::class.java,
                Array<Array<String>>::class.java,
                Function1::class.java
            )
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

    @JvmStatic
    fun selectFiles(
        allowFiles: Boolean, allowFolders: Boolean, allowMultiples: Boolean,
        startFolder: FileReference, toSave: Boolean, filters: List<FileExtensionFilter>,
        callback: (List<FileReference>) -> Unit
    ) {
        if (!allowFiles && !allowFolders) {
            callback(emptyList())
            return
        }
        thread(name = "Select File/Folder") {
            val startFolder1 = (startFolder as? FileFileRef)?.file
            val canUseJavaFX = !(allowFiles && allowFolders) && // both types are not supported together in JavaFX
                    !(allowMultiples && (allowFolders || toSave)) // multiple folders cannot be selected in JavaFX; multiple toSave files neither
            val method = if (canUseJavaFX) method else null
            if (method != null) {
                val filters1: Array<Array<String>> = filters.map {
                    (listOf(it.nameDesc.name) + it.extensions).toTypedArray()
                }.toTypedArray()
                method.invoke(
                    null, allowFiles, allowFolders, allowMultiples,
                    startFolder1, toSave, filters1, callback
                )
            } else selectFilesUsingJFileChooser(
                allowFiles, allowFolders, allowMultiples,
                startFolder1, toSave, filters, callback
            )
        }
    }

    private fun initSwingStyle() {
        UIManager.setLookAndFeel(NimbusLookAndFeel::class.java.name)
    }

    private fun selectFilesUsingJFileChooser(
        allowFiles: Boolean, allowFolders: Boolean, allowMultiples: Boolean,
        startFolder: File?, toSave: Boolean, filters: List<FileExtensionFilter>,
        callback: (List<FileReference>) -> Unit
    ) {

        initSwingStyle()

        val jFileChooser = object : JFileChooser() {
            override fun createDialog(p0: Component?): JDialog {
                val dialog = super.createDialog(p0)
                val image = loadResource("icon.png").use { ImageIO.read(it) }
                dialog.setIconImage(image)
                return dialog
            }
        }

        if (startFolder != null) jFileChooser.currentDirectory = startFolder
        jFileChooser.fileSelectionMode = when {
            allowFolders && allowFiles -> JFileChooser.FILES_AND_DIRECTORIES
            allowFolders -> JFileChooser.DIRECTORIES_ONLY
            else -> JFileChooser.FILES_ONLY
        }
        jFileChooser.isMultiSelectionEnabled = allowMultiples
        jFileChooser.isAcceptAllFileFilterUsed = filters.isEmpty() || filters.any { "*" in it.extensions }
        for (filter in filters) {
            if (filter.extensions == listOf("*")) continue
            jFileChooser.addChoosableFileFilter(object : FileFilter() {
                override fun getDescription() = filter.nameDesc.name // actually the title
                override fun accept(file: File) = file.extension.lowercase() in filter.extensions
            })
        }
        val retCode =
            if (toSave) jFileChooser.showSaveDialog(null)
            else jFileChooser.showOpenDialog(null)
        if (retCode == JFileChooser.APPROVE_OPTION) {
            val sf = jFileChooser.selectedFiles
            callback(sf.map { getReference(it) })
        } else callback(emptyList())
    }

    @JvmStatic
    private val LOGGER = LogManager.getLogger(FileChooser::class)
}