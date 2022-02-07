package me.anno.remsstudio

import me.anno.audio.openal.ALBase
import me.anno.audio.openal.AudioManager
import me.anno.audio.openal.AudioTasks
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.baseTheme
import me.anno.gpu.GFX
import me.anno.gpu.GFX.gameTime
import me.anno.input.ActionManager
import me.anno.input.Input.keyUpCtr
import me.anno.installer.Installer.checkInstall
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.Dict
import me.anno.remsstudio.CheckVersion.checkVersion
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.remsstudio.audio.AudioManager2
import me.anno.remsstudio.cli.RemsCLI
import me.anno.remsstudio.gpu.shader.ShaderLibV2
import me.anno.remsstudio.objects.Camera
import me.anno.remsstudio.objects.Transform
import me.anno.remsstudio.objects.text.Text
import me.anno.remsstudio.ui.StudioFileImporter
import me.anno.remsstudio.ui.scene.ScenePreview
import me.anno.remsstudio.ui.sceneTabs.SceneTabs.currentTab
import me.anno.studio.GFXSettings
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.ui.editor.WelcomeUI
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.style.Style
import me.anno.utils.OS
import java.io.File

// todo bug: runtime stats are missing :/

// todo bug: signed distance field texts are missing / not rendering

// todo isolate and remove certain frequencies from audio
// todo visualize audio frequency, always!!!, from 25Hz to 48kHz
// inspiration: https://www.youtube.com/watch?v=RA5UiLYWdbM&ab_channel=TomScott

// todo use YouTube videos as a video source?

// todo when rendering low-res images: for nearest-interpolated textures use interpolation on the edges of pixels

// todo scripting?...
// todo gizmos

// Launch4j

// todo configurable background color (render settings)
// todo scene settings: render duration & size and such should be inside there as well


// todo when mixing strings, \n and spaces have a special role:
// todo - substring should not be longer in a line than the total

// todo settings should open in a window, that does not cover everything, just maybe 90%

// todo if a resource is requested, and there is a mutex limitation, it should be rejected instead of queued
// so we don't put strain on the cpu & memory, if we don't really need the resource
// this is the case for audio in the timeline, when just scrolling

// todo calculate in-between frames for video by motion vectors; https://www.youtube.com/watch?v=PEe-ZeVbTLo ?
// 30 -> 60 fps
// the topic is complicated...

// todo mode to move vertices of rectangle one by one (requires clever transforms, or a new type of GFXTransform)
// for perspective matching: e.g. moving a fake image onto another image

// todo create proxies only for sections of video
// todo proxies for faster playback? e.g. every 10th frame? idk...

// proxy creation uses 100% cpu... prevent that somehow, or decrease process priority?
// it uses 36% on its own -> heavier weight?
// -> idk how on Windows; done for Linux

// nearby frame compression (small changes between frames, could use lower resolution) on the gpu side? maybe...
// -> would maybe allow 60fps playback better


// todo Version 2:
// todo make stuff component based and compile shaders on the fly...
// would allow easy system for pdf particles and such... maybe...

// done color to alpha
// done alpha to color
// todo discard alpha being > or < than x / map alpha

// todo splines for the polygon line

// todo sketching: draw frame by frame, only save x,y,radius?
// todo record drawing, and use meta-ball like shapes (maybe)

// todo translations for everything...
// todo limit the history to entries with 5x the same name? how exactly?...

// todo saturation/lightness controls by hue

// to do Mod with "hacked"-text effect for text: swizzle characters and introduce others?

// todo bug: cutting (control+x) only cuts one

// todo zoom in on point in 2D using mouse position

// todo when playing video, and the time hasn't been touched manually, slide the time panel, when the time reaches the end: slide by 1x window width

// todo signed field to split meshes with option for face subdivisions

object RemsStudio : StudioBase(true, "Rem's Studio", 10105) {

    // private val LOGGER = LogManager.getLogger(RemsStudio::class)

    lateinit var currentCamera: Camera
    var lastTouchedCamera: Camera? = null

    fun updateLastLocalTime(parent: Transform, time: Double) {

        val localTime = parent.getLocalTime(time)
        parent.lastLocalTime = localTime
        val children = parent.children
        for (i in children.indices) {
            val child = children[i]
            updateLastLocalTime(child, localTime)
        }

        editorTime += GFX.deltaTime * editorTimeDilation
        if (editorTime <= 0.0 && editorTimeDilation < 0.0) {
            editorTimeDilation = 0.0
            editorTime = 0.0
        }

    }

    override fun loadConfig() {
        RemsRegistry.init()
        RemsConfig.init()
    }

    override fun onGameInit() {
        gfxSettings = GFXSettings.get(DefaultConfig["editor.gfx", GFXSettings.LOW.id], GFXSettings.LOW)
        workspace = DefaultConfig["workspace.dir", getReference(OS.documents, configName)]
        checkInstall()
        checkVersion()
        AudioManager2.init()
        ShaderLibV2.init()
    }

    lateinit var welcomeUI: WelcomeUI

    override fun isSelected(obj: Any?): Boolean {
        return obj == Selection.selectedInspectable ||
                obj == Selection.selectedProperty ||
                obj == Selection.selectedTransform
    }

    override fun createUI() {
        Dict.loadDefault()
        welcomeUI = object : WelcomeUI() {
            override fun loadProject(name: String, folder: FileReference): Pair<String, FileReference> {
                val project = RemsStudio.loadProject(name, folder)
                return Pair(project.name, project.file)
            }

            override fun createProjectUI() {
                RemsStudioUILayouts.createEditorUI(welcomeUI)
            }
        }
        welcomeUI.createWelcomeUI(this, RemsStudio::createBackground)
        StudioActions.register()
        ActionManager.init()
    }

    fun loadProject(name: String, folder: File) {
        loadProject(name, getReference(folder))
    }

    fun loadProject(name: String, folder: FileReference): Project {
        val project = Project(name.trim(), folder)
        RemsStudio.project = project
        project.open()
        GFX.addGPUTask(1) {
            GFX.setTitle("Rem's Studio: ${project.name}")
        }
        return project
    }

    override fun openHistory() {
        history?.display()
    }

    override fun save() {
        project?.save()
    }

    override fun getDefaultFileLocation(): FileReference {
        return project?.file ?: InvalidRef
    }

    override fun getPersistentStorage(): FileReference {
        return project?.file ?: super.getPersistentStorage()
    }

    override fun importFile(file: FileReference) {
        addEvent {
            StudioFileImporter.addChildFromFile(
                root,
                file,
                FileContentImporter.SoftLinkMode.ASK,
                true
            ) {}
        }
    }

    var project: Project? = null

    var editorTime = 0.5

    var editorTimeDilation = 0.0
        set(value) {
            if (value != field) updateAudio()
            field = value
        }

    val isPaused get() = editorTimeDilation == 0.0
    val isPlaying get() = editorTimeDilation != 0.0

    val targetDuration get(): Double = project?.targetDuration ?: Double.POSITIVE_INFINITY
    val targetSampleRate get(): Int = project?.targetSampleRate ?: 48000
    val targetFPS get(): Double = project?.targetFPS ?: 60.0
    val targetWidth get(): Int = project?.targetWidth ?: GFX.width
    val targetHeight get(): Int = project?.targetHeight ?: GFX.height
    val targetOutputFile get(): FileReference = project!!.targetOutputFile
    val motionBlurSteps get(): AnimatedProperty<Int> = project!!.motionBlurSteps
    val shutterPercentage get() = project!!.shutterPercentage
    val history get() = currentTab?.history
    val nullCamera get() = project?.nullCamera

    var root = Transform()

    var currentlyDrawnCamera: Camera? = nullCamera

    val selection = ArrayList<String>()

    override fun onGameLoopStart() {
        updateLastLocalTime(root, editorTime)
    }

    override fun onGameLoop(w: Int, h: Int) {
        DefaultConfig.saveMaybe("main.config")
        baseTheme.values.saveMaybe("style.config")
        Selection.update()
        super.onGameLoop(w, h)
    }

    override fun onGameLoopEnd() {

    }

    override fun onGameClose() {

    }

    private var lastCode: Any? = null
    fun incrementalChange(title: String, run: () -> Unit) =
        incrementalChange(title, title, run)

    fun incrementalChange(title: String, groupCode: Any, run: () -> Unit) {
        val history = history ?: return run()
        val code = groupCode to keyUpCtr
        if (lastCode != code) {
            change(title, code, run)
            lastCode = code
        } else {
            run()
            history.update(title, code)
        }
        currentTab?.hasChanged = true
        invalidateUI()
    }

    fun largeChange(title: String, run: () -> Unit) {
        change(title, gameTime, run)
        lastCode = null
        currentTab?.hasChanged = true
        invalidateUI()
    }

    private fun change(title: String, code: Any, run: () -> Unit) {
        val history = history ?: return run()
        if (history.isEmpty()) {
            history.put("Start State", Unit)
        }
        run()
        history.put(title, code)
    }

    private fun createBackground(style: Style): Panel {
        val background = ScenePreview(style)
        val grayPlane = PanelListX(style).apply { backgroundColor = 0x55777777 }
        val panel = PanelStack(style)
            .apply {
                add(background)
                add(grayPlane)
            }
        root.children.clear()
        Text("Rem's Studio", root).apply {
            blockAlignmentX.set(0f)
            blockAlignmentY.set(0f)
            textAlignment.set(0f)
            relativeCharSpacing = 0.12f
            invalidate()
        }
        return panel
    }

    fun updateAudio() {
        AudioTasks.addTask(100) {
            // update the audio player...
            if (isPlaying) {
                AudioManager.requestUpdate()
            } else {
                AudioManager2.stop()
            }
            ALBase.check()
        }
    }

    override fun clearAll() {
        super.clearAll()
        root.findFirstInAll { it.clearCache(); false }
    }

    // UI with traditional editor?
    // - adding effects
    // - simple mask overlays
    // - simple color correction

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            run()
        } else {
            RemsCLI.main(args)
        }
    }

}