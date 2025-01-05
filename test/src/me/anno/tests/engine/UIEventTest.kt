package me.anno.tests.engine

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.interfaces.InputListener
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.control.PlayControls
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Key
import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("UIEventTest")
fun main() {
    val scene = Entity()
        .add(MeshComponent(flatCube))
        .add(object : Component(), InputListener {
            override fun onKeyTyped(key: Key): Boolean {
                val transform = transform ?: return false
                when (key) {
                    Key.KEY_W -> transform.translateLocal(0.0, +0.2, 0.0)
                    Key.KEY_S -> transform.translateLocal(0.0, -0.2, 0.0)
                    Key.KEY_A -> transform.translateLocal(-0.2, 0.0, 0.0).teleportUpdate()
                    Key.KEY_D -> transform.translateLocal(+0.2, 0.0, 0.0).teleportUpdate()
                    else -> LOGGER.warn("Unknown key $key")
                }
                return true
            }
        })
    testSceneWithUI("UIEventTest", scene) {
        it.editControls = PlayControls(it.renderView)
    }
}