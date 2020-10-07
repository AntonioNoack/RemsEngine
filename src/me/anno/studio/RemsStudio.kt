package me.anno.studio

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.objects.Camera
import me.anno.objects.Inspectable
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.project.Project
import me.anno.ui.dragging.IDraggable
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.UILayouts
import me.anno.ui.editor.sceneTabs.SceneTabs
import kotlin.math.max

// todo scene screenshot/editor screenshot

// todo draw frame by frame, only save x,y,radius?

// todo small preview?

// todo input field title, no focus color

object RemsStudio: StudioBase(true){

    override fun createUI() {
        UILayouts.createWelcomeUI()
    }

    var gfxSettings = DefaultConfig["editor.gfx", 0].run { GFXSettings.values().firstOrNull { it.id == this } ?: GFXSettings.LOW }
        set(value) {
            field = value
            DefaultConfig["editor.gfx"] = value.id
            DefaultConfig.putAll(value.data)
        }

    var project: Project? = null

    // todo save in project
    var editorTime = 0.5
        set(value) {// cannot be set < 0
            // clamping to the right edge is annoying
            field = max(value, 0.0)
        }

    var editorTimeDilation = 0.0
        set(value) {
            if(value != field) updateAudio()
            field = value
        }

    val isPaused get() = editorTimeDilation == 0.0
    val isPlaying get() = editorTimeDilation != 0.0

    val targetDuration get() = project!!.targetDuration
    val targetFPS get() = project!!.targetFPS
    val targetWidth get() = project?.targetWidth ?: GFX.windowWidth
    val targetHeight get() = project?.targetHeight ?: GFX.windowHeight
    val targetOutputFile get() = project!!.targetOutputFile
    val history get() = SceneTabs.currentTab!!.history

    val nullCamera = Camera(null)

    init {
        nullCamera.name = "Inspector Camera"
        nullCamera.onlyShowTarget = false
        // higher far value to allow other far values to be seen
        nullCamera.farZ.defaultValue = 5000f
        nullCamera.timeDilation = 0.0 // the camera has no time, so no motion can be recorded
    }

    var root = Transform()
    // var selectedCamera = nullCamera
    var usedCamera = nullCamera

    var selectedTransform: Transform? = null
    var selectedProperty: AnimatedProperty<*>? = null
    var selectedInspectable: Inspectable? = null

}