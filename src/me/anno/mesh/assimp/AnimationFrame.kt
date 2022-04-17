package me.anno.mesh.assimp

import org.joml.Matrix4x3f

fun AnimationFrame(boneCount: Int) = Array(boneCount) { Matrix4x3f() }
