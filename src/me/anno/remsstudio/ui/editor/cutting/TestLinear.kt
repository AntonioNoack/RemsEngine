package me.anno.remsstudio.ui.editor.cutting

import org.apache.logging.log4j.LogManager
import org.joml.Vector4f

fun main(){

    val g = LayerViewGradient(null,
        0, 2,
        Vector4f(0f, 0f, 0f, 0f), Vector4f(0.2f, 0f, 0f, 0.2f)
    )

    val logger = LogManager.getLogger("TestLinear")

    logger.info(g.isLinear(5, 1, Vector4f(0.5f, 0f, 0f, 0.5f)).toString())// t
    logger.info(g.isLinear(5, 1, Vector4f(0.4f, 0f, 0f, 0.5f)).toString())// f
    logger.info(g.isLinear(5, 1, Vector4f(0.6f, 0f, 0f, 0.5f)).toString())// f

    logger.info(g.isLinear(5, 2, Vector4f(0.5f, 0f, 0f, 0.5f)).toString())// t
    logger.info(g.isLinear(5, 2, Vector4f(0.4f, 0f, 0f, 0.5f)).toString())// t
    logger.info(g.isLinear(5, 2, Vector4f(0.6f, 0f, 0f, 0.5f)).toString())// t

}

fun t1(){

    val logger = LogManager.getLogger("TestLinear")

    val g = LayerViewGradient(null,
        0, 1,
        Vector4f(0f), Vector4f(1f)
    )

    logger.info(g.isLinear(5, 1, Vector4f(5f)).toString())// t
    logger.info(g.isLinear(5, 1, Vector4f(4f)).toString())// f
    logger.info(g.isLinear(5, 1, Vector4f(6f)).toString())// f

}