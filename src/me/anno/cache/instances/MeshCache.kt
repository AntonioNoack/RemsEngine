package me.anno.cache.instances

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.svg.SVGMesh
import me.anno.io.FileReference
import me.anno.io.xml.XMLElement
import me.anno.io.xml.XMLReader

object MeshCache : CacheSection("Meshes") {

    fun getMesh(
        file: FileReference,
        key: Any,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: (Any) -> ICacheData
    ) = getEntry(file to key, timeout, asyncGenerator, generator)

    fun getSVG(file: FileReference, timeout: Long, asyncGenerator: Boolean): StaticBuffer? {
        return MeshCache.getEntry(file to "svg", timeout, asyncGenerator) {
            val svg = SVGMesh()
            svg.parse(XMLReader.parse(file.file.inputStream().buffered()) as XMLElement)
            val buffer = svg.buffer // may be null if the parsing failed / the svg is blank
            if (buffer != null) {
                buffer.setBounds(svg)
                buffer
            } else CacheData(0)
        } as? StaticBuffer
    }

}