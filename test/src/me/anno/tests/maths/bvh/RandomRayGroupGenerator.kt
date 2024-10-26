package me.anno.tests.maths.bvh

import me.anno.maths.bvh.RayGroup
import me.anno.tests.maths.bvh.HitsSphere.hitsSphere
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.Booleans.toInt
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.random.Random

class RandomRayGroupGenerator {

    val gen = RandomRayGenerator()

    val groupSize = 64
    val random = Random(12345)
    val dirI = Vector3f()
    val rayGroupDirs = createList(groupSize) {
        Quaternionf()
            .rotateX((random.nextFloat() - 0.5f) * 0.05f)
            .rotateY((random.nextFloat() - 0.5f) * 0.05f)
            .rotateZ((random.nextFloat() - 0.5f) * 0.05f)
    }

    val rayGroup = RayGroup(8, 8, RayGroup(8, 8))

    fun next() {
        gen.next()
        rayGroup.setMain(gen.pos, gen.dir, 1e3f)
        for (j in 0 until groupSize) {
            gen.dir.rotate(rayGroupDirs[j], dirI)
            rayGroup.setRay(j, dirI)
        }
    }

    fun check(): Int {
        var ctr = 0
        for (j in 0 until groupSize) {
            gen.dir.rotate(rayGroupDirs[j], dirI)
            val hitsSphere = rayGroup.depths[j] < 2f
            val shouldHitSphere = hitsSphere(gen.pos, dirI)
            ctr += (shouldHitSphere == hitsSphere).toInt()
        }
        return ctr
    }
}