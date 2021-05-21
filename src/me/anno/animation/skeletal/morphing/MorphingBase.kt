package me.anno.animation.skeletal.morphing

import me.anno.animation.skeletal.Skeleton
import me.anno.animation.skeletal.geometry.Face
import me.anno.animation.skeletal.geometry.Point
import me.anno.animation.skeletal.geometry.SectionMarker
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.StaticBuffer
import java.io.DataOutputStream
import kotlin.math.max
import kotlin.math.sqrt

class MorphingBase(
    val points: Array<Point>,
    val faces: Array<Face>
) {

    val markers = ArrayList<SectionMarker>()

    fun getGroup(name: String): IntRange {
        val index0 = markers.indexOfFirst { it.name == name }
        if (index0 < 0) return faces.indices
        val endIndex = if (index0 == markers.size - 1) faces.size else markers[index0 + 1].faceIndex
        return markers[index0].faceIndex until endIndex
    }

    override fun toString() = "MorphingBase(points: ${points.size}, faces: ${faces.size}, markers: $markers)"

    fun createInstance() = FloatArray(points.size * 6)

    /*fun clear() {
        for (pt in points) pt.clear()
    }*/

    fun clear(dst: FloatArray) {
        for ((i, pt) in points.withIndex()) {
            val i0 = i * 6
            dst[i0 + 0] = pt.x
            dst[i0 + 1] = pt.y
            dst[i0 + 2] = pt.z
        }
    }

    fun createSmoothNormals(dst: FloatArray) {
        val points = points
        val faces = faces
        // for (point in points) {
        //     point.normal.set(0f)
        // }
        for (i0 in 0 until points.size * 6 step 6) {
            dst[i0 + 3] = 0f
            dst[i0 + 4] = 0f
            dst[i0 + 5] = 0f
        }
        // update normals by adjacent faces
        for (face in faces) {
            val ai = face.a * 6
            val bi = face.b * 6
            val ci = face.c * 6
            // val normal: Vector3f = (c.position - a.position).cross(b.position - a.position)
            val ax = dst[ai]
            val ay = dst[ai + 1]
            val az = dst[ai + 2]
            val dx1 = dst[ci + 0] - ax
            val dx2 = dst[bi + 0] - ax
            val dy1 = dst[ci + 1] - ay
            val dy2 = dst[bi + 1] - ay
            val dz1 = dst[ci + 2] - az
            val dz2 = dst[bi + 2] - az
            // 23 32 | 31 13 | 12 21
            var nx = dy1 * dz2 - dz1 * dy2
            var ny = dz1 * dx2 - dx1 * dz2
            var nz = dx1 * dy2 - dy1 * dx2
            // .normalize()
            val mul = 1f / max(sqrt(nx * nx + ny * ny + nz * nz), 1e-30f)
            nx *= mul
            ny *= mul
            nz *= mul
            // a.normal.add(normal)
            // b.normal.add(normal)
            // c.normal.add(normal)
            dst[ai + 3] += nx
            dst[ai + 4] += ny
            dst[ai + 5] += nz
            dst[bi + 3] += nx
            dst[bi + 4] += ny
            dst[bi + 5] += nz
            dst[ci + 3] += nx
            dst[ci + 4] += ny
            dst[ci + 5] += nz
        }
        // for (pt in points) {
        //    pt.normal.normalize()
        //}
        for (i0 in dst.indices step 6) {
            val nx = dst[i0 + 3]
            val ny = dst[i0 + 4]
            val nz = dst[i0 + 5]
            val mul = 1f / max(sqrt(nx * nx + ny * ny + nz * nz), 1e-30f)
            dst[i0 + 3] = nx * mul
            dst[i0 + 4] = ny * mul
            dst[i0 + 5] = nz * mul
        }
    }

    fun write(dos: DataOutputStream) {
        dos.writeInt(points.size)
        for (point in points) point.write(dos)
        dos.writeInt(faces.size)
        for (face in faces) {
            dos.writeShort(face.a)
            dos.writeShort(face.b)
            dos.writeShort(face.c)
        }
        dos.writeInt(markers.size)
        for (marker in markers) {
            dos.writeInt(marker.faceIndex)
            dos.writeUTF(marker.name)
        }
    }

    /*val simplestBuffer = lazy {
        val pts = createInstance()
        clear(pts)
        createSmoothNormals(pts)
        val buffer = StaticBuffer(staticAttr, faces.size * 3)
        fun write(i: Int) {
            val pt = points[i]
            val i0 = i * 6
            buffer.put(pts[i0 + 0], pts[i0 + 1], pts[i0 + 2]) // coordinates
            buffer.put(pts[i0 + 3], pts[i0 + 4], pts[i0 + 5]) // normal
            buffer.put(0f,0f,0f) // tangent
            buffer.put(pt.u, pt.v) // uv
        }
        for (face in faces) {
            write(face.a)
            write(face.b)
            write(face.c)
        }
        buffer
    }*/

    fun createSkeletalMesh(skeleton: Skeleton, pts: FloatArray, buffer0: StaticBuffer? = null): StaticBuffer {

        val weights0 = skeleton.weights
        val section = getGroup("body")
        val buffer = buffer0 ?: StaticBuffer(skeletalAttr, (section.last - section.first + 1) * 3)
        val points = points
        val bonesPerVertex = skeleton.weights.bonesPerPoint // should be 4, or this function may not work
        val indices = weights0.indices
        val weights = weights0.weights

        buffer.nioBuffer!!.position(0)

        fun add(i: Int) {
            val pt = points[i]
            val i0 = i * 6
            // buffer.put(pt.position)
            buffer.put(pts[i0 + 0], pts[i0 + 1], pts[i0 + 2])
            // buffer.put(pt.normal)
            buffer.put(pts[i0 + 3], pts[i0 + 4], pts[i0 + 5])
            buffer.put(pt.u, pt.v)
            val baseIndex = i * bonesPerVertex
            for (boneIndex in 0 until bonesPerVertex) {
                val index = baseIndex + boneIndex
                buffer.put(weights[index])
            }
            for (boneIndex in 0 until bonesPerVertex) {
                val index = baseIndex + boneIndex
                buffer.putUByte(indices[index])
            }
        }

        val faces = faces
        for (faceIndex in section) {
            val face = faces[faceIndex]
            add(face.a)
            add(face.b)
            add(face.c)
        }

        return buffer

    }

    companion object {

        val skeletalAttr = listOf(
                Attribute("coords", 3),
                Attribute("normals", 3),
                Attribute("uvs", 2),
                Attribute("weights", 4),
                Attribute("indices", AttributeType.UINT8, 4, true)
        )

        // skeleton weights
        // todo dynamic morph targets for facial animation
        /*fun read(dis: DataInputStream): MorphingBase {
            val pointCount = dis.readInt()
            val points = Array(pointCount) { Point.read(dis) }
            val faceCount = dis.readInt()
            val faces = Array(faceCount) {
                Face(dis.readUnsignedShort(), dis.readUnsignedShort(), dis.readUnsignedShort())
            }
            val markerCount = dis.readInt()
            val base = MorphingBase(points, faces)
            for (i in 0 until markerCount) {
                base.markers.add(SectionMarker(dis.readInt(), dis.readUTF()))
            }
            return base
        }*/
    }

}