package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode
import me.anno.utils.types.Vectors.print
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f

class FBXModel(data: FBXNode) : FBXObject(data) {

    // val shading = data.getProperty("Shading") as? Boolean ?: true
    // val culling = data.getProperty("Culling") as? String ?: "CullingOff" // CullingOff, ...

    var localTranslation = Vector3f()
    var localRotation = Vector3f()
    var localScale = Vector3f(1f)

    val rotationPivot = Vector3f()
    val scalingPivot = Vector3f()

    var rotationActive = true // ???

    var show = true

    fun printProperties(){
        LOGGER.info("$name: ${localTranslation.print()} ${localRotation.print()} ${localScale.print()} | ${rotationPivot.print()} ${scalingPivot.print()}")
    }

    override fun onReadProperty70(name: String, value: Any) {
        when (name) {
            "ScalingMin", "ScalingMax" -> {
                // vec3:0,0,0 ???
            }
            "DefaultAttributeIndex" -> {
                // int:0 ???
            }
            "Lcl Translation" -> localTranslation = value as Vector3f
            "Lcl Rotation" -> localRotation = value as Vector3f
            "Lcl Scaling" -> localScale = value as Vector3f
            "RotationActive" -> rotationActive = value as Boolean
            "Show" -> show = value as Boolean
            "MultiTake", "InheritType", "ManipulationMode",
            "ScalingPivotUpdateOffset",
            "PreferedAngleX", "PreferedAngleY", "PreferedAngleZ",
            "SetPreferedAngle", // spelling mistake...
            "PivotsVisibility", "RotationLimitsVisibility",
            "RotationRefVisibility", "RotationAxisVisibility",
            "ScalingRefVisibility", "HierarchicalCenterVisibility",
            "GeometricCenterVisibility", "ReferentialSize",
            "DefaultKeyingGroup", "DefaultKeyingGroupEnum",
            "Pickable", "Transformable", "CullingMode",// difference to culling?
            "ShowTrajectories",
            "lockInfluenceWeights", "liw", // liw = lock influence weights by Maja
            "LocalTranslationRefVisibility" -> {
            } // idc
            "ForegroundTransparent" -> {}
            "BackgroundMode" -> {} // whatever...
            "ViewFrustum" -> {} // idc
            "AspectW", "AspectH" -> {} // looks like camera settings...
            "ResolutionMode" -> {} // idk, idc?
            "RotationPivot", "RotationOffset" -> rotationPivot.set(value as Vector3f)
            "ScalingPivot", "ScalingOffset" -> scalingPivot.set(value as Vector3f)
            "currentUVSet" -> {} // map1?
            else -> super.onReadProperty70(name, value)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(FBXModel::class)
    }

}