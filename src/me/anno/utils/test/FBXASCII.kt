package me.anno.utils.test

import me.anno.mesh.fbx.structure.FBXReader
import me.anno.utils.OS
import java.io.File

fun main(){
    FBXReader(File(OS.downloads.file, "Female Standing Pose.fbx").inputStream().buffered())
}