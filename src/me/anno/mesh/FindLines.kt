package me.anno.mesh

object FindLines {

    fun isLine(ab: Boolean, bc: Boolean, ca: Boolean): Boolean {
        return (ab || ca || bc) && (!ab || !ca || !bc)
    }

    fun isLine(a: Int, b: Int, c: Int): Boolean {
        return isLine(a == b, b == c, c == a)
    }

    fun isLine(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float
    ): Boolean {
        val ab = ax == bx && ay == by && az == bz
        val bc = bx == cx && by == cy && bz == cz
        val ca = cx == ax && cy == ay && cz == az
        return isLine(ab, bc, ca)
    }

    fun getAllLines(indices: IntArray?, positions: FloatArray?, old: IntArray? = null): IntArray? {
        // todo remove duplicate lines? (from neighbor triangles)
        var lineCount = 0
        if (indices == null) {
            // compare vertices
            positions ?: return null
            var i = 0
            while (i < positions.size) {
                lineCount += if (isLine(
                        positions[i++], positions[i++], positions[i++],
                        positions[i++], positions[i++], positions[i++],
                        positions[i++], positions[i++], positions[i++]
                    )
                ) 1 else 3
            }
            return if (lineCount > 0) {
                val indexCount = lineCount * 2
                val lineIndices = if (old != null && old.size == indexCount) old else IntArray(indexCount)
                var j = 0
                i = 0
                while (i < positions.size) {
                    val ax = positions[i++]
                    val ay = positions[i++]
                    val az = positions[i++]
                    val bx = positions[i++]
                    val by = positions[i++]
                    val bz = positions[i++]
                    val cx = positions[i++]
                    val cy = positions[i++]
                    val cz = positions[i++]
                    val ab = ax == bx && ay == by && az == bz
                    val bc = bx == cx && by == cy && bz == cz
                    val ca = cx == ax && cy == ay && cz == az
                    val i3 = i / 3
                    if (isLine(ab, bc, ca)) {
                        lineIndices[j++] = i3
                        lineIndices[j++] = i3 + if (ab) 2 else 1
                    } else {
                        lineIndices[j++] = i3
                        lineIndices[j++] = i3 + 1
                        lineIndices[j++] = i3 + 1
                        lineIndices[j++] = i3 + 2
                        lineIndices[j++] = i3 + 2
                        lineIndices[j++] = i3
                    }
                }
                return lineIndices
            } else null
        } else {
            // compare indices
            for (i in indices.indices step 3) {
                val a = indices[i]
                val b = indices[i + 1]
                val c = indices[i + 2]
                lineCount += if (isLine(a, b, c)) 1 else 3
            }
            return if (lineCount > 0) {
                val indexCount = lineCount * 2
                val lineIndices = if (old != null && old.size == indexCount) old else IntArray(indexCount)
                var j = 0
                for (i in indices.indices step 3) {
                    val a = indices[i]
                    val b = indices[i + 1]
                    val c = indices[i + 2]
                    if (isLine(a, b, c)) {
                        lineIndices[j++] = a
                        lineIndices[j++] = if (a == b) c else b
                    } else {
                        lineIndices[j++] = a
                        lineIndices[j++] = b
                        lineIndices[j++] = b
                        lineIndices[j++] = c
                        lineIndices[j++] = c
                        lineIndices[j++] = a
                    }
                }
                lineIndices
            } else null
        }
    }

    fun findLines(indices: IntArray?, positions: FloatArray?): IntArray? {
        var lineCount = 0
        if (indices == null) {
            // compare vertices
            positions ?: return null
            var i = 0
            while (i < positions.size) {
                if (isLine(
                        positions[i++], positions[i++], positions[i++],
                        positions[i++], positions[i++], positions[i++],
                        positions[i++], positions[i++], positions[i++]
                    )
                ) lineCount++
            }
            return if (lineCount > 0) {
                val lineIndices = IntArray(lineCount * 2)
                var j = 0
                i = 0
                while (i < positions.size) {
                    val ax = positions[i++]
                    val ay = positions[i++]
                    val az = positions[i++]
                    val bx = positions[i++]
                    val by = positions[i++]
                    val bz = positions[i++]
                    val cx = positions[i++]
                    val cy = positions[i++]
                    val cz = positions[i++]
                    val ab = ax == bx && ay == by && az == bz
                    val bc = bx == cx && by == cy && bz == cz
                    val ca = cx == ax && cy == ay && cz == az
                    if (isLine(ab, bc, ca)) {
                        lineIndices[j++] = i / 3
                        lineIndices[j++] = if (ab) (i / 3) + 2 else (i / 3) + 1
                    }
                }
                return lineIndices
            } else null
        } else {
            // compare indices
            for (i in indices.indices step 3) {
                val a = indices[i]
                val b = indices[i + 1]
                val c = indices[i + 2]
                if (isLine(a, b, c)) {
                    lineCount++
                }
            }
            return if (lineCount > 0) {
                val lineIndices = IntArray(lineCount * 2)
                var j = 0
                for (i in indices.indices step 3) {
                    val a = indices[i]
                    val b = indices[i + 1]
                    val c = indices[i + 2]
                    if (isLine(a, b, c)) {
                        lineIndices[j++] = a
                        lineIndices[j++] = if (a == b) c else b
                    }
                }
                lineIndices
            } else null
        }
    }

}