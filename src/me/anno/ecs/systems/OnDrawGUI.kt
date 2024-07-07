package me.anno.ecs.systems

import me.anno.gpu.pipeline.Pipeline

/**
 * Component, which shall render (line) stuff in editor
 * */
interface OnDrawGUI {
    fun onDrawGUI(pipeline: Pipeline, all: Boolean)
}