package me.anno.cache.instances

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.ICacheData
import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.svg.SVGMesh
import me.anno.io.files.FileReference
import me.anno.io.xml.XMLElement
import me.anno.io.xml.XMLReader

@Deprecated("Please use MeshCache and ECS if possible")
object OldMeshCache : CacheSection("Meshes") {

    fun getSVG(file: FileReference, timeout: Long, asyncGenerator: Boolean): StaticBuffer? {
        return OldMeshCache.getEntry(file to "svg", timeout, asyncGenerator) {
            val svg = SVGMesh()
            svg.parse(XMLReader.parse(file.inputStreamSync()) as XMLElement)
            val buffer = svg.buffer // may be null if the parsing failed / the svg is blank
            if (buffer != null) {
                buffer.setBounds(svg)
                buffer
            } else CacheData(0)
        } as? StaticBuffer
    }

    fun getSVG2(file: FileReference, timeout: Long, asyncGenerator: Boolean): Mesh? {
        val data = OldMeshCache.getEntry(file to "svg2", timeout, asyncGenerator) {
            val svg = SVGMesh()
            svg.parse(XMLReader.parse(file.inputStreamSync()) as XMLElement)
            val buffer = svg.mesh // may be null if the parsing failed / the svg is blank
            if (buffer != null) {
                // buffer.setBounds(svg)
                CacheData(buffer)
            } else CacheData(0)
        } as? CacheData<*>
        return data?.value as? Mesh
    }

}