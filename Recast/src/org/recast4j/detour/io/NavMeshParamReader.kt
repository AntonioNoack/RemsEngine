package org.recast4j.detour.io

import org.recast4j.detour.NavMeshParams
import java.nio.ByteBuffer

object NavMeshParamReader {
    fun read(bb: ByteBuffer): NavMeshParams {
        val params = NavMeshParams()
        params.origin.set(bb.getFloat(), bb.getFloat(), bb.getFloat())
        params.tileWidth = bb.getFloat()
        params.tileHeight = bb.getFloat()
        bb.getInt() // max tiles
        params.maxPolys = bb.getInt()
        return params
    }
}