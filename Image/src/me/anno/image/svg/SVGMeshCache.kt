package me.anno.image.svg

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.xml.generic.XMLReader
import org.apache.logging.log4j.LogManager
import java.io.InputStream

object SVGMeshCache : CacheSection<FileKey, SVGBuffer>("Meshes") {

    private val LOGGER = LogManager.getLogger(SVGMeshCache::class)

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

    fun loadSVGMeshSync(input: InputStream?): SVGBuffer? {
        if (input == null) {
            LOGGER.warn("Input for SVG was null")
            return null
        }

        val xml = XMLReader(input.reader()).readXMLNode()
        if (xml == null) {
            LOGGER.warn("Failed loading SVG as XML")
            return null
        }

        val svg = SVGMesh()
        svg.parse(xml)
        val buffer = svg.buffer
        if (buffer == null) {
            LOGGER.warn("Failed interpreting SVG from XML")
            return null
        }

        return SVGBuffer(svg.bounds, buffer)
    }
}