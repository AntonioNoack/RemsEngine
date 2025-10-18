package me.anno.image.svg

import me.anno.cache.Promise
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.xml.generic.XMLReader
import org.apache.logging.log4j.LogManager
import java.io.InputStream

object SVGMeshCache : CacheSection<FileKey, SVGBuffer>("Meshes") {

    private val LOGGER = LogManager.getLogger(SVGMeshCache::class)

    operator fun get(file: FileReference, timeout: Long): Promise<SVGBuffer> {
        val data = getFileEntry(file, false, timeout, ::loadSVGMeshAsync)
        return data
    }

    private fun loadSVGMeshAsync(key: FileKey, result: Promise<SVGBuffer>) {
        key.file.inputStream { input, err ->
            err?.printStackTrace()
            val mesh = loadSVGMeshSync(input)
            val buffer = mesh?.buffer?.value
            result.value = if (mesh != null && buffer != null) {
                SVGBuffer(mesh.bounds, buffer)
            } else null
        }
    }

    fun loadSVGMeshSync(input: InputStream?): SVGMesh? {
        if (input == null) {
            LOGGER.warn("Input for SVG was null")
            return null
        }

        val xml = XMLReader(input.reader()).readXMLNode()
        if (xml == null) {
            LOGGER.warn("Failed loading SVG as XML")
            return null
        }

        val svg = SVGMesh(xml)
        if (!svg.isValid) {
            LOGGER.warn("Failed interpreting SVG from XML")
            return null
        }

        return svg
    }
}