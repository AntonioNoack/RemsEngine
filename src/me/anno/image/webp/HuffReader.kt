package me.anno.image.webp

import java.io.IOException
import kotlin.math.max

class HuffReader {

    var simple = true
    var numSymbols = 0
    var simpleSymbol0 = 0
    var simpleSymbol1 = 0
    var table: VLC? = null

    fun getSymbol(bits: BitReader): Int {
        return if (simple) {
            when {
                numSymbols == 1 -> {
                    simpleSymbol0
                }
                bits.read1() -> simpleSymbol1
                else -> simpleSymbol0
            }
        } else getVLC2(bits, table!!, 8, 2)
    }

    fun getVLC2(bits: BitReader, table: VLC, a: Int, b: Int): Int {


        TODO("webp get vlc")
    }

    fun buildCanonical(codeLengths: IntArray, alphabetSize: Int) {

        var len = 0
        var code = 0
        for (sym in 0 until alphabetSize) {
            if (codeLengths[sym] > 0) {
                len++
                code = sym
                if (len > 1) break
            }
        }
        if (len == 1) {
            numSymbols = 1
            simpleSymbol0 = code
            simple = true
            return
        }
        var maxCodeLength = 0
        for (sym in 0 until alphabetSize) {
            maxCodeLength = max(maxCodeLength, codeLengths[sym])
        }
        if (maxCodeLength == 0 || maxCodeLength > WebPReader.MAX_HUFFMAN_CODE_LENGTH) throw IOException()
        val codes = IntArray(alphabetSize)
        code = 0
        this.numSymbols = 0
        for (len2 in 1..maxCodeLength) {
            for (sym in 0 until alphabetSize) {
                if (codeLengths[sym] != len2) continue
                codes[sym] = code++
                this.numSymbols++
            }
            code = code shl 1
        }
        if (this.numSymbols == 0) throw IOException("Invalid data")
        TODO()//initVLC(r.table, 8, alphabetSize, codeLengths, codes, 0)
        simple = false
    }

    fun readSimple(bits: BitReader) {
        numSymbols = bits.read(1) + 1
        simpleSymbol0 = if (bits.read1()) {
            bits.read(8)
        } else {
            bits.read(1)
        }
        if (numSymbols == 2) {
            simpleSymbol1 = bits.read(8)
        }
        simple = true
    }

    fun readNormal(bits: BitReader, alphabetSize: Int) {
        val codeLenHC = HuffReader()
        val codeLengthCodeLengths = IntArray(WebPReader.NUM_CODE_LENGTH_CODES)
        val numCodes = 4 + bits.read(4)
        if (numCodes > WebPReader.NUM_CODE_LENGTH_CODES) throw IOException("Invalid data")
        for (i in 0 until numCodes) {
            codeLengthCodeLengths[LZ77.codeLengthCodeOrder[i]] = bits.read(3)
        }

        codeLenHC.buildCanonical(codeLengthCodeLengths, WebPReader.NUM_CODE_LENGTH_CODES)

        val codeLengths = IntArray(alphabetSize)
        var maxSymbol: Int
        if (bits.read1()) {
            val bits0 = 2 + 2 * bits.read(3)
            maxSymbol = 2 + bits.read(bits0)
            if (maxSymbol > alphabetSize) throw IOException("Max Symbol > alphabet size")
        } else maxSymbol = alphabetSize

        var prevCodeLen = 8
        var symbol = 0
        while (symbol < alphabetSize) {
            if (maxSymbol-- == 0) break
            val codeLen = codeLenHC.getSymbol(bits)
            if (codeLen < 16) {
                // literal code length
                codeLengths[symbol++] = codeLen
                if (codeLen != 0) prevCodeLen = codeLen
            } else {
                var repeat = 0
                var length = 0
                when (codeLen) {
                    16 -> {
                        repeat = 3 + bits.read(2)
                        length = prevCodeLen
                    }
                    17 -> repeat = 3 + bits.read(3)
                    18 -> repeat = 11 + bits.read(7)
                }
                if (symbol + repeat > alphabetSize) throw IOException("Invalid symbol + repeat > alphabet size")
                for (i in 0 until repeat) {
                    codeLengths[symbol++] = length
                }
            }
        }

        buildCanonical(codeLengths, alphabetSize)

    }


}