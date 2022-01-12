package me.anno.studio.rems

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import java.io.File

data class ProjectHeader(val name: String, val file: FileReference) {
    constructor(name: String, file: File) : this(name, getReference(file))
}