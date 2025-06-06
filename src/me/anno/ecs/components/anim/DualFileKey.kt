package me.anno.ecs.components.anim

import me.anno.io.files.FileReference

/**
 * key for Caches, which represents to file states at once
 * */
fun DualFileKey(
    file0: FileReference,
    file1: FileReference,
) = file0.getFileKey() to file1.getFileKey()