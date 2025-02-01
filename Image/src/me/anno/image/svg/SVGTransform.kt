package me.anno.image.svg

import me.anno.image.svg.SVGTokenizer.svgTokenize
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.structures.lists.Lists.indexOf2
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.Matrix3x2f
import org.joml.Matrix4d

object SVGTransform {
    private val LOGGER = LogManager.getLogger("SVGTransform")
    fun applyTransform(transform: Matrix4d, actions: String) {
        val tokens = svgTokenize(actions)
        var i = 0
        while (i + 2 < tokens.size) {
            val name = tokens[i] as? String
            if (name != null && tokens[i + 1] == '(') {
                val endIndex = tokens.indexOf2(')', i, true)
                if (endIndex < 0) return
                val params = tokens.subList(i + 2, endIndex)
                    .filterIsInstance2(Double::class)
                when (name.lowercase()) {
                    "translate" -> {
                        if (params.isNotEmpty()) {
                            transform.translate(
                                params[0],
                                params.getOrNull(1) ?: 0.0,
                                params.getOrNull(2) ?: 0.0
                            )
                        }
                    }
                    "scale" -> {
                        if (params.isNotEmpty()) {
                            transform.scale(
                                params[0],
                                params.getOrNull(1) ?: 1.0,
                                params.getOrNull(2) ?: 1.0
                            )
                        }
                    }
                    "matrix" -> {
                        if (params.size == 6) {
                            transform.mul(
                                Matrix4d(
                                    params[0], params[1], 0.0, params[2],
                                    params[3], params[4], 0.0, params[5],
                                    0.0, 0.0, 1.0, 0.0,
                                    0.0, 0.0, 0.0, 1.0
                                )
                            )
                        }
                    }
                    "rotate" -> {
                        when (params.size) {
                            1 -> {
                                transform.rotateZ(params[0].toRadians())
                            }
                            3 -> {
                                val dx = params[1]
                                val dy = params[2]
                                transform.translate(-dx, -dy, 0.0)
                                transform.rotateZ(params[0].toRadians())
                                transform.translate(dx, dy, 0.0)
                            }
                        }
                    }
                    "skewx" -> {
                        if (params.size == 1) {
                            transform.skew(params[0], 0.0)
                        }
                    }
                    "skewy" -> {
                        if (params.size == 1) {
                            transform.skew(0.0, params[0])
                        }
                    }
                    else -> LOGGER.warn("Unknown transform $name($params)")
                }
                i = endIndex
            }// else unknown stuff...
            i++
        }
    }

    fun applyTransform(m: Matrix3x2f, actions: String) {
        val tmp = Matrix4d()
        m.copyInto(tmp)
        applyTransform(tmp, actions)
        tmp.copyInto(m)
    }
}