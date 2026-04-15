package me.anno.mesh.usd

import me.anno.io.binary.ByteArrayIO.readLE64
import net.jpountz.lz4.LZ4Factory

class USDCReader(private val fileData: ByteArray, val path: String) {

    private var pos = 0

    var major = 0
    var minor = 0
    var patch = 0

    fun read(): USDPrim {
        readHeader()
        TODO()
    }

    fun readHeader() {
        consume("PXR-USDC")
        major = fileData[pos++].toInt()
        minor = fileData[pos++].toInt()
        patch = fileData[pos].toInt()
        pos += 6 // unused
        println("USDC $major.$minor.$patch")
        val toc = fileData.readLE64(pos)
        pos += 16 // 8 bytes extra are unused
        println("TOC: $toc (0x${toc.toString(16)})")
        readTOC(toc)
        readTokens(sections["TOKENS"])
    }

    data class Section(val offset: Int, val size: Int)

    val sections = HashMap<String, Section>()

    fun readTOC(toc: Long) {
        check(toc + 8 < fileData.size)
        pos = toc.toInt()
        val numSections = readLE64()
        check(toc + 8 + numSections * 32 <= fileData.size) { "Not enough space for sections" }
        for (i in 0 until numSections) {
            val name = readString16()
            val start = readLE64()
            val size = readLE64()
            println("Section '$name' @$start += $size")
            check(start > 0 && start + size <= fileData.size)
            sections[name] = Section(start.toInt(), size.toInt())
        }
    }

    fun readTokens(section: Section?) {
        section ?: return
        pos = section.offset
        val numTokens = readLE64()
        val decompressedSize = readLE64()
        val compressedSize = readLE64()
        println("start for compressed tokens: $pos (0x${pos.toString(16)})")
        // pos = (pos + alignment).and(alignment.inv())
        check(pos + compressedSize <= fileData.size)
        check(decompressedSize <= Int.MAX_VALUE.toLong())
        println("#tokens: $numTokens, decompressed size: $decompressedSize, compressed size: $compressedSize")
        val compressedData = fileData.copyOfRange(pos, pos + compressedSize.toInt())
        println(
            "compressed data: $compressedSize, ${
                compressedData.toList().map {
                    if (it in 32..128) "'${it.toInt().toChar()}'"
                    else "#" + it.toInt().and(0xff).toString(16)
                }
            }, $numTokens tokens"
        )
        val uncompressedData = LZ4Factory.safeInstance()
            .safeDecompressor()
            .decompress(compressedData, decompressedSize.toInt())
        TODO("read $numTokens tokens from ${uncompressedData.size} bytes")
    }

    fun readLE64(): Long {
        val value = fileData.readLE64(pos)
        pos += 8
        return value
    }

    fun readString16(): String {
        var i = pos
        val max = pos + 16
        while (i < max && fileData[i] != 0.toByte()) i++
        val value = String(fileData, pos, i - pos)
        pos += 16
        return value
    }

    fun consume(str: String) {
        val pos0 = pos
        for (c in str) {
            check(fileData[pos++].toInt() == c.code) {
                "Expected to find $str at $pos0"
            }
        }
    }

}