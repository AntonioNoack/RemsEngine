package me.anno.tests.collider

import me.anno.Engine
import me.anno.ecs.components.collider.Collider
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayQueryLocal
import me.anno.image.ImageWriter
import me.anno.maths.bvh.HitType
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3f

fun writeImage(
    w: Int,
    h: Int,
    heavy: Boolean,
    name: String,
    normalize: Boolean,
    getValue: (x: Float, y: Float) -> Float
) {
    if (heavy) {
        ImageWriter.writeImageFloat(w, h, name, 512, normalize) { x, y, _ ->
            getValue(x.toFloat(), y.toFloat())
        }
    } else {
        ImageWriter.writeImageFloatMSAA(w, h, name, 64, normalize, getValue)
    }
}

/* test for box ray collisions: defines a fish-eye camera, and renders the distance to the box */
fun renderCollider(b: Collider, name: String = b.name.ifBlank { b.className }) {
    LogManager.getLogger("RaycastTest").info("Rendering $name")
    val heavy = name == "Monkey"
    val rotation = Quaternionf()
        .rotateY(30f.toRadians())
    val w = if (heavy) 256 else 1024
    val h = if (heavy) 256 else 1024
    val fov = 90f.toRadians()
    val fovY = fov / h
    val start = Vector3f(0f, 0f, 3f)
        .rotate(rotation)
    writeImage(w, h, heavy, "img/$name-r.png", true) { x, y ->
        val dir = JomlPools.vec3f.create()
            .set(0f, 0f, -1f)
            .rotateX((y - h / 2) * -fovY)
            .rotateY((x - w / 2) * +fovY)
            .rotate(rotation)
        val query = RayQueryLocal(start, dir, 10f, 0f, 0f, HitType.CLOSEST)
        val dist = b.raycast(query, null)
        JomlPools.vec3f.sub(1)
        dist
        /*  val hit = RayHit()
          val b = b.raycast(
              sampleEntity, Vector3d(start), Vector3d(dir), Vector3d(dir).mul(10.0).add(start),
              0.0, 0.0, -1, false, hit
          )
          if (b) hit.distance.toFloat()
          else Float.POSITIVE_INFINITY*/
    }
}

fun main() {

    OfficialExtensions.initForTests()

    for (shape in testShapes.filter { it.name == "Monkey" }) {
        renderCollider(shape)
    }
    Engine.requestShutdown()
}