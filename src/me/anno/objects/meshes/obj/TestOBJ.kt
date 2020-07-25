package me.anno.objects.meshes.obj

import me.anno.utils.OS
import java.io.File

fun main(){

    val obj = OBJReader(File(OS.documents, "plane.obj"))
    println(obj.pointsByMaterial)

}