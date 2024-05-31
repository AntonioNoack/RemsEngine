package me.anno.tests.engine.effect

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView1
import me.anno.engine.ui.render.SceneView
import me.anno.graph.visual.render.effects.DepthOfFieldNode
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.FloatInput
import me.anno.ui.input.NumberType
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    val scene = downloads.getChild("3d/ogldev-source/crytek_sponza/sponza.fbx")
    val scene1 = PrefabCache[scene]!!.getSampleInstance()
    testUI3("Depth Of Field") { // todo this effect is broken :(, being blurry everywhere
        EditorState.prefabSource = scene
        val sceneView = SceneView(RenderView1(PlayMode.EDITING, scene1, style), style)
        sceneView.renderer.renderMode = RenderMode.DEPTH_OF_FIELD
        sceneView.weight = 1f
        PrefabInspector.currentInspector = PrefabInspector(scene)
        val list = PanelListY(style)
        val list2 = PanelListX(style)
        val effect = RenderMode.DEPTH_OF_FIELD.renderGraph!!.nodes.filterIsInstance<DepthOfFieldNode>().first()
        list2.add(FloatInput("Point", effect.getFloatInput(1), NumberType.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(1, it.toFloat()) })
        list2.add(FloatInput("Scale", effect.getFloatInput(2), NumberType.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(2, it.toFloat()) })
        list2.add(FloatInput("RadScale", effect.getFloatInput(3), NumberType.LONG_PLUS, style)
            .setChangeListener { effect.setInput(3, it.toFloat()) })
        list2.add(FloatInput("MaxSize", effect.getFloatInput(4), NumberType.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(4, it.toFloat()) })
        list2.add(FloatInput("Spherical", effect.getFloatInput(5), NumberType.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(5, it.toFloat()) })
        list.add(sceneView)
        list.add(list2)
        list
    }
}