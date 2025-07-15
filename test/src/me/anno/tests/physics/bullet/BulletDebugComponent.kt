package me.anno.tests.physics.bullet

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.dynamics.DynamicsWorld
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.LineShapes
import me.anno.ui.UIColors
import me.anno.utils.Color.black
import org.joml.Matrix4x3
import org.joml.Vector3d

class BulletDebugComponent(
    val world: DynamicsWorld,
    val timeStep: Double, val maxSubSteps: Int
) : Component(), OnUpdate {

    @DebugAction
    fun step() {
        world.stepSimulation(timeStep, maxSubSteps)
    }

    override fun onUpdate() {
        debugRender(world)
    }

    private fun debugRender(dynamicsWorld: DynamicsWorld) {
        // debug render everything
        val m = Matrix4x3()
        for (co in dynamicsWorld.collisionObjects) {

            val tr = co.worldTransform
            val x = DebugLine(tr.origin, tr.transform(Vector3d(1.0, 0.0, 0.0)), UIColors.axisXColor, 0f)
            val y = DebugLine(tr.origin, tr.transform(Vector3d(0.0, 1.0, 0.0)), UIColors.axisYColor, 0f)
            val z = DebugLine(tr.origin, tr.transform(Vector3d(0.0, 0.0, 1.0)), UIColors.axisZColor, 0f)
            DebugShapes.debugLines.addAll(listOf(x, y, z))

            val shape = co.collisionShape
            m.set(tr.basis).setTranslation(tr.origin)
            when (shape) {
                is SphereShape -> LineShapes.drawSphere(null, shape.radius, tr.origin, -1)
                is BoxShape -> {
                    val tmp = shape.getHalfExtentsWithMargin(Vector3d())
                    LineShapes.drawBox(m, -1, tmp.x, tmp.y, tmp.z)
                }
                is StaticPlaneShape -> {
                    for (s in listOf(1.0, 3.0, 9.0)) {
                        LineShapes.drawBox(m, -1, s, 0.0, s)
                    }
                }
                else -> {
                    LineShapes.drawSphere(null, 1.0, tr.origin, 0xff00ff or black)
                }
            }
        }
    }
}