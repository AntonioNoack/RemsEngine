package me.anno.mesh

import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.DeflaterOutputStream
import kotlin.math.roundToInt

object MeshWriter {

    fun writeMesh(output: OutputStream, withUVs: Boolean, objects: List<Model>) {

        output.write('M'.toInt())
        output.write('E'.toInt())
        output.write('S'.toInt())
        output.write('H'.toInt())

        // write custom file format
        val wrapper = DeflaterOutputStream(output)
        val dos = DataOutputStream(wrapper)

        dos.writeBoolean(withUVs)

        dos.writeInt(objects.size)
        for (obj in objects) {
            dos.writeUTF(obj.name)
            val meshes = obj.meshes
            dos.writeInt(meshes.size)
            for (mesh in meshes) {

                dos.writeUTF(mesh.material)
                val points = mesh.points
                dos.writeInt(points.size)

                // objects are not that large...
                // therefore compression to a byte should be ok

                if(points.isNotEmpty()){

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

                    fun normalize(x: Float, min: Float, max: Float) =
                        if (max == min) 127
                        else ((x - min) / (max - min + 1e-35f) * 255).roundToInt()

                    for (point in points) {
                        val pos = point.position
                        dos.writeByte(normalize(pos.x, x0, x1))
                        dos.writeByte(normalize(pos.y, y0, y1))
                        dos.writeByte(normalize(pos.z, z0, z1))
                        val nor = point.normal
                        dos.writeByte(normalize(nor.x, -1f, 1f))
                        dos.writeByte(normalize(nor.y, -1f, 1f))
                        dos.writeByte(normalize(nor.z, -1f, 1f))
                        if(withUVs){
                            // idk how well I could compress it...
                            val uv = point.uv
                            dos.writeFloat(uv?.x ?: 0f)
                            dos.writeFloat(uv?.y ?: 0f)
                        }
                    }

                }

            }
        }

        dos.flush()
        wrapper.finish()

    }

}