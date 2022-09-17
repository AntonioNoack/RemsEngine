package me.anno.io.css

import me.anno.image.svg.CSSData
import me.anno.image.svg.SVGMesh
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

object CSSReader {

    fun read(mesh: SVGMesh, str: String) {

        /*
        #rect1 { fill: url(#Gradient1); }
        .stop1 { stop-color: red; }
        .stop2 { stop-color: black; stop-opacity: 0; }
        .stop3 { stop-color: blue; }
        * */

        var i = 0
        while (i < str.length) {

            val lastI = i
            i = str.indexOf('{', i)
            if (i < 0) break // done

            val prefix = str.substring(lastI, i).trim()
            val endIndex = str.indexOf('}', i + 1)
            if (endIndex < 0) {
                LOGGER.warn("Unexpected end of CSS")
                break
            }

            val content = str.substring(i + 1, endIndex)
            i = endIndex + 1
            val container = when {
                prefix.startsWith("#") -> {
                    mesh.ids.getOrPut(prefix.substring(1)) { CSSData() }
                }
                prefix.startsWith(".") -> {
                    mesh.classes.getOrPut(prefix.substring(1)) { CSSData() }
                }
                else -> {
                    LOGGER.warn("Couldn't understand CSS container '$prefix'")
                    null
                }
            }

            // interpret the stuff
            if (container != null) {
                val keyValuePairs = content
                    .split(';')
                    .filter { !it.isBlank2() }
                    .map { it.trim() }
                for (kvp in keyValuePairs) {
                    val colonIndex = kvp.indexOf(':')
                    if (colonIndex > 0) {
                        val name = kvp.substring(0, colonIndex).trim()
                        val value = kvp.substring(colonIndex + 1).trim()
                        container[name] = value
                    } else {
                        LOGGER.warn("Missing color/name in CSS for '$kvp'")
                    }
                }
            }
        }

    }

    private val LOGGER = LogManager.getLogger(CSSReader::class)

}