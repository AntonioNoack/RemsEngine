package me.anno.ui.editor.files

import me.anno.io.files.FileReference
import me.anno.io.files.SignatureCache
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.files.Files.formatFileSize
import java.text.SimpleDateFormat
import java.util.Date

enum class FileStatType(val alignment: AxisAlignment) {
    FILE_NAME(AxisAlignment.MIN) {
        override fun getValue(file: FileReference): String {
            return file.nameWithoutExtension
        }
    },
    FILE_SIZE(AxisAlignment.CENTER) {
        override fun getValue(file: FileReference): String {
            return if (file.isDirectory) ""
            else file.length().formatFileSize()
        }
    },
    EXTENSION(AxisAlignment.CENTER) {
        override fun getValue(file: FileReference): String {
            return file.extension
        }
    },
    SIGNATURE(AxisAlignment.CENTER) {
        override fun getValue(file: FileReference): String {
            return SignatureCache[file, true]?.name ?: ""
        }
    },
    IMPORT_TYPE(AxisAlignment.CENTER) {
        override fun getValue(file: FileReference): String {
            return SignatureCache[file, true]?.importType ?: ""
        }
    },
    CREATED(AxisAlignment.MIN) {
        override fun getValue(file: FileReference): String {
            return dateFormat.format(Date(file.creationTime))
        }
    },
    MODIFIED(AxisAlignment.MIN) {
        override fun getValue(file: FileReference): String {
            return dateFormat.format(Date(file.lastModified))
        }
    };

    abstract fun getValue(file: FileReference): String

    companion object {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
}