package me.anno.jvm

import me.anno.engine.EngineBase
import me.anno.input.Clipboard
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Reference
import me.anno.utils.Sleep
import me.anno.utils.files.Files
import me.anno.utils.async.Callback.Companion.mapCallback
import me.anno.utils.structures.maps.BiMap
import org.apache.logging.log4j.LogManager
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.RenderedImage
import java.io.File
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

object ClipboardImpl {

    private val LOGGER = LogManager.getLogger(ClipboardImpl::class)
    private val copiedInternalFiles = BiMap<File, FileReference>()

    fun register() {
        Clipboard.setClipboardContentImpl = ClipboardImpl::setClipboardContent
        Clipboard.getClipboardContentImpl = ClipboardImpl::getClipboardContent
        Clipboard.copyFilesImpl = ClipboardImpl::copyFiles
    }

    fun setClipboardContent(copied: String) {
        val selection = StringSelection(copied)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }

    fun getClipboardContent(): Any? {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        try {
            val data = clipboard.getData(DataFlavor.stringFlavor)
            if (data is String) {
                return data
            }
        } catch (_: UnsupportedFlavorException) {
        }
        try {
            val data = clipboard.getData(DataFlavor.javaFileListFlavor) as? List<*>
            val data2 = data?.filterIsInstance<File>()
            if (!data2.isNullOrEmpty()) {
                return data2.map { copiedInternalFiles[it] ?: Reference.getReference(it.absolutePath) }
            }
        } catch (_: UnsupportedFlavorException) {
        }
        try {
            val data = clipboard.getData(DataFlavor.imageFlavor) as RenderedImage
            val folder = EngineBase.instance!!.getPersistentStorage()
            val file0 = folder.getChild("PastedImage.png")
            val file1 = Files.findNextFile(file0, 3, '-', 1)
            file1.outputStream().use { out: OutputStream ->
                if (!ImageIO.write(data, "png", out)) {
                    LOGGER.warn("Couldn't find writer for PNG format")
                }
            }
            LOGGER.info("Pasted image of size ${data.width} x ${data.height}, placed into $file1")
            return listOf(file1)
        } catch (_: UnsupportedFlavorException) {
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun copyFiles(files: List<FileReference>) {
        LOGGER.info("Copying $files")
        // we need this folder, when we have temporary copies,
        // because just FileFileRef.createTempFile() changes the name,
        // and we need the original file name
        val tmpFolder = lazy {
            val file = java.nio.file.Files.createTempDirectory("tmp").toFile()
            file.deleteOnExit()
            file
        }
        files.mapCallback(
            { _, src, cb ->
                if (src is FileFileRef) {
                    cb.ok(src.file)
                } else {
                    // create a temporary copy, that the OS understands
                    val tmp0 = copiedInternalFiles.reverse[src]
                    val ctr = AtomicInteger()
                    if (tmp0 != null) {
                        cb.ok(tmp0)
                    } else {
                        val tmp = File(tmpFolder.value, src.name)
                        copyHierarchy(
                            src,
                            Reference.getReference(tmp.absolutePath),
                            { ctr.incrementAndGet() },
                            { ctr.decrementAndGet() })
                        // wait for all copying to complete
                        Sleep.waitUntil(true, { ctr.get() == 0 }, {
                            copiedInternalFiles[tmp] = src
                            cb.ok(tmp)
                        })
                    }
                }
            }, { tmpFiles, err ->
                err?.printStackTrace()
                if (tmpFiles != null) {
                    Toolkit.getDefaultToolkit().systemClipboard
                        .setContents(FileTransferable(tmpFiles), null)
                }
            })
    }

    fun copyHierarchy(
        src: FileReference,
        dst: FileReference,
        started: (FileReference) -> Unit,
        finished: (FileReference) -> Unit
    ) {
        if (src.isDirectory) {
            dst.mkdirs()
            started(src)
            for (child in src.listChildren()) {
                copyHierarchy(child, dst.getChild(child.name), started, finished)
            }
            finished(src)
        } else {
            started(src)
            src.copyTo(dst) {
                finished(src)
            }
        }
    }
}