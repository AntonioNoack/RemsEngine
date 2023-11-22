package me.anno.image.svg

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import java.io.InputStream

object SVGMeshCache : CacheSection("Meshes") {
    operator fun get(file: FileReference, timeout: Long, asyncGenerator: Boolean): StaticBuffer? {
        return SVGMeshCache.getEntry(file to "svg", timeout, asyncGenerator) {
            val svg = SVGMesh()
            file.inputStreamSync().use { input: InputStream ->
                svg.parse(XMLReader().read(input) as XMLNode)
                val buffer = svg.buffer // may be null if the parsing failed / the svg is blank
                if (buffer != null) {
                    buffer.setBounds(svg)
                    buffer
                } else CacheData(0)
            }
        } as? StaticBuffer
    }
}