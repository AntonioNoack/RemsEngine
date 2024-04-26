package me.anno.io.utils

import me.anno.io.files.FileReference

object LinkCreator {
    var createLink: ((src: FileReference, dst: FileReference, tmp: FileReference?) -> FileReference?)? = null
}