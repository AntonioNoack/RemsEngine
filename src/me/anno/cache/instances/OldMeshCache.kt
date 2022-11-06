package me.anno.cache.instances

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.svg.SVGMesh
import me.anno.io.files.FileReference
import me.anno.io.xml.XMLNode
import me.anno.io.xml.XMLReader

@Deprecated("Please use MeshCache and ECS if possible")
object OldMeshCache : CacheSection("Meshes") {

    fun getSVG(file: FileReference, timeout: Long, asyncGenerator: Boolean): StaticBuffer? {
        return OldMeshCache.getEntry(file to "svg", timeout, asyncGenerator) {
            val svg = SVGMesh()
            file.inputStreamSync().use {
                svg.parse(XMLReader.parse(it) as XMLNode)
                val buffer = svg.buffer // may be null if the parsing failed / the svg is blank
                if (buffer != null) {
                    buffer.setBounds(svg)
                    buffer
                } else CacheData(0)
            }
        } as? StaticBuffer
    }

}