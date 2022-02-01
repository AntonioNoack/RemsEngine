package me.anno.image.svg

import me.anno.gpu.GFX
import me.anno.image.svg.tokenizer.SVGTokenizer
import me.anno.objects.Transform
import me.anno.utils.structures.lists.Lists.indexOf2
import me.anno.utils.types.Matrices.skew
import org.joml.Matrix4d
import java.util.*

object SVGTransform {

    fun applyTransform(transform: Matrix4d, actions: String) {
        val tokens = SVGTokenizer(actions).tokens
        var i = 0
        while (i + 2 < tokens.size) {
            val name = tokens[i] as? String
            if (name != null && tokens[i + 1] == '(') {
                val endIndex = tokens.indexOf2(')', i, true)
                if (endIndex < 0) return
                val params = tokens.subList(i + 2, endIndex)
                    .filterIsInstance<Double>()
                when (name.lowercase(Locale.getDefault())) {
                    "translate" -> {
                        if (params.isNotEmpty()) {
                            transform.translate(
                                params[0],
                                params.getOrElse(1) { 0.0 },
                                params.getOrElse(2) { 0.0 })
                        }
                    }
                    "scale" -> {
                        if (params.isNotEmpty()) {
                            transform.scale(
                                params[0],
                                params.getOrElse(1) { 1.0 },
                                params.getOrElse(2) { 1.0 })
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
                                transform.rotate(GFX.toRadians(params[0]), Transform.zAxis)
                            }
                            3 -> {
                                val dx = params[1]
                                val dy = params[2]
                                transform.translate(-dx, -dy, 0.0)
                                transform.rotate(GFX.toRadians(params[0]), Transform.zAxis)
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
                    else -> throw RuntimeException("Unknown transform $name($params)")
                }
                i = endIndex
            }// else unknown stuff...
            i++
        }
    }


}