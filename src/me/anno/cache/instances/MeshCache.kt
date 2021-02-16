package me.anno.cache.instances

import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.svg.SVGMesh
import me.anno.io.xml.XMLElement
import me.anno.io.xml.XMLReader
import me.anno.objects.Video
import java.io.File

object MeshCache : CacheSection("Meshes") {

    fun getMesh(
        file: File,
        key: Any,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: () -> ICacheData
    ) = getEntry(file, false, key, timeout, asyncGenerator, generator)

    fun getSVG(file: File, timeout: Long, asyncGenerator: Boolean): StaticBuffer? {
        return MeshCache.getEntry(file to "svg", timeout, asyncGenerator) {
            val svg = SVGMesh()
            svg.parse(XMLReader.parse(file.inputStream().buffered()) as XMLElement)
            val buffer = svg.buffer!!
            buffer.setBounds(svg)
            buffer
        } as? StaticBuffer
    }

}