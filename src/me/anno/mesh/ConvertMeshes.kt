package me.anno.mesh

import me.anno.mesh.ModelWriter.writeModels
import me.anno.mesh.fbx.FBXLoader.loadFBX
import me.anno.mesh.obj.ObjLoader.loadObj
import me.anno.utils.FileHelper.use
import java.io.File
import java.io.InputStream
import kotlin.concurrent.thread

object ConvertMeshes {

    @JvmStatic
    fun main(args: Array<String>) {

        // val src = File("C:\\Users\\Antonio\\Documents\\IdeaProjects\\Residents2020")//"C:\\Users\\Antonio\\Documents\\IdeaProjects\\ChemicalWars")
        // convertMeshes(src, true)
        convertMeshes(File("C:\\Users\\Antonio\\Documents\\IdeaProjects\\HomeDesigner"), true)

    }

    fun convertMeshes(base: File, ifDstExists: Boolean) {
        convertMeshes(File(base, "models"), File(base, "assets/models"), ifDstExists)
    }

    fun convertMeshes(src: File, dst: File, ifDstExists: Boolean) {
        if (src.isDirectory) {
            for (file in src.listFiles()!!) {
                if (file.isDirectory) {
                    convertMeshes(File(src, file.name), File(dst, file.name), ifDstExists)
                } else {
                    convertMesh(file, dst, ifDstExists)
                }
            }
        } else {
            convertMesh(src, dst.parentFile, ifDstExists)
        }
    }

    fun convertMesh(file: File, dst: File, ifDstExists: Boolean) {
        val tags = file.name.replace('.', '-').split('-')
        val newName = tags[0] + ".msh"
        val dstFile = File(dst, newName)
        if (ifDstExists || !dstFile.exists()) {
            when (file.extension) {
                "obj", "fbx" -> {
                    //thread {
                        convertMesh(file.extension, file.inputStream().buffered(), dstFile, tags)
                   // }
                }
            }
        }
    }

    fun convertMesh(type: String, input: InputStream, dstFile: File, tags: List<String>) {
        val models = when (type) {
            "obj" -> {
                dstFile.parentFile?.mkdirs()
                loadObj(input)
            }
            "fbx" -> {
                dstFile.parentFile?.mkdirs()
                loadFBX(input).apply {
                    forEach { it.scale(0.01f) }
                }
            }
            else -> throw RuntimeException()
        }
        val flipV = "v" in tags
        if(flipV){
            models.forEach {
                it.flipV()
            }
        }
        val switchYZ = "yz" in tags
        if (switchYZ) {
            models.forEach { model ->
                model.switchYZ()
            }
        }
        val withUVs = "u" !in tags
        use(dstFile.outputStream().buffered()){
            writeModels(it, withUVs, models)
        }
        // ModelReader.readModels(dstFile.inputStream().buffered())
    }

}