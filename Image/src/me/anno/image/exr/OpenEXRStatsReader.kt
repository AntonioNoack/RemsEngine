/*
 *  Copyright (c) 2011 Michael Zucchi
 *
 *  This file is part of ImageZ, a bitmap image editing appliction.
 *
 *  ImageZ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ImageZ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with ImageZ.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.anno.image.exr

import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.io.Streams.read0String
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readNBytes2
import me.anno.io.Streams.skipN
import me.anno.utils.Color.toHexString
import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import org.joml.Vector2i
import java.io.IOException
import java.io.InputStream

/**
 * OpenEXR file format reader
 * @author notzed <- did the basics
 * @author Antonio Noack <- extracted the basics & converted to Kotlin
 */
object OpenEXRStatsReader {

    private val LOGGER = LogManager.getLogger(OpenEXRStatsReader::class)

    /**
     * returns Vector2i or Exception
     * */
    fun findSize(input: InputStream): Any {
        consumeVersion(input)
        readHeaders(input) { name, type, size ->
            if (type == "box2i" && name == "dataWindow" && size >= 4 * 4) {
                return input.readSizeFromBox()
            } else false
        }
        return IOException("Missing size attribute")
    }

    fun findThumbnail(input: InputStream): Image? {
        consumeVersion(input)
        readHeaders(input) { _, type, size ->
            if (type == "preview") {
                val width = input.readLE32()
                val height = input.readLE32()
                assertEquals(size, width * height * 4)

                val buffer = input.readNBytes2(width * height * 4, true) ?: return null
                return ByteImage(width, height, ByteImageFormat.ABGR, buffer)
            } else false
        }
        return null
    }

    fun findChannels(input: InputStream): List<EXRChannel> {
        consumeVersion(input)
        readHeaders(input) { _, type, _ ->
            if (type == "chlist") {
                return input.readChannels()
            } else false
        }
        return emptyList()
    }

    private fun consumeVersion(input: InputStream) {
        val magic = input.readLE32()
        val version = input.readLE32()

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Magic: ${magic.toHexString()}\n")
            val mode = if ((version and 0x200) == 0) "scanlines" else "tiles"
            LOGGER.debug("Version: ${version and 0xff} flags ${version shr 8} ($mode)\n")
        }
    }

    private inline fun readHeaders(
        input: InputStream,
        handleHeader: (name: String, type: String, size: Int) -> Boolean
    ) {
        // read header
        // (attribute name
        // attribute type
        // attribute size
        // attribute value)*
        // 0x00
        var c = input.read()
        check(c >= 0) { "EOF" }

        while (c != 0) {
            val name = input.read0String(c)
            val type = input.read0String()
            val size = input.readLE32()

            LOGGER.debug("header $name type '$type' size $size")
            if (!handleHeader(name, type, size)) {
                input.skipN(size.toLong())
            }
            c = input.read()
        }
    }

    private fun InputStream.readSizeFromBox(): Vector2i {
        val x = readLE32()
        val y = readLE32()
        val width = readLE32() - x + 1
        val height = readLE32() - y + 1
        return Vector2i(width, height)
    }

    private fun InputStream.readChannels(): List<EXRChannel> {
        val channels = ArrayList<EXRChannel>()
        var c = read()
        while (c != 0) {
            val channel = EXRChannel()
            channel.name = read0String(c)
            val pixelTypeId = readLE32()
            channel.pixType = EXRPixelType.entries.getOrNull(pixelTypeId)
                ?: throw IOException("Unknown pixelType: $pixelTypeId")
            channel.linear = read().toByte()
            read()
            read()
            read()
            channel.xSampling = readLE32()
            channel.ySampling = readLE32()
            channels.add(channel)

            debugPrintChannel(channel)

            c = read()

        }
        return channels
    }

    private fun debugPrintChannel(channel: EXRChannel) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" channel name ${channel.name}")
            LOGGER.debug("  type = " + channel.pixType)
            LOGGER.debug("  linear = " + channel.linear)
            LOGGER.debug("  xSampling= " + channel.xSampling)
            LOGGER.debug("  ySampling= " + channel.ySampling)
        }
    }

}