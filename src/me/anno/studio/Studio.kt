package me.anno.studio

import me.anno.audio.ALBase
import me.anno.audio.AudioManager
import me.anno.gpu.GFX
import me.anno.objects.Camera
import me.anno.objects.Inspectable
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.project.Project
import me.anno.ui.dragging.IDraggable
import java.util.concurrent.ConcurrentLinkedQueue

object Studio {

    var project: Project? = null

    var dragged: IDraggable? = null

    var editorTime = 0f

    var editorTimeDilation = 0f
        set(value) {
            if(value != field) updateAudio()
            field = value
        }

    val isPaused get() = editorTimeDilation == 0f
    val isPlaying get() = editorTimeDilation != 0f

    val targetFPS get() = project!!.targetFPS
    val targetWidth get() = project!!.targetWidth
    val targetHeight get() = project!!.targetHeight
    val targetOutputFile get() = project!!.targetOutputFile

    val nullCamera = Camera(null)

    init {
        nullCamera.name = "Inspector Camera"
        nullCamera.onlyShowTarget = false
        // higher far value to allow other far values to be seen
        nullCamera.farZ.addKeyframe(0f, 5000f, 1f)
        nullCamera.timeDilation = 0f // the camera has no time, so no motion can be recorded
    }

    var root = Transform()
    var selectedCamera = nullCamera
    var usedCamera = nullCamera

    var selectedTransform: Transform? = null
    var selectedProperty: AnimatedProperty<*>? = null
    var selectedInspectable: Inspectable? = null

    fun updateAudio(){
        GFX.addAudioTask {
            // update the audio player...
            if(isPlaying){
                AudioManager.requestTimeUpdate()
            } else {
                AudioManager.stop()
            }
            ALBase.check()
            100
        }
    }

    fun addEvent(event: () -> Unit){
        eventTasks += event
    }

    val eventTasks = ConcurrentLinkedQueue<() -> Unit>()

}