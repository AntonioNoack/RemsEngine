package me.anno.mesh.fbx

import me.anno.mesh.MeshWriter
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object FBX2Mesh {

    fun convertFBX(input: File, output: File, withUVs: Boolean) {
        val i = input.inputStream().buffered()
        val o = output.outputStream().buffered()
        convertFBX(i, o, withUVs)
        i.close()
        o.close()
    }

    fun convertFBX(input: InputStream, output: OutputStream, withUVs: Boolean) {
        val objects = FBXLoader.loadFBX(input)
        MeshWriter.writeMesh(output, withUVs, objects)
    }

}