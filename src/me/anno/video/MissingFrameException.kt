package me.anno.video

import me.anno.objects.Transform
import java.io.File
import java.lang.RuntimeException

class MissingFrameException(msg: String): RuntimeException(msg){
    constructor(src: File?): this(src.toString())
    constructor(src: Transform): this(src.toString())
}