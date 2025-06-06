package me.anno.image.svg

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.async.Callback
import java.io.InputStream

object SVGMeshCache : CacheSection("Meshes") {

    fun getAsync(file: FileReference, timeout: Long, callback: Callback<AsyncCacheData<StaticBuffer>>) {
        getFileEntryAsync(file, false, timeout, true, ::loadSVGMeshAsync, callback)
    }

    operator fun get(file: FileReference, timeout: Long, asyncGenerator: Boolean): StaticBuffer? {
        val data = getFileEntry(file, false, timeout, asyncGenerator, ::loadSVGMeshAsync) ?: return null
        if (!asyncGenerator) data.waitFor()
        return data.value
    }

    private fun loadSVGMeshAsync(key: FileKey): AsyncCacheData<StaticBuffer> {
        val data = AsyncCacheData<StaticBuffer>()
        key.file.inputStream { input, err ->
            err?.printStackTrace()
            data.value = loadSVGMeshSync(input)
        }
        return data
    }

    private fun loadSVGMeshSync(input: InputStream?): StaticBuffer? {
        input ?: return null
        val svg = SVGMesh()
        svg.parse(XMLReader(input.reader()).read() as XMLNode)
        val buffer = svg.buffer // may be null if the parsing failed / the svg is blank
        buffer?.bounds = svg.bounds
        return buffer
    }
}