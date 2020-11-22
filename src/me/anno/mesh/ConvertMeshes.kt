package me.anno.mesh

import me.anno.mesh.fbx.FBX2Mesh
import me.anno.mesh.obj.Obj2Mesh
import java.io.File
import kotlin.concurrent.thread

object ConvertMeshes {

    @JvmStatic
    fun main(args: Array<String>) {

        val src = File("C:\\Users\\Antonio\\Documents\\IdeaProjects\\Residents2020")//"C:\\Users\\Antonio\\Documents\\IdeaProjects\\ChemicalWars")
        convertMeshes(src,true)

    }

    fun convertMeshes(base: File, ifDstExists: Boolean){
        convertMeshes(File(base, "models"), File(base, "assets/models"), ifDstExists)
    }

    fun convertMeshes(src: File, dst: File, ifDstExists: Boolean){

        for (file in src.listFiles()!!) {
            if(file.isDirectory){
                convertMeshes(File(src, file.name), File(dst, file.name), ifDstExists)
            } else {
                val tags = file.name.replace('.', '-').split('-')
                val newName = tags[0]+".msh"
                val dstFile = File(dst, newName)
                if(ifDstExists || !dstFile.exists()){
                    val withUVs = "u" !in tags
                    when (file.extension) {
                        "obj" -> thread {
                            Obj2Mesh.convertObj(file, dstFile, withUVs)
                        }
                        "fbx" -> thread {
                            FBX2Mesh.convertFBX(file, dstFile, withUVs)
                        }
                    }
                }
            }
        }

    }

}