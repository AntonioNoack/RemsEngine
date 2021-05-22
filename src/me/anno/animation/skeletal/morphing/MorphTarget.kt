package me.anno.animation.skeletal.morphing

import me.anno.animation.skeletal.geometry.Point.Companion.mapXYZ
import me.anno.animation.skeletal.geometry.Point.Companion.unmapXYZ
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs
import kotlin.math.min

class MorphTarget(
    private val indices: IntArray,
    private val deltas: FloatArray
) {

    init {
        // if (indices.isEmpty()) throw IllegalArgumentException("Target must not be empty!")
        if (indices.size * 3 != deltas.size) throw IllegalArgumentException("Incompatible sizes ${indices.size}, ${deltas.size}")
        for (i in 1 until indices.size) {
            if (indices[i - 1] >= indices[i]) throw IllegalArgumentException("Indices need to be sorted!")
        }
    }


    val minIndex = indices.firstOrNull() ?: 0
    val maxIndex = indices.lastOrNull() ?: 0

    override fun toString() =
        "MorphTarget(size: ${indices.size}, min: ${indices.minOrNull()}, max: ${indices.maxOrNull()})"

    fun withWeight(weight: Float) = WeightedTarget(this, weight)

    fun apply(points: FloatArray, amount: Float) {
        if (abs(amount) < minWeight) return
        for ((index, vertexIndex) in indices.withIndex()) {
            // points[vertexIndex].position.add(amount * deltas[i0], amount * deltas[i0 + 1], amount * deltas[i0 + 2])
            val i0 = index * 3
            val i1 = vertexIndex * 6
            points[i1 + 0] += amount * deltas[i0 + 0]
            points[i1 + 1] += amount * deltas[i0 + 1]
            points[i1 + 2] += amount * deltas[i0 + 2]
        }
    }

    fun write(dos: DataOutputStream) {
        dos.writeInt(indices.size)
        var lastIndex = 0
        for (i in indices) {
            val delta = i - lastIndex
            dos.writeShort(delta)
            lastIndex = i + 1
        }
        for (delta in deltas) dos.writeShort(mapXYZ(delta))
    }

    fun createSingularGPUMesh(mesh: MorphingBase, pts: FloatArray, buffer0: StaticBuffer? = null): StaticBuffer {
        val section = mesh.getGroup("body")
        val buffer = buffer0 ?: StaticBuffer(singularAttr, (section.last - section.first + 1) * 3)
        val points = mesh.points
        val addedCoordinates = FloatArray(points.size * 3)
        for ((i0, i) in indices.withIndex()) {
            val ai = i0 * 3
            val bi = i * 3
            addedCoordinates[bi] = deltas[ai]
            addedCoordinates[bi + 1] = deltas[ai + 1]
            addedCoordinates[bi + 2] = deltas[ai + 2]
        }
        fun add(i: Int) {
            val pt = points[i]
            val i0 = i * 6
            // buffer.put(pt.position)
            buffer.put(pts[i0 + 0], pts[i0 + 1], pts[i0 + 2])
            val ai = i * 3
            buffer.put(addedCoordinates[ai], addedCoordinates[ai + 1], addedCoordinates[ai + 2])
            // buffer.put(pt.normal)
            buffer.put(pts[i0 + 3], pts[i0 + 4], pts[i0 + 5])
            buffer.put(pt.u, pt.v)
        }

        val faces = mesh.faces
        for (fi in section) {
            val face = faces[fi]
            add(face.a)
            add(face.b)
            add(face.c)
        }
        return buffer
    }

    companion object {

        val minWeight = 1e-3f

        val nullTarget = MorphTarget(IntArray(0), FloatArray(0))

        val singularAttr = listOf(
            Attribute("target0", 3),
            Attribute("target1", 3),
            Attribute("normals", 3),
            Attribute("uvs", 2)
        )

        /*val singularTest = lazy {
            val base = baseModel.value
            val points = base.createInstance()
            base.clear(points)
            base.createSmoothNormals(points)
            val target = africanGirlInternal.value
            target.createSingularGPUMesh(base, points, null)
        }*/

        fun combineTargets(targets0: List<WeightedTarget>): MorphTarget {
            val targets = targets0
                .filter { !it.isUseless }
                .sortedBy { it.weight } // for better addition? not really costly
            if (targets.isEmpty()) return nullTarget
            val minIndex = targets.minOf { it.target.minIndex }
            val maxIndex = targets.maxOf { it.target.maxIndex } + 1
            val indexList = BooleanArray(maxIndex - minIndex)
            var indexCount = maxIndex - minIndex
            if (targets.any { it.target.indices.size == indexCount }) {
                // all indices
                for (i in indexList.indices) indexList[i] = true
            } else {
                for (target in targets) {
                    for (index in target.target.indices) {
                        indexList[index - minIndex] = true
                    }
                }
                indexCount = indexList.count { it }
            }
            targets.forEach { it.nextIndex = minIndex }
            val compactIndices = IntArray(indexCount)
            val deltas = FloatArray(indexCount * 3)
            var compactIndex = 0
            for (index0 in indexList.indices) {
                if (indexList[index0]) {
                    val index = index0 + minIndex
                    // add the values
                    var dx = 0f
                    var dy = 0f
                    var dz = 0f
                    for (wTarget in targets) {
                        val target = wTarget.target
                        val nextIndex = wTarget.nextIndex
                        if (target.indices[nextIndex] == index) {
                            // found <3
                            val weight = wTarget.weight
                            val i0 = nextIndex * 3
                            dx += weight * target.deltas[i0]
                            dy += weight * target.deltas[i0 + 1]
                            dz += weight * target.deltas[i0 + 2]
                            wTarget.nextIndex = min(target.indices.lastIndex, nextIndex + 1)
                        }// else not in this target
                    }
                    compactIndices[compactIndex] = index
                    val deltaIndex = compactIndex * 3
                    deltas[deltaIndex] = dx
                    deltas[deltaIndex + 1] = dy
                    deltas[deltaIndex + 2] = dz
                    compactIndex++
                }
            }
            return MorphTarget(compactIndices, deltas)
        }

        fun read(dis: DataInputStream): MorphTarget {
            val count = dis.readInt()
            var lastIndex = 0
            val indices = IntArray(count)
            for (i in 0 until count) {
                val delta = dis.readUnsignedShort()
                indices[i] = lastIndex + delta
                lastIndex += delta + 1
            }
            val deltas = FloatArray(count * 3) { unmapXYZ(dis.readUnsignedShort()) }
            return MorphTarget(indices, deltas)
        }
    }

}