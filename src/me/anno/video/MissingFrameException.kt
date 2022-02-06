package me.anno.video

import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import java.io.File

class MissingFrameException(msg: String) : RuntimeException(msg) {
    constructor(src: FileReference?) : this(src.toString())
    constructor(src: ISaveable) : this(toString(src))
    constructor(src: File) : this(src.toString())

    companion object {
        fun toString(src: ISaveable): String {
            val str = src.toString()
            return str.ifEmpty { src.className }
        }
    }
}