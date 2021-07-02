package me.anno.ecs.components.anim

import org.joml.Matrix4f

class SkinAnimation : Animation() {

    val joints = ArrayList<Joint>()

    class Joint {

        val matrices = ArrayList<Matrix4f>()

    }

}