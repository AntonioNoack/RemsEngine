package me.anno.utils

import org.joml.Vector3f
import java.util.*

fun DoubleArray.toVec3() = Vector3f(this[0].toFloat(), this[1].toFloat(), this[2].toFloat())