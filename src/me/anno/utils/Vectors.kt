package me.anno.utils

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.joml.Vector3f

fun Vector3f.toCommonsMath() = Vector3D(x.toDouble(), y.toDouble(), z.toDouble())