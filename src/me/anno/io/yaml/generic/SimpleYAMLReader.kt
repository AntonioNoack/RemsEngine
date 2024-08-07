package me.anno.io.yaml.generic

import me.anno.io.yaml.generic.YAMLReader.findColon

object SimpleYAMLReader {
    @JvmStatic
    fun read(lines: Iterator<String>, needsSpaceAfterColon: Boolean): Map<String, String> {
        return read(lines, needsSpaceAfterColon, HashMap())
    }

    @JvmStatic
    fun read(
        lines: Iterator<String>,
        needsSpaceAfterColon: Boolean,
        dst: MutableMap<String, String>
    ): Map<String, String> {
        while (lines.hasNext()) {
            val line = lines.next()
            if (line.startsWith('#')) continue
            var i0 = findColon(line, needsSpaceAfterColon) + 1
            var i1 = line.lastIndex
            if (i0 > 0) {
                val key = line.substring(0, i0 - 1)
                while (i0 < i1 && line[i0].isWhitespace()) i0++
                while (i1 > i0 && line[i1].isWhitespace()) i1--
                dst[key] = line.substring(i0, i1 + 1)
            } else continue
        }
        return dst
    }
}