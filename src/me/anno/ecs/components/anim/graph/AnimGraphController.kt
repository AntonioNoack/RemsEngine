package me.anno.ecs.components.anim.graph

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.graph.visual.states.StateMachine
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

@Docs("Controls animations using a state machine like in Unity")
class AnimGraphController : Component(), OnUpdate {

    @Docs("Source file for animation graph")
    @Type("StateMachine/Reference")
    var graphSource: FileReference = InvalidRef

    @DebugProperty
    @NotSerializedProperty
    var graphInstance: StateMachine? = null

    @DebugProperty
    @NotSerializedProperty
    private var renderer: AnimMeshComponent? = null

    @DebugProperty
    @NotSerializedProperty
    private var lastGraphSource: FileReference = InvalidRef

    @Docs("Whether the animation graph is allowed to load async")
    var asyncLoading = true

    @DebugAction
    fun openEditor() {
        ECSSceneTabs.open(graphSource, PlayMode.EDITING, true)
    }

    override fun onEnable() {
        super.onEnable()
        onChangeStructure(entity ?: return)
    }

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        renderer = entity.getComponent(AnimMeshComponent::class)
    }

    override fun onUpdate() {
        val renderer = renderer
        if (renderer == null) { // wait for renderer
            lastWarning = "Renderer missing"
            return
        }
        val graph = loadGraph()
        if (graph == null) { // wait for a graph
            lastWarning = "Graph missing"
            return
        }
        val newState = graph.update()
        if (newState is AnimStateNode) {
            newState.updateRenderer(renderer)
        } else lastWarning = "Graph is missing default state"
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is AnimGraphController) return
        dst.graphSource = graphSource
    }

    fun loadGraph(): StateMachine? {
        if (lastGraphSource == graphSource) return graphInstance
        val graph = graphSource
        val newInstance = PrefabCache.newInstance(graph)
            .waitFor(asyncLoading) as? StateMachine ?: return null // wait
        lastGraphSource = graph
        graphInstance = newInstance
        return graphInstance
    }
}