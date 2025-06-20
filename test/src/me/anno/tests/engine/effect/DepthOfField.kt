package me.anno.tests.engine.effect

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView1
import me.anno.engine.ui.render.SceneView
import me.anno.graph.visual.render.effects.DepthOfFieldNode
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.FloatInput
import me.anno.ui.input.NumberType
import me.anno.utils.OS.downloads
import me.anno.utils.structures.lists.Lists.firstInstance2

fun main() {
    // todo bug: color is grayish where blurred :/
    //  can be seen by setting Scale to 5-10
    OfficialExtensions.initForTests()
    val scene = downloads.getChild("3d/ogldev-source/crytek_sponza/sponza.fbx")
    val scene1 = PrefabCache[scene].waitFor()!!.sample as PrefabSaveable
    testUI3("Depth Of Field") {
        EditorState.prefabSource = scene
        val sceneView = SceneView(RenderView1(PlayMode.EDITING, scene1, style), style)
        sceneView.renderView.renderMode = RenderMode.DEPTH_OF_FIELD
        sceneView.weight = 1f
        PrefabInspector.currentInspector = PrefabInspector(scene)
        val list = PanelListY(style)
        val list2 = PanelListX(style)
        val effect = RenderMode.DEPTH_OF_FIELD.renderGraph!!.nodes.firstInstance2(DepthOfFieldNode::class)
        list2.add(FloatInput(NameDesc("Point"), effect.getFloatInput(1), NumberType.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(1, it.toFloat()) })
        list2.add(FloatInput(NameDesc("Scale"), effect.getFloatInput(2), NumberType.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(2, it.toFloat()) })
        list2.add(FloatInput(NameDesc("RadScale"), effect.getFloatInput(3), NumberType.LONG_PLUS, style)
            .setChangeListener { effect.setInput(3, it.toFloat()) })
        list2.add(FloatInput(NameDesc("MaxSize"), effect.getFloatInput(4), NumberType.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(4, it.toFloat()) })
        list2.add(FloatInput(NameDesc("Spherical"), effect.getFloatInput(5), NumberType.FLOAT_PLUS, style)
            .setChangeListener { effect.setInput(5, it.toFloat()) })
        list.add(sceneView)
        list.add(list2)
        list
    }
}