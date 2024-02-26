package me.anno.ui.editor.files

import me.anno.io.files.FileReference
import me.anno.utils.structures.Compare.ifSame

enum class FileSorting {
    NAME {
        override fun compare(a: FileReference, b: FileReference): Int {
            return a.name.compareTo(b.name, true)
        }
    },
    SIZE {
        override fun compare(a: FileReference, b: FileReference): Int {
            return a.length().compareTo(b.length())
        }
    },
    LAST_MODIFIED {
        override fun compare(a: FileReference, b: FileReference): Int {
            return a.lastModified.compareTo(b.lastModified)
        }
    },
    CREATION_TIME {
        override fun compare(a: FileReference, b: FileReference): Int {
            return a.creationTime.compareTo(b.creationTime)
        }
    },
    EXTENSION {
        override fun compare(a: FileReference, b: FileReference): Int {
            return a.lcExtension.compareTo(b.lcExtension).ifSame(a.name.compareTo(b.name, true))
        }
    };

    abstract fun compare(a: FileReference, b: FileReference): Int
}