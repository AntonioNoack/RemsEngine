package me.anno.video

import me.anno.io.FileReference
import me.anno.objects.Transform
import java.io.File

class MissingFrameException(msg: String) : RuntimeException(msg) {
    constructor(src: FileReference?) : this(src.toString())
    constructor(src: Transform) : this(src.toString())
    constructor(src: File) : this(src.toString())
}