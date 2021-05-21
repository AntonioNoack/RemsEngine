package me.anno.animation.skeletal

import org.joml.Vector3f
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*

// todo do we need the planes? only if we want to use the premade-animations...
// todo implement animation retargeting
// todo do so by extracting the animation from a source animation...

/**
 * A skeleton hierarchy, whose bones are based on the average position of multiple points
 * this is the case for MakeHuman skeletons. In a currently private project of mine, I implemented
 * the MakeHuman transforms outside of MakeHuman, for my own projects.
 * */
class AnchoredSkeletalHierarchy(
        names: Array<String>,
        parentIndices: IntArray,
        val meshAnchor0: IntArray,
        val meshAnchors: Array<IntArray>): SkeletalHierarchy(names, parentIndices) {

    init {
        if (names.size != parentIndices.size) throw IllegalArgumentException()
        if (names.size != meshAnchors.size) throw IllegalArgumentException()
        if (names.isEmpty()) throw IllegalArgumentException()
        for (i in parentIndices) {
            if (i != 0 && parentIndices[i] >= i) throw IllegalArgumentException("Bones must be sorted from root to rest, ${parentIndices[i]} >= $i")
        }
    }

    override fun removeBones(kept: SortedSet<Int>): AnchoredSkeletalHierarchy {

        if(0 !in kept) throw IllegalArgumentException("Root must be kept")

        val boneCount = names.size

        val keptNames = names.filterIndexed { index, _ -> index in kept }
        val keptParents = kept.map {
            var parent = it
            do {
                parent = parentIndices[parent]
            } while (parent !in kept)
            parent
        }

        val indexMapping = IntArray(boneCount)
        kept.forEachIndexed { index, bone ->
            indexMapping[bone] = index
        }

        val keptParentsRemapped = keptParents.map {
            indexMapping[it]
        }.toIntArray()

        val filteredAnchors = meshAnchors
                .filterIndexed { index, _ -> index in kept }
                .toTypedArray()

        return AnchoredSkeletalHierarchy(keptNames.toTypedArray(), keptParentsRemapped, meshAnchor0, filteredAnchors)

    }

    override fun cutByNames(kept: Set<String>): AnchoredSkeletalHierarchy = removeBones(kept.map { getBone(it) }.toSortedSet())

    override fun calculateBonePositions(pts: FloatArray, bonePositions: FloatArray): FloatArray {
        // return bonePositions
        val average = Vector3f()
        var i = 0
        for (anchor in meshAnchors) {
            average.set(0f)
            for (index in anchor) {
                average.add(pts[index*6], pts[index*6+1], pts[index*6+2])
            }
            average.div(anchor.size.toFloat())
            bonePositions[i++] = average.x
            bonePositions[i++] = average.y
            bonePositions[i++] = average.z
        }
        average.set(0f)
        for (index in meshAnchor0) {
            average.add(pts[index*6], pts[index*6+1], pts[index*6+2])
        }
        average.div(meshAnchor0.size.toFloat())
        bonePositions[i++] = average.x
        bonePositions[i++] = average.y
        bonePositions[i + 0] = average.z
        return bonePositions
    }

    fun write(dos: DataOutputStream) {
        dos.writeByte(names.size - 1) // 1 .. 256
        for (name in names) dos.writeUTF(name)
        for (i in parentIndices) dos.writeByte(i)
        writeAnchor(dos, meshAnchor0)
        for (anchor in meshAnchors) writeAnchor(dos, anchor)
    }

    private fun writeAnchor(dos: DataOutputStream, anchor: IntArray) {
        dos.writeByte(anchor.size)
        for (i in anchor) dos.writeShort(i) // index, so < 65k
    }

    override fun toString() = names.joinToString()

    companion object {

        private fun readAnchor(dis: DataInputStream): IntArray {
            val length = dis.read()
            return IntArray(length) { dis.readUnsignedShort() }
        }

        fun read(dis: DataInputStream): AnchoredSkeletalHierarchy {
            val count = dis.read() + 1
            val names = Array(count) { dis.readUTF() }
            val parentIndices = IntArray(count) { dis.read() }
            val anchor0 = readAnchor(dis)
            val anchors = Array(count) { readAnchor(dis) }
            return AnchoredSkeletalHierarchy(names, parentIndices, anchor0, anchors)
        }

    }


}