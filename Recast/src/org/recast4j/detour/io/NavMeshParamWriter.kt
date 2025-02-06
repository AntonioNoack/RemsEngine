package org.recast4j.detour.io

import org.recast4j.detour.NavMeshParams
import java.io.OutputStream
import java.nio.ByteOrder

object NavMeshParamWriter : DetourWriter() {
    fun write(stream: OutputStream, params: NavMeshParams, order: ByteOrder) {
        write(stream, params.origin, order)
        writeF32(stream, params.tileWidth, order)
        writeF32(stream, params.tileHeight, order)
        writeI32(stream, -1, order) // max tiles
        writeI32(stream, params.maxPolys, order)
    }
}