package me.anno.studio

import me.anno.ui.dragging.IDraggable
import java.util.concurrent.ConcurrentLinkedQueue

object Studio {

    var dragged: IDraggable? = null

    var editorTimeDilation = 0f

    var targetFPS = 30f
    var targetWidth = 1920
    var targetHeight = 1080

    fun addEvent(event: () -> Unit){
        eventTasks += event
    }

    val eventTasks = ConcurrentLinkedQueue<() -> Unit>()

}