package me.anno.tests.collider

import me.anno.ecs.components.collider.SphereCollider
import me.anno.engine.raycast.RayQueryLocal
import me.anno.maths.bvh.HitType
import me.anno.utils.types.Floats.step
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.sqrt

/* correctness test for sphere collider ray tests */
fun main() {
    val s = SphereCollider()
    val offset = 1f
    for (x in -2f..2f step 0.1f) {
        val query = RayQueryLocal(Vector3f(x, 0f, -offset), Vector3f(0f, 0f, 1f), 10f, HitType.CLOSEST)
        val distance = s.raycast(query, null)
        val target = if (abs(x) > 1f) Float.POSITIVE_INFINITY else offset - sqrt(1f - x * x)
        println("$x -> $distance, error: ${if (distance == target) "ok" else (distance - target) / distance}")
    }
}