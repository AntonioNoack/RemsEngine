package me.anno.cache.instances

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.ICacheData
import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.svg.SVGMesh
import me.anno.io.files.FileReference
import me.anno.io.xml.XMLElement
import me.anno.io.xml.XMLReader

@Deprecated("Please use MeshCache and ECS if possible")
object OldMeshCache : CacheSection("Meshes") {

    fun getMesh(
        file: FileReference,
        key: Any,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: (Any, Any) -> ICacheData
    ) = getEntry(file, key, timeout, asyncGenerator, generator)

    fun getSVG(file: FileReference, timeout: Long, asyncGenerator: Boolean): StaticBuffer? {
        return OldMeshCache.getEntry(file to "svg", timeout, asyncGenerator) {
            val svg = SVGMesh()
            svg.parse(XMLReader.parse(file.inputStream()) as XMLElement)
            val buffer = svg.buffer // may be null if the parsing failed / the svg is blank
            if (buffer != null) {
                buffer.setBounds(svg)
                buffer
            } else CacheData(0)
        } as? StaticBuffer
    }

}