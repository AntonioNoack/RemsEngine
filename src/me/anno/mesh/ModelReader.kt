package me.anno.mesh

import me.anno.utils.maths.Maths.mix
import me.anno.utils.io.Writing.readVec3
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.InflaterInputStream

object ModelReader {

    fun readMesh(dis: DataInputStream, withUVs: Boolean): Mesh {
        val material = dis.readUTF()
        val pointCount = dis.readInt()
        val points = ArrayList<Point>(pointCount)
        fun read1(min: Float, max: Float) = mix(min, max, (dis.readUnsignedByte() and 255) / 255f)
        fun read2(min: Float, max: Float) = mix(min, max, (dis.readUnsignedShort() and 65535) / 65535f)
        if (pointCount != 0) {
            val x0 = dis.readFloat()
            val x1 = dis.readFloat()
            val y0 = dis.readFloat()
            val y1 = dis.readFloat()
            val z0 = dis.readFloat()
            val z1 = dis.readFloat()
            for (k in 0 until pointCount) {
                val position = Vector3f(read2(x0, x1), read2(y0, y1), read2(z0, z1))
                val normal = Vector3f(read1(-1f, 1f), read1(-1f, 1f), read1(-1f, 1f))
                val uv = if (withUVs) Vector2f(dis.readFloat(), dis.readFloat()) else null
                points += Point(position, normal, uv)
            }
        }
        val lineCount = dis.readInt()
        val lines = ArrayList<Line>(lineCount)
        if (lineCount != 0) {
            val x0 = dis.readFloat()
            val x1 = dis.readFloat()
            val y0 = dis.readFloat()
            val y1 = dis.readFloat()
            val z0 = dis.readFloat()
            val z1 = dis.readFloat()
            for (k in 0 until pointCount) {
                val p0 = Vector3f(read2(x0, x1), read2(y0, y1), read2(z0, z1))
                val p1 = Vector3f(read2(x0, x1), read2(y0, y1), read2(z0, z1))
                lines += Line(p0, p1)
            }
        }
        return Mesh(material, points, lines)
    }

    fun readModel(dis: DataInputStream, withUVs: Boolean): Model {

        val name = dis.readUTF()
        val localTranslation = dis.readVec3()
        val localRotation = dis.readVec3()
        val localScale = dis.readVec3()
        val pivot = dis.readVec3()

        val count = dis.readInt()
        val meshes = ArrayList<Mesh>(count)
        for (j in 0 until count) {
            meshes += readMesh(dis, withUVs)
        }

        return Model(name, meshes).apply {
            this.localTranslation.set(localTranslation)
            this.localRotation.set(localRotation)
            this.localScale.set(localScale)
            this.pivot.set(pivot)
        }

    }

    fun readModels(input: InputStream): List<Model> {
        if (input.read() != 'M'.code) throw RuntimeException()
        if (input.read() != 'E'.code) throw RuntimeException()
        if (input.read() != 'S'.code) throw RuntimeException()
        if (input.read() != 'H'.code) throw RuntimeException()
        val inflater = InflaterInputStream(input)
        val dis = DataInputStream(inflater)
        val withUVs = dis.readBoolean()
        val count = dis.readInt()
        val list = ArrayList<Model>(count)
        for (i in 0 until count) {
            list += readModel(dis, withUVs)
        }
        return list
    }

    fun readMeshes(input: InputStream): List<Mesh> {
        val models = readModels(input)
        val list = ArrayList<Mesh>(models.sumOf { it.meshes.size })
        for (model in models) {
            if (model.localTranslation.length() > 1e-7f) {
                model.meshes.forEach { mesh ->
                    mesh.translate(model.localTranslation)
                }
            }
            list += model.meshes
        }
        return list
    }

}