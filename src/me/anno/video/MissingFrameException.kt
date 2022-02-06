package me.anno.video

import me.anno.io.files.FileReference
import me.anno.remsstudio.objects.Transform
import java.io.File

class MissingFrameException(msg: String) : RuntimeException(msg) {
    constructor(src: FileReference?) : this(src.toString())
    constructor(src: Transform) : this(toString(src))
    constructor(src: File) : this(src.toString())

    companion object {
        fun toString(src: Transform): String {
            val str = src.toString()
            return str.ifEmpty { src.className }
        }
    }
}