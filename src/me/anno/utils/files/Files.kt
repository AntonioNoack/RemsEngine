package me.anno.utils.files

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.io.*
import java.util.*
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

object Files {

    fun findNextFileName(reference: FileReference, digitsLength: Int, colonSymbol: Char, startingNumber: Long = 1): FileReference {
        // format: name-1.json
        // -, because the usual name may contain numbers itself
        // find all files matching the description, and then use the max+1
        val parent = reference.getParent() ?: return InvalidRef
        val name = reference.nameWithoutExtension
        val extension = reference.extension
        val siblings = parent.listChildren() ?: return InvalidRef
        val prefix = if(colonSymbol.code == 0) name else "$name$colonSymbol"
        val nameLength = prefix.length + digitsLength
        val relatedFiles = siblings.filter {
            it.extension == extension &&
                    it.nameWithoutExtension.startsWith(prefix) &&
                    it.nameWithoutExtension.length == nameLength
        }.mapNotNull {
            val name2 = it.nameWithoutExtension
            var i = prefix.length
            while (i + 1 < prefix.length && name2[i] == '0') {
                i++
            }
            name2.substring(i).toLongOrNull() // ^^, long for large names
        }
        val maxNumber = relatedFiles.maxOrNull() ?: startingNumber
        val nextNumber = maxNumber + 1
        val newName = "$prefix$nextNumber.$extension"
        return parent.getChild(newName)
    }

    fun Long.formatFileSize(): String {
        val endings = "kMGTPEZY"
        val divider = if (DefaultConfig["ui.file.showGiB", true]) 1024 else 1000
        val suffix = if (divider == 1024) "i" else ""
        val halfDivider = divider / 2
        var v = this
        if (v < halfDivider) return "$v Bytes"
        for (prefix in endings) {
            val vSaved = v
            v = (v + halfDivider) / divider
            if (v < divider) {
                return "${
                    when (v) {
                        in 0..9 -> "%.2f".format(Locale.ENGLISH, (vSaved.toFloat() / divider))
                        in 10..99 -> "%.1f".format(Locale.ENGLISH, (vSaved.toFloat() / divider))
                        else -> v.toString()
                    }
                } ${prefix}B$suffix"
            }
        }
        return "$v ${endings.last()}B$suffix"
    }

    fun File.listFiles2(includeHiddenFiles: Boolean = OS.isWindows) = listFiles()?.filter {
        !it.name.equals("desktop.ini", true) && (!name.startsWith('.') || includeHiddenFiles)
    } ?: emptyList()

    fun FileReference.listFiles2(includeHiddenFiles: Boolean = OS.isWindows) = listChildren()?.filter {
        !it.name.equals("desktop.ini", true) && (!name.startsWith('.') || includeHiddenFiles)
    } ?: emptyList()

    fun File.openInExplorer() {
        if (!exists()) {
            parentFile?.openInExplorer() ?: LOGGER.warn("Cannot open file $this, as it does not exist!")
        } else {
            when {
                OS.isWindows -> {// https://stackoverflow.com/questions/2829501/implement-open-containing-folder-and-highlight-file
                    OS.startProcess("explorer.exe", "/select,", absolutePath)
                }
                else -> {
                    if (Desktop.isDesktopSupported()) {
                        val desktop = Desktop.getDesktop()
                        desktop.open(if (isDirectory) this else this.parentFile ?: this)
                    } else if (OS.isLinux) {// https://askubuntu.com/questions/31069/how-to-open-a-file-manager-of-the-current-directory-in-the-terminal
                        OS.startProcess("xdg-open", absolutePath)
                    }
                }
            }
        }
    }

    inline fun <I : Closeable, V> use(closeable: I, run: (I) -> V): V {
        return closeable.use {
            run(it)
        }
    }

    fun File.zippedOutput() = outputStream().zipped()
    fun File.zippedInput() = inputStream().zipped()

    fun OutputStream.zipped() = DataOutputStream(DeflaterOutputStream(this.buffered()))
    fun InputStream.zipped() = DataInputStream(InflaterInputStream(this.buffered()))

    private val LOGGER = LogManager.getLogger(Files::class)

}