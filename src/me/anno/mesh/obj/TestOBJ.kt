package me.anno.mesh.obj

import me.anno.utils.LOGGER
import me.anno.utils.OS
import java.io.File

fun main(){

    val obj = OBJReader(File(OS.documents.file, "plane.obj"))
    LOGGER.info(obj.pointsByMaterial.toString())

}