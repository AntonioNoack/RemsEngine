package me.anno.ecs.components.anim

import me.anno.io.files.FileReference

/**
 * key for Caches, which represents to file states at once
 * */
data class DualFileKey(
    val file0: FileReference,
    val file1: FileReference,
    val file0LastModified: Long,
    val file1LastModified: Long,
) {
    constructor(file0: FileReference, file1: FileReference) :
            this(file0, file1, file0.lastModified, file1.lastModified)
}