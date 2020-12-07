package me.anno.mesh.fbx

import me.anno.mesh.ModelWriter
import me.anno.mesh.Model
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object FBX2Mesh {

    fun convertFBX(input: File, output: File, withUVs: Boolean) {
        val i = input.inputStream().buffered()
        val o = output.outputStream().buffered()
        val models = convertFBX(i, o, withUVs)
        i.close()
        o.close()
        if(models.isEmpty()) LOGGER.warn("No models found inside $input")
    }

    fun convertFBX(input: InputStream, output: OutputStream, withUVs: Boolean): List<Model> {
        val objects = FBXLoader.loadFBX(input)
        objects.forEach { it.scale(0.01f) } // because FBX has a 1cm scale
        ModelWriter.writeModels(output, withUVs, objects)
        return objects
    }

    private val LOGGER = LogManager.getLogger(FBX2Mesh::class)

}