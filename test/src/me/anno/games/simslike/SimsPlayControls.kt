package me.anno.games.simslike

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponents
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.mix
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.utils.types.Floats.toRadians
import kotlin.math.abs
import kotlin.math.sin

class SimsPlayControls(controls: SimsControls, rv: RenderView) :
    SimsControlBase(controls, rv) {

    var currentSim = household.sims.first()

    val actionStack = PanelListY(style)

    init {

        // add sim-selection bar
        val simsSelectBar = PanelListX(style)
        simsSelectBar.alignmentX = AxisAlignment.CENTER
        simsSelectBar.alignmentY = AxisAlignment.MAX
        for (sim in household.sims) {
            simsSelectBar.add(
                TextButton(sim.nameDesc, style)
                    .addLeftClickListener {
                        currentSim = sim
                        showActionStack()
                    }
            )
        }
        add(simsSelectBar)

        showActionStack()
        actionStack.alignmentX = AxisAlignment.MIN
        actionStack.alignmentY = AxisAlignment.MAX
        add(actionStack)

        add(
            TextButton(NameDesc("Build"), style)
                .addLeftClickListener {
                    // switch to build mode
                    sceneView.editControls = controls.buildControls
                    controls.buildControls.rotationTargetDegrees.set(rotationTargetDegrees)
                }
                .apply {
                    alignmentX = AxisAlignment.MAX
                    alignmentY = AxisAlignment.MAX
                })
    }

    override fun onUpdate() {
        super.onUpdate()
        if (actionStack.children.size != currentSim.actions.size) {
            showActionStack()
        }
    }

    fun showActionStack() {
        actionStack.clear()
        val actions = currentSim.actions
        for (i in actions.lastIndex downTo 0) {
            val action = actions[i]
            actionStack.add(
                TextButton(action.nameDesc, style)
                    .addLeftClickListener {
                        action.state = ActionState.CANCEL
                    })
        }
    }

    fun onClickedActions(entity: Entity, actions: List<SimAction>, hit: RayHit) {
        // todo make action cancellable
        // to do group them?
        Menu.openMenu(windowStack, actions.map { action0 ->
            MenuOption(action0.nameDesc) {
                // we might need this action multiple times, so clone it!
                // todo how do we handle, that we might need an action multiple times / in multiple sims, but still need individual data???
                val action = action0 // .clone() as SimAction

                when {
                    action.isSimTarget -> {
                        // todo if is sim action, decide angle for interaction
                        // todo also register action in other sim, so they can find together
                    }
                    action.isObjectTarget -> {
                        // fine
                        // todo deny if on fire?
                    }
                    else -> {
                        // walking target
                        action.offset.set(hit.positionWS)
                    }
                }

                action.state = ActionState.READY

                // add action to current character
                currentSim.actions.add(action)
                showActionStack()
            }
        })
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            // move around by dragging
            val xSpeed = (-pixelsToWorldFactor * renderView.radius / height).toFloat()
            val ry = rotationTargetDegrees.y.toRadians()
            val ySpeed = xSpeed / mix(1f, abs(sin(ry)), 0.5f)
            moveCamera(dx * xSpeed, 0f, dy * ySpeed)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        // resolve click
        val query = renderView.rayQuery()
        query.collisionMask = clickCollision
        if (Raycast.raycast(scene, query)) {
            val comp = query.result.component
            var entity = comp?.entity
            while (entity != null) {
                val actions = entity.getComponents(SimAction::class)
                if (actions.isNotEmpty()) {
                    onClickedActions(entity, actions, query.result)
                    return
                }
                entity = entity.parentEntity
            }
        }// else hit the sky... idk about that
    }
}