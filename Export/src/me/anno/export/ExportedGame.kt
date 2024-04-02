package me.anno.export

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.EngineActions
import me.anno.engine.EngineBase
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.GFX
import me.anno.gpu.shader.ShaderLib
import me.anno.input.ActionManager
import me.anno.installer.Installer
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.utils.StringMap
import me.anno.ui.Window

// todo register all classes that we need (not excluded ones)
class ExportedGame(val config: StringMap) : EngineBase(
    config["gameTitle", "Rem's Engine"],
    config["configName", "config"],
    config["versionNumber", 1],
    true // it would be very rare, that we didn't need it
) {
    override fun createUI() {
        workspace = getReference("res://") // ok so?
        val scene = PrefabCache[config["firstScenePath", InvalidRef]]!!.createInstance()
        val windowStack = GFX.someWindow.windowStack
        val panel = SceneView(RenderView1(PlayMode.PLAYING, scene, style), style)
        windowStack.add(Window(panel, false, windowStack))

        Installer.checkFFMPEGInstall()
        ShaderLib.init()

        EngineActions.register()
        ActionManager.init()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val configText = getReference("res://export.json").readTextSync()
            val config = JsonStringReader.readFirst<StringMap>(configText, InvalidRef)
            ExportedGame(config).run()
        }
    }
}