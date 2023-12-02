package me.anno.tests.engine

import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.io.utils.StringMap
import org.joml.*

class TypeTestComponent : Component() {

    var someBoolean = true

    @Range(0.0, 15.0)
    var someByte: Byte = 0

    @Range(10.0, 500.0)
    var someShort: Short = 150

    var someInt = 0

    var someFloat = 0.4f

    var someDouble = 0.4

    var someLong = 5613213L

    // to do annotation for text types: names, email, password,
    var someString = ""

    var someVec2 = Vector2f()

    var someVec3 = Vector3d()

    var someMat3 = Matrix3d()

    var someMat4 = Matrix4f()

    var someMat4x3 = Matrix4x3f()

    var someVecInt = Vector3i(23, 43, 1)

    var someArray = arrayOf("a", "b", "c")

    var someIntArray = intArrayOf(1, 2, 3)

    var someShortArray = shortArrayOf(4, 5, 6)

    var someLongArray = longArrayOf(435, 654, -15)

    var someMap = mapOf(
        "key" to "value",
        "key2" to "value2"
    )

    var someStringMap = StringMap(mapOf("key3" to "value3"))

    var someIntMap = hashMapOf(1 to 2, 5 to -17)

    var someRot = Quaternionf()

    var someRotDouble = Quaterniond(0.707, 0.0, 0.0, 0.707).normalize()

    override val className: String get() = "TypeTestComponent"

}