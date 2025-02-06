package org.recast4j.detour.io

import org.recast4j.detour.NavMeshParams
import java.io.OutputStream
import java.nio.ByteOrder

object NavMeshParamWriter : DetourWriter() {
    fun write(stream: OutputStream, params: NavMeshParams, order: ByteOrder) {
        write(stream, params.origin, order)
        write(stream, params.tileWidth, order)
        write(stream, params.tileHeight, order)
        write(stream, -1, order) // max tiles
        write(stream, params.maxPolys, order)
    }
}