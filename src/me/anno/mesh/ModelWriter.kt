package me.anno.mesh

import me.anno.utils.io.Writing.writeVec3
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.DeflaterOutputStream
import kotlin.math.roundToInt

object ModelWriter {

    fun normalize1(x: Float, min: Float, max: Float) =
        if (max == min) 127
        else ((x - min) / (max - min + 1e-35f) * 255).roundToInt()

    fun normalize2(x: Float, min: Float, max: Float) =
        if (max == min) 65535/2
        else ((x - min) / (max - min + 1e-35f) * 65535).roundToInt()

    fun writeMesh(dos: DataOutputStream, withUVs: Boolean, mesh: Mesh){

        dos.writeUTF(mesh.material)

        val points = mesh.points
        dos.writeInt(points?.size ?: 0)

        // objects are not that large...
        // therefore compression to a byte should be ok

        if (points != null && points.isNotEmpty()) {

            val position = points.map { it.position }
            val x0 = position.minBy { it.x }!!.x
            val x1 = position.maxBy { it.x }!!.x
            val y0 = position.minBy { it.y }!!.y
            val y1 = position.maxBy { it.y }!!.y
            val z0 = position.minBy { it.z }!!.z
            val z1 = position.maxBy { it.z }!!.z

            dos.writeFloat(x0)
            dos.writeFloat(x1)
            dos.writeFloat(y0)
            dos.writeFloat(y1)
            dos.writeFloat(z0)
            dos.writeFloat(z1)

            for (point in points) {
                val pos = point.position
                dos.writeShort(normalize2(pos.x, x0, x1))
                dos.writeShort(normalize2(pos.y, y0, y1))
                dos.writeShort(normalize2(pos.z, z0, z1))
                val nor = point.normal
                dos.writeByte(normalize1(nor.x, -1f, 1f))
                dos.writeByte(normalize1(nor.y, -1f, 1f))
                dos.writeByte(normalize1(nor.z, -1f, 1f))
                if (withUVs) {
                    // idk how well I could compress it...
                    val uv = point.uv
                    dos.writeFloat(uv?.x ?: 0f)
                    dos.writeFloat(uv?.y ?: 0f)
                }
            }
        }

        val lines = mesh.lines
        dos.writeInt(lines?.size ?: 0)
        if(lines != null && lines.isNotEmpty()){

            val positions = lines.map { it.a } + lines.map { it.b }
            val x0 = positions.minBy { it.x() }!!.x()
            val x1 = positions.maxBy { it.x() }!!.x()
            val y0 = positions.minBy { it.y() }!!.y()
            val y1 = positions.maxBy { it.y() }!!.y()
            val z0 = positions.minBy { it.z() }!!.z()
            val z1 = positions.maxBy { it.z() }!!.z()

            dos.writeFloat(x0)
            dos.writeFloat(x1)
            dos.writeFloat(y0)
            dos.writeFloat(y1)
            dos.writeFloat(z0)
            dos.writeFloat(z1)

            for (line in lines) {
                val a = line.a
                val b = line.b
                dos.writeShort(normalize2(a.x(), x0, x1))
                dos.writeShort(normalize2(a.y(), y0, y1))
                dos.writeShort(normalize2(a.z(), z0, z1))
                dos.writeShort(normalize2(b.x(), x0, x1))
                dos.writeShort(normalize2(b.y(), y0, y1))
                dos.writeShort(normalize2(b.z(), z0, z1))
            }

        }

    }

    fun writeModel(dos: DataOutputStream, withUVs: Boolean, model: Model) {

        dos.writeUTF(model.name)
        dos.writeVec3(model.localTranslation)
        dos.writeVec3(model.localRotation)
        dos.writeVec3(model.localScale)
        dos.writeVec3(model.pivot)

        val meshes = model.meshes
        dos.writeInt(meshes.size)

        for (mesh in meshes) {
            writeMesh(dos, withUVs, mesh)
        }
    }

    fun writeModels(output: OutputStream, withUVs: Boolean, models: List<Model>) {

        output.write('M'.toInt())
        output.write('E'.toInt())
        output.write('S'.toInt())
        output.write('H'.toInt())

        // write custom file format
        val wrapper = DeflaterOutputStream(output)
        val dos = DataOutputStream(wrapper)

        dos.writeBoolean(withUVs)
        dos.writeInt(models.size)
        for (obj in models) {
            writeModel(dos, withUVs, obj)
        }

        dos.flush()
        wrapper.finish()

    }

}