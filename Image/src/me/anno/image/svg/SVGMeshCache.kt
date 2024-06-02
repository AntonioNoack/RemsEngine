package me.anno.image.svg

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileReference
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.Warning.unused
import java.io.InputStream

object SVGMeshCache : CacheSection("Meshes") {
    operator fun get(file: FileReference, timeout: Long, asyncGenerator: Boolean): StaticBuffer? {
        val data = getFileEntry(file, false, timeout, asyncGenerator, ::loadSVGMeshAsync) ?: return null
        if (!asyncGenerator) data.waitFor()
        return data.value
    }

    private fun loadSVGMeshAsync(file: FileReference, lastModified: Long): AsyncCacheData<StaticBuffer> {
        unused(lastModified)
        val data = AsyncCacheData<StaticBuffer>()
        file.inputStream { input, err ->
            err?.printStackTrace()
            data.value = loadSVGMeshSync(input)
        }
        return data
    }

    private fun loadSVGMeshSync(input: InputStream?): StaticBuffer? {
        input ?: return null
        val svg = SVGMesh()
        svg.parse(XMLReader().read(input) as XMLNode)
        val buffer = svg.buffer // may be null if the parsing failed / the svg is blank
        buffer?.bounds = svg.bounds
        return buffer
    }
}