package me.anno.utils.test

import me.anno.mesh.fbx.structure.FBXReader
import me.anno.utils.OS

fun main() {
    FBXReader(OS.downloads.getChild("Female Standing Pose Unity.fbx")!!.inputStream().buffered())
    // FBXReader(File(OS.downloads.unsafeFile, "Female Standing Pose Binary.fbx").inputStream().buffered())
}