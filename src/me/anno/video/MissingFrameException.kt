package me.anno.video

import me.anno.io.ISaveable

class MissingFrameException(msg: String) : RuntimeException(msg) {

    constructor(src: Any?) : this(src.toString())
    constructor(src: ISaveable) : this(toString(src))

    companion object {
        fun toString(src: ISaveable): String {
            val str = src.toString()
            return str.ifEmpty { src.className }
        }
    }
}