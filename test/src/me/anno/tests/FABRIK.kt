package me.anno.tests

import au.edu.federation.caliko.FabrikBone3D
import au.edu.federation.caliko.FabrikChain3D
import au.edu.federation.utils.Vec3f
import me.anno.utils.LOGGER

fun main() {

    val chain = FabrikChain3D();
    chain.addBone(FabrikBone3D(Vec3f(0f, 0f, 0f), Vec3f(0f, 1f, 0f)))
    chain.addConsecutiveBone(Vec3f(0f, 1f, 0f), 1f)
    chain.addConsecutiveBone(Vec3f(0f, 1f, 0f), 1f)
    chain.addConsecutiveBone(Vec3f(0f, 1f, 0f), 1f)

    LOGGER.info(chain.solveForTarget(1f, 0f, 0f))

    // chain.getBone(0).ballJointConstraintDegs

}