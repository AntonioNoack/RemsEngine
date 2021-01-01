package me.anno.video

import java.io.File
import java.lang.RuntimeException

class MissingFrameException(msg: String): RuntimeException(msg){
    constructor(src: File?): this(src.toString())
}