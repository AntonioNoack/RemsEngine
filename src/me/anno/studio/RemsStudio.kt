package me.anno.studio

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.ActionManager
import me.anno.installer.Installer.checkInstall
import me.anno.objects.Camera
import me.anno.objects.Inspectable
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.history.SceneState
import me.anno.studio.project.Project
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.UILayouts
import me.anno.ui.editor.graphs.GraphEditorBody
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.editor.sceneView.ISceneView
import me.anno.ui.editor.treeView.TreeView
import me.anno.utils.OS
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.abs

// todo scene screenshot/editor screenshot

// todo draw frame by frame, only save x,y,radius?

// todo small preview?

// todo input field title, no focus color

object RemsStudio: StudioBase(true, "Rem's Studio", "RemsStudio"){

    override fun onGameInit() {
        RemsConfig.init()
        gfxSettings = DefaultConfig["editor.gfx", 0].run { GFXSettings.values().firstOrNull { it.id == this } ?: GFXSettings.LOW }
        workspace = DefaultConfig["workspace.dir", File(OS.documents, "RemsStudio")]
        checkInstall()
    }

    override fun createUI() {
        UILayouts.createWelcomeUI()
        StudioActions.register()
        ActionManager.init()
    }

    var gfxSettings = GFXSettings.LOW
        set(value) {
            field = value
            DefaultConfig["editor.gfx"] = value.id
            DefaultConfig.putAll(value.data)
        }

    lateinit var workspace: File

    var project: Project? = null

    // todo save in project
    var editorTime = 0.5
        set(value) {// cannot be set < 0
            // clamping to the right edge is annoying
            // clamping to the left is annoying too
            field = value // max(value, 0.0)
        }

    var editorTimeDilation = 0.0
        set(value) {
            if(value != field) updateAudio()
            field = value
        }

    val isSaving = AtomicBoolean(false)
    var forceSave = false
    var lastSave = System.nanoTime()
    var saveIsRequested = true

    val isPaused get() = editorTimeDilation == 0.0
    val isPlaying get() = editorTimeDilation != 0.0

    val targetDuration get() = project!!.targetDuration
    val targetFPS get() = project!!.targetFPS
    val targetWidth get() = project?.targetWidth ?: GFX.windowWidth
    val targetHeight get() = project?.targetHeight ?: GFX.windowHeight
    val targetOutputFile get() = project!!.targetOutputFile
    val history get() = SceneTabs.currentTab!!.history

    val nullCamera: Camera by lazy {
        Camera(null)
            .apply {
                name = "Inspector Camera"
                onlyShowTarget = false
                // higher far value to allow other far values to be seen
                farZ.defaultValue = 5000f
                timeDilation = 0.0 // the camera has no time, so no motion can be recorded
            }
    }

    var root = Transform()

    // var selectedCamera = nullCamera
    var usedCamera = nullCamera

    var selectedTransform: Transform? = null
    var selectedProperty: AnimatedProperty<*>? = null
    var selectedInspectable: Inspectable? = null

    override fun onGameLoopStart() {
        saveStateMaybe()
    }

    override fun onGameLoopEnd() {

    }

    override fun onGameClose() {

    }

    var wasSavingConfig = 0L
    fun saveStateMaybe() {
        val historySaveDuration = 1e9
        if (saveIsRequested && !isSaving.get()) {
            val current = System.nanoTime()
            if (forceSave || abs(current - lastSave) > historySaveDuration) {
                lastSave = current
                forceSave = false
                saveIsRequested = false
                saveState()
            }
        }
        if ((DefaultConfig.wasChanged || DefaultConfig.style.values.wasChanged)
            && GFX.lastTime > wasSavingConfig + 1_000_000_000
        ) {// only save every 1s
            // delay in case it needs longer
            wasSavingConfig = GFX.lastTime + (60 * 1e9).toLong()
            thread {
                DefaultConfig.save()
                wasSavingConfig = GFX.lastTime
            }
        }
    }

    fun saveState() {
        // saving state
        if (project == null) return
        isSaving.set(true)
        thread {
            try {
                val state = SceneState()
                state.update()
                history.put(state)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isSaving.set(false)
        }
    }

    fun onSmallChange(cause: String) {
        saveIsRequested = true
        SceneTabs.currentTab?.hasChanged = true
        updateSceneViews()
        // LOGGER.info(cause)
    }

    fun onLargeChange() {
        saveIsRequested = true
        forceSave = true
        SceneTabs.currentTab?.hasChanged = true
        updateSceneViews()
    }

    fun updateSceneViews() {
        windowStack.forEach { window ->
            window.panel.listOfVisible
                .forEach {
                    when (it) {
                        is TreeView, is ISceneView -> {
                            it.invalidateDrawing()
                        }
                        is GraphEditorBody -> {
                            if (selectedProperty != null) {
                                it.invalidateDrawing()
                            }
                        }
                        is PropertyInspector -> {
                            if(selectedTransform != null){
                                it.needsUpdate = true
                            }
                        }
                    }
                }
        }
    }

}