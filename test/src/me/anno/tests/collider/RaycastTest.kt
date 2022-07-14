package me.anno.tests.collider

import me.anno.ecs.components.collider.Collider
import me.anno.image.ImageWriter
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
    val dir = Quaternionf()
        .rotateY(30f.toRadians())
    val w = if (heavy) 256 else 1024
    val h = if (heavy) 256 else 1024
    val fov = 90f.toRadians()
    val fovY = fov / h
    val start = Vector3f(0f, 0f, 1f)
        .rotate(dir)
        .mul(2f)
    writeImage(w, h, heavy, "img/$name-r.png", true) { x, y ->
        val rayDir = Vector3f(0f, 0f, -1f)
            .rotateX((y - h / 2) * -fovY)
            .rotateY((x - w / 2) * +fovY)
            .rotate(dir)
        b.raycast(start, rayDir, 0f, 0f, null, 10f)
    }
}

fun main() {
    for (shape in testShapes) {
        renderCollider(shape)
    }
}