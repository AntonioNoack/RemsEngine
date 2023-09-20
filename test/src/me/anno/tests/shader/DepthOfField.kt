package me.anno.tests.shader

import me.anno.animation.Type
import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.graph.render.effects.DepthOfFieldNode
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.input.FloatInput
import me.anno.utils.OS.downloads

fun main() {
    val scene = downloads.getChild("ogldev-source/crytek_sponza/sponza.obj")
    testUI3("Depth Of Field") {
        EditorState.prefabSource = scene
        val sceneView = SceneView(EditorState, PlayMode.EDITING, style)
        sceneView.renderer.renderMode = RenderMode.DEPTH_OF_FIELD
        sceneView.weight = 1f
        PrefabInspector.currentInspector = PrefabInspector(scene)
        val list = PanelListY(style)
        val list2 = PanelListX(style)
        val effect = RenderMode.DEPTH_OF_FIELD.renderGraph!!.nodes.filterIsInstance<DepthOfFieldNode>().first()
        list2.add(FloatInput("Point", effect.getInput(1) as Float, Type.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(1, it.toFloat()) })
        list2.add(FloatInput("Scale", effect.getInput(2) as Float, Type.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(2, it.toFloat()) })
        list2.add(FloatInput("RadScale", effect.getInput(3) as Float, Type.LONG_PLUS, style)
            .setChangeListener { effect.setInput(3, it.toFloat()) })
        list2.add(FloatInput("MaxSize", effect.getInput(4) as Float, Type.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(4, it.toFloat()) })
        list2.add(FloatInput("Spherical", effect.getInput(5) as Float, Type.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(5, it.toFloat()) })
        list.add(sceneView)
        list.add(list2)
        list
    }
}