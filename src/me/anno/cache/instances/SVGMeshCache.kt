package me.anno.cache.instances

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.svg.SVGMesh
import me.anno.io.files.FileReference
import me.anno.io.xml.XMLNode
import me.anno.io.xml.XMLReader
import java.io.InputStream

object SVGMeshCache : CacheSection("Meshes") {
    operator fun get(file: FileReference, timeout: Long, asyncGenerator: Boolean): StaticBuffer? {
        return SVGMeshCache.getEntry(file to "svg", timeout, asyncGenerator) {
            val svg = SVGMesh()
            file.inputStreamSync().use { input: InputStream ->
                svg.parse(XMLReader().parse(input) as XMLNode)
                val buffer = svg.buffer // may be null if the parsing failed / the svg is blank
                if (buffer != null) {
                    buffer.setBounds(svg)
                    buffer
                } else CacheData(0)
            }
        } as? StaticBuffer
    }
}