package me.anno.ecs.components.anim

import org.joml.Matrix4f

class BoneByBoneAnimation : Animation() {

    val joints = ArrayList<Joint>()

    class Joint {

        val matrices = ArrayList<Matrix4f>()

    }

}