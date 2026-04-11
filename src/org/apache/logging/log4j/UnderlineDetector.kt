package org.apache.logging.log4j

object UnderlineDetector {

    fun isUnderlined(text: String, index: Int): Boolean {
        val ESC = '\u001B'

        var i = 0
        var isUnderlined = false

        while (i < text.length && i < index) {
            if (text[i] == ESC && i + 1 < text.length && text[i + 1] == '[') {
                // Parse ANSI sequence
                val end = text.indexOf('m', i)
                if (end == -1) break

                val codes = text.substring(i + 2, end)
                    .split(';')
                    .mapNotNull { it.toIntOrNull() }

                for (code in codes) {
                    when (code) {
                        0 -> {
                            // reset all
                            isUnderlined = false
                        }
                        4 -> {
                            // underline on
                            isUnderlined = true
                        }
                        24 -> {
                            // underline off
                            isUnderlined = false
                        }
                        // ignore everything else
                    }
                }

                i = end + 1
            } else {
                i++
            }
        }

        return isUnderlined
    }

    fun getUnderlinedRegions(text: String): List<IntRange> {
        val ESC = '\u001B'

        var i = 0
        var underlinedStart = 0
        var isUnderlined = false
        val ranges = ArrayList<IntRange>()

        while (i < text.length) {
            if (text[i] == ESC && i + 1 < text.length && text[i + 1] == '[') {
                // Parse ANSI sequence
                val end = text.indexOf('m', i)
                if (end == -1) break

                val codes = text.substring(i + 2, end)
                    .split(';')
                    .mapNotNull { it.toIntOrNull() }

                val wasUnderlined = isUnderlined
                for (code in codes) {
                    when (code) {
                        0 -> {
                            // reset all
                            isUnderlined = false
                        }
                        4 -> {
                            // underline on
                            isUnderlined = true
                        }
                        24 -> {
                            // underline off
                            isUnderlined = false
                        }
                        // ignore everything else
                    }
                }

                if (wasUnderlined != isUnderlined) {
                    if (wasUnderlined) {
                        // was underlined
                        ranges.add(underlinedStart until i)
                    } else {
                        underlinedStart = end + 1
                    }
                }

                i = end + 1
            } else {
                // normal text
                i++
            }
        }

        if (isUnderlined) {
            ranges.add(underlinedStart until text.length)
        }

        return ranges
    }
}