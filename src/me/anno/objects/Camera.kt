package me.anno.objects

import me.anno.objects.animation.AnimatedProperty
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

class Camera {

    // todo allow cameras to be merged
    // todo allow cameras to film camera (post processing) -> todo create a stack of cameras/scenes?

    var matchViewport = AnimatedProperty.float().set(1f)

    var nearZ = AnimatedProperty.float().set(0.001f)
    var farZ = AnimatedProperty.float().set(1000f)
    var position = AnimatedProperty.pos().set(Vector3f(0f,0f,1f))
    var lookAt = AnimatedProperty.pos().set(Vector3f(0f, 0f, 0f))
    var fovDegrees = AnimatedProperty.float().set(90f)

    var colorMapping = 0

    var focusButton = 0
    var inverseFocusButton = 0


}