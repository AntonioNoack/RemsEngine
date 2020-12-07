package me.anno.mesh.obj

import me.anno.mesh.ModelWriter.writeModels
import me.anno.mesh.obj.ObjLoader.loadObj
import java.io.*

object Obj2Mesh {

    fun convertObj(input: File, output: File, withUVs: Boolean) {
        val i = input.inputStream().buffered()
        val o = output.outputStream().buffered()
        convertObj(i, o, withUVs)
        i.close()
        o.close()
    }

    fun convertObj(input: InputStream, output: OutputStream, withUVs: Boolean) {
        val objects = loadObj(input)
        writeModels(output, withUVs, objects)
    }

}