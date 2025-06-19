package me.anno.image.svg

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import java.io.InputStream

object SVGMeshCache : CacheSection<FileKey, SVGBuffer>("Meshes") {

    operator fun get(file: FileReference, timeout: Long): AsyncCacheData<SVGBuffer> {
        val data = getFileEntry(file, false, timeout, ::loadSVGMeshAsync)
        return data
    }

    private fun loadSVGMeshAsync(key: FileKey, result: AsyncCacheData<SVGBuffer>) {
        key.file.inputStream { input, err ->
            err?.printStackTrace()
            result.value = loadSVGMeshSync(input)
        }
    }

    private fun loadSVGMeshSync(input: InputStream?): SVGBuffer? {
        input ?: return null
        val svg = SVGMesh()
        svg.parse(XMLReader(input.reader()).read() as XMLNode)
        val buffer = svg.buffer ?: return null // may be null if the parsing failed / the svg is blank
        return SVGBuffer(svg.bounds, buffer)
    }
}