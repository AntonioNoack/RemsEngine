package me.anno.mesh.usd

object CompressedPaths {



    private class PathBuilderContext(
        val pathIndices: IntArray,
        val elementTokenIndices: IntArray,
        val jumps: IntArray,
        val tokens: List<String>,
        val result: Array<USDNode>,
        val invalidNode: USDNode,
    )

    fun buildDecompressedPaths(
        pathIndices: IntArray,
        elementTokenIndices: IntArray,
        jumps: IntArray,
        tokens: List<String>
    ): Array<USDNode> {
        val invalidNode = USDNode(null, "?", false)
        val result = Array(pathIndices.size) { invalidNode }
        val context = PathBuilderContext(
            pathIndices, elementTokenIndices, jumps, tokens,
            result, invalidNode,
        )
        buildDecompressedPathsI(context, 0, tokens.lastIndex, -1)
        check(invalidNode !in result)
        return result
    }

    private fun buildDecompressedPathsI(
        context: PathBuilderContext,

        curStartIndex: Int,
        curEndIndex: Int,
        parentPathIndex: Int,
    ) {
        var parentPathIndex = parentPathIndex
        println("handling $curStartIndex..$curEndIndex")
        for (thisIndex in curStartIndex..curEndIndex) {
            if (parentPathIndex == -1) {
                check(thisIndex == 0)

                // Root node
                parentPathIndex = thisIndex
                val idx = context.pathIndices[thisIndex]
                check(context.result[idx] == context.invalidNode)
                context.result[idx] = USDNode(null, "", false)

            } else {
                val tokenIndexRaw = context.elementTokenIndices[thisIndex]
                val isPrimPropertyPath = tokenIndexRaw < 0
                val tokenIndex = if (isPrimPropertyPath) -tokenIndexRaw else tokenIndexRaw

                val elemToken = context.tokens[tokenIndex]
                val idx = context.pathIndices[thisIndex]
                check(context.result[idx] == context.invalidNode)

                println("paths[$idx], $isPrimPropertyPath, $elemToken")
                context.result[idx] = USDNode(context.result[parentPathIndex], elemToken, isPrimPropertyPath)
            }

            val jumps = context.jumps
            val hasChild = (jumps[thisIndex] > 0) || (jumps[thisIndex] == -1)
            val hasSibling = jumps[thisIndex] >= 0

            if (hasChild) {
                if (hasSibling) {
                    val siblingIndex = thisIndex + jumps[thisIndex]
                    check(siblingIndex <= jumps.size)

                    // Find subtree end
                    var subtreeIdx = siblingIndex
                    while (subtreeIdx < jumps.size) {
                        val hasChild2 = (jumps[subtreeIdx] > 0) || (jumps[subtreeIdx] == -1)
                        val hasSibling2 = (jumps[subtreeIdx] >= 0)

                        if (hasChild2 || hasSibling2) {
                            subtreeIdx++
                            continue
                        }
                        break
                    }

                    val subtreeEndIdx = subtreeIdx
                    if (subtreeEndIdx >= siblingIndex) {
                        if (jumps[thisIndex] > 1) {
                            val i0 = thisIndex + 1
                            val i1 = siblingIndex - 1
                            val pi = context.pathIndices[thisIndex]
                            buildDecompressedPathsI(context, i0, i1, pi)
                        }

                        return buildDecompressedPathsI(context, siblingIndex, subtreeEndIdx, parentPathIndex)
                    }
                }

                parentPathIndex = context.pathIndices[thisIndex]
            }
        }
    }
}