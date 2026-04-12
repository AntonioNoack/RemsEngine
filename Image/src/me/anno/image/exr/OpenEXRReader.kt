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

import me.anno.image.raw.FloatImage
import me.anno.io.Streams.readBE16
import me.anno.io.Streams.readLE32F
import me.anno.io.Streams.skipN
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Floats.float16ToFloat32
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.InflaterInputStream
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import kotlin.math.min

/**
 * OpenEXR file format reader
 * @author notzed <- did the basics
 * @author Antonio Noack <- made it work
 */
object OpenEXRReader {

    // https://github.com/mitsuba-renderer/openexr/blob/master/OpenEXR/exrheader/main.cpp
    enum class Compression {
        NO,
        RLE,
        ZIPS,
        ZIP,
        PIZ,
        PXR24,
        B44,
        B44A,
        DWA_SMALL,
        DWA_MEDIUM,
    }

    enum class LineOrder {
        ASCENDING,
        DESCENDING,
        RANDOM,
    }

    enum class PixelType {
        UINT32,
        HALF,
        FLOAT,
    }

    enum class LevelMode {
        SINGLE,
        MIPMAPS,
        RIPMAPS, // mipmaps in one texture??
    }

    enum class RoundingMode {
        DOWN,
        UP,
    }

    // Maps compression type to # of scan lines per block
    private val compressionToSPB = intArrayOf(1, 1, 1, 16, 32, 16, 32, 32)

    fun readImage(input: InputStream): FloatImage {
        val fis = OpenEXRInputStream(input)

        // high level layout
        // magic no
        // version
        // header
        // line offset table
        // scan line blocks
        var dataSize: Rectangle? = null
        var compression = Compression.NO
        var channels: MutableList<OpenEXRInputStream.ChannelAttr>? = null

        val magic = fis.readInt()
        val version = fis.readInt()

        System.out.printf("Magic: %08x\n", magic)
        System.out.printf(
            "Version: %02x flags %06x (%s)\n",
            version and 0xff,
            version shr 8,
            if ((version and 0x200) == 0) "scanlines" else "tiles"
        )

        // read header
        // (attribute name
        // attribute type
        // attribute size
        // attribute value)*
        // 0x00
        var c = fis.readByte().toInt() and 0xff
        val attName = StringBuilder()
        // StringBuilder attType = new StringBuilder();
        val blah = ByteArray(1024)
        while (c != 0) {
            attName.setLength(0)
            do {
                attName.append(c.toChar())
                c = fis.readByte().toInt()
            } while (c != 0)
            val atype = fis.readStringZ()
            val aname = attName.toString()

            var attSize = fis.readInt()

            //fis.skipBytes(attSize)
            println("header $attName type '$atype' size $attSize")
            when (atype) {
                "chlist" -> {
                    channels = fis.readChannels()

                    //for (ChannelAttr ch : channels) {
                    //	System.out.println("channel: " + ch.toString());
                    //}
                }
                "compression" -> {
                    val compressionId = fis.readByte().toInt() and 0xff
                    compression = Compression.entries.getOrNull(compressionId)
                        ?: throw IllegalStateException("Unknown compression: $compressionId")

                    println(" compression = $compression\n")
                }
                "box2i" -> {
                    val box = fis.readbox2i()
                    if (aname == "dataWindow") {
                        dataSize = box
                    }
                    println(" box = $box")
                }
                "preview" -> {
                    // todo previews are nice, use them as thumbnails...
                    val width = fis.readInt()
                    val height = fis.readInt()

                    if (false) {
                        val preview = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
                        val data = (preview.raster.getDataBuffer() as DataBufferByte).getData()

                        fis.read(data)
                        val frame = JFrame()
                        val label = JLabel(ImageIcon(preview))

                        frame.add(label)
                        frame.pack()
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
                        frame.setTitle("OpenEXR preview")
                        frame.isVisible = true
                    } else {
                        fis.skipN(width * height * 4L)
                    }
                }
                else -> {
                    while (attSize > 0) {
                        val read = min(attSize, 1024)
                        attSize -= fis.read(blah, 0, read)
                    }
                }
            }
            c = fis.readByte().toInt() and 0xff
        }

        // next: line offset table

        // a bit silly, need to know the compression to know how many scanlines/block
        val scanPerBlock = compressionToSPB[compression.ordinal]
        val offsetCount = (dataSize!!.height + (scanPerBlock - 1)) / scanPerBlock

        println("Have $offsetCount scanline blocks\n")
        println("skipping offsets ...\n") // because we're loading the whole file
        fis.skipN(offsetCount * 8L)

        // find out how big each scanline is (for each channel?)
        val channelCount = channels!!.size
        val channelOffset = IntArray(channelCount)
        val channelStride = IntArray(channelCount)
        var scanlineSize = 0

        for (i in 0 until channelCount) {
            val ch = channels[i]
            val perPixel = if (ch.pixType == 1) 2 else 4
            val stride = dataSize.width * perPixel

            channelStride[i] = stride
            channelOffset[i] = scanlineSize
            scanlineSize += stride
        }

        val image = FloatImage(dataSize.width, dataSize.height, channelCount)
        println("scan line blocks")

        for (i in 0 until offsetCount) {
            val y = fis.readInt()
            val pixelSize = fis.readInt()

            System.out.printf("%d: block coord %d size %d\n", i, y, pixelSize)
            // TODO: is there a way to 'sub-stream' this so it just reads from the raw stream
            //does it matter ... probably not
            val packed = ByteArray(pixelSize)
            fis.read(packed)

            val input = ByteArrayInputStream(packed)
            val zip = InflaterInputStream(input)

            for (dy in 0 until scanPerBlock) {
                val yi = y + dy
                for (chi in 0 until channelCount) {
                    val ch = channels[chi]
                    val channel = when (ch.name) {
                        "R" -> 0
                        "G" -> 1
                        "B" -> 2
                        "A" -> 3
                        else -> {
                            // unknown channel, ignore it
                            val skip = channelStride[chi]
                            zip.skipN(skip.toLong())
                            continue
                        }
                    }

                    when (ch.pixType) {
                        0 -> {
                            assertEquals(dataSize.width * 4, channelStride[chi])
                            // uint 32 bit - not used for raster data, since it isn't normalized?
                            /*for(x in 0 until dataSize.width) {
                                val full = zip.readLE32()
                            }*/
                            zip.skipN(dataSize.width * 4L)
                        }
                        1 -> {
                            assertEquals(dataSize.width * 2, channelStride[chi])
                            // half type
                            for (x in 0 until dataSize.width) {
                                val half = zip.readBE16() // todo this having BE, and below using LE is weird
                                // HACK: gotta work out how the data size relates to the y coordinate
                                val v = float16ToFloat32(half)
                                image.setValue(x, yi, channel, v)
                            }
                        }
                        2 -> {
                            assertEquals(dataSize.width * 4, channelStride[chi])
                            // float type
                            for (x in 0 until dataSize.width) {
                                image.setValue(x, yi, channel, zip.readLE32F())
                            }
                        }
                        else -> {
                            // unknown type
                            zip.skipN(channelStride[chi].toLong())
                        }
                    }
                }
            }
        }

        //image.addLayer(layer);
        //image.addDamage(layer.bounds);
        //image.refresh(layer.bounds);

        return image
    }

}