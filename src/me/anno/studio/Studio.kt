package me.anno.studio

import me.anno.studio.project.Project
import me.anno.ui.dragging.IDraggable
import java.util.concurrent.ConcurrentLinkedQueue

object Studio {

    var project: Project? = null

    var dragged: IDraggable? = null

    var editorTimeDilation = 0f

    val targetFPS get() = project!!.targetFPS
    val targetWidth get() = project!!.targetWidth
    val targetHeight get() = project!!.targetHeight

    fun addEvent(event: () -> Unit){
        eventTasks += event
    }

    val eventTasks = ConcurrentLinkedQueue<() -> Unit>()

}