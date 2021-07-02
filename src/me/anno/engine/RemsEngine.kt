package me.anno.engine

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.DefaultLayout
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.Window
import me.anno.input.ActionManager
import me.anno.language.translation.Dict
import me.anno.studio.StudioBase
import me.anno.studio.rems.StudioActions
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.editor.UILayouts

class RemsEngine : StudioBase(true, "Rem's Engine", "RemsEngine", 1) {

    override fun createUI() {

        Dict.loadDefault()

        // todo select project view, like Rem's Studio
        // todo select scene
        // todo show scene, and stuff, like Rem's Studio

        // todo different editing modes like Blender?, e.g. animating stuff, scripting, ...
        // todo and always be capable to change stuff

        // todo create our editor, where we can drag stuff into the scene, view it in 3D, move around, and such
        // todo play the scene

        // todo base shaders, which can be easily made touch-able

        // for testing directly jump in the editor
        val loadedScene = RootEntity()
        val style = style

        val list = PanelListY(style)
        // todo add controls
        list += DefaultLayout.createDefaultMainUI(loadedScene, false, style)
        // todo add console output
        list += ConsoleOutputPanel(style)
        windowStack.add(Window(list))


        StudioActions.register()
        ActionManager.init()

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>){
            RemsEngine().run()
        }
    }

}