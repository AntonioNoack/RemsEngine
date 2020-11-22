package me.anno.mesh

import me.anno.utils.Maths.mix
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.InflaterInputStream

object MeshLoader {
    fun load(input: InputStream): List<Model> {
        if (input.read() != 'M'.toInt()) throw RuntimeException()
        if (input.read() != 'E'.toInt()) throw RuntimeException()
        if (input.read() != 'S'.toInt()) throw RuntimeException()
        if (input.read() != 'H'.toInt()) throw RuntimeException()
        val inflater = InflaterInputStream(input)
        val dis = DataInputStream(inflater)
        val withUVs = dis.readBoolean()
        val count = dis.readInt()
        val list = ArrayList<Model>(count)
        for (i in 0 until count) {
            val name = dis.readUTF()
            val count2 = dis.readInt()
            val meshes = ArrayList<Mesh>(count2)
            for (j in 0 until count2) {
                val material = dis.readUTF()
                val count3 = dis.readInt()
                val points = ArrayList<Point>(count3)
                if (count3 != 0) {
                    val x0 = dis.readFloat()
                    val x1 = dis.readFloat()
                    val y0 = dis.readFloat()
                    val y1 = dis.readFloat()
                    val z0 = dis.readFloat()
                    val z1 = dis.readFloat()
                    fun readByte(min: Float, max: Float) = mix(min, max,(dis.readByte().toInt() and 255)/255f)
                    for (k in 0 until count3) {
                        val position = Vector3f(readByte(x0,x1), readByte(y0,y1), readByte(z0,z1))
                        val normal = Vector3f(readByte(-1f,1f), readByte(-1f, 1f), readByte(-1f, 1f))
                        val uv = if(withUVs) Vector2f(dis.readFloat(), dis.readFloat()) else null
                        points += Point(position, normal, uv)
                    }
                }
                meshes += Mesh(material, points)
            }
            list += Model(name, meshes)
        }
        return list
    }
}