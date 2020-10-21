package me.anno.io.base

import me.anno.io.ISaveable
import me.anno.io.utils.StringMap
import me.anno.objects.*
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Keyframe
import me.anno.objects.animation.drivers.FunctionDriver
import me.anno.objects.animation.drivers.HarmonicDriver
import me.anno.objects.animation.drivers.PerlinNoiseDriver
import me.anno.objects.attractors.ColorAttractor
import me.anno.objects.attractors.UVAttractor
import me.anno.objects.effects.MaskLayer
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.objects.meshes.Mesh
import me.anno.objects.particles.ParticleSystem
import me.anno.ui.custom.data.CustomListData
import me.anno.ui.custom.data.CustomPanelData
import me.anno.ui.editor.sceneView.SceneTabData
import me.anno.utils.LOGGER

abstract class BaseReader {

    val content = HashMap<Int, ISaveable>()
    val missingReferences = HashMap<Int, ArrayList<Pair<Any, String>>>()

    fun getNewClassInstance(clazz: String): ISaveable {
        return when(clazz){
            "SMap" -> StringMap()
            "Transform" -> Transform()
            "Text" -> Text()
            "Circle" -> Circle()
            "Polygon" -> Polygon()
            "Video", "Audio", "Image" -> Video()
            "GFXArray" -> GFXArray()
            "MaskLayer" -> MaskLayer()
            "ParticleSystem" -> ParticleSystem()
            "Camera" -> Camera()
            "Mesh" -> Mesh()
            "Timer" -> Timer()
            "AnimatedProperty" -> AnimatedProperty.any()
            "Keyframe" -> Keyframe<Any>(0.0, 0f)
            "HarmonicDriver" -> HarmonicDriver()
            "PerlinNoiseDriver" -> PerlinNoiseDriver()
            "CustomDriver", "FunctionDriver" -> FunctionDriver()
            "CustomListData" -> CustomListData()
            "CustomPanelData" -> CustomPanelData()
            "SceneTabData" -> SceneTabData()
            "ColorAttractor" -> ColorAttractor()
            "UVAttractor" -> UVAttractor()
            else -> {
                // just for old stuff; AnimatedProperties must not be loaded directly; always just copied into
                if(clazz.startsWith("AnimatedProperty<")) AnimatedProperty.any()
                else ISaveable.objectTypeRegistry[clazz]?.invoke() ?: throw RuntimeException("Unknown class $clazz")
            }
        }
    }

    fun register(value: ISaveable, ptr: Int){
        if(ptr != 0){
            content[ptr] = value
            missingReferences[ptr]?.forEach { (obj, name) ->
                when(obj){
                    is ISaveable -> {
                        obj.readObject(name, value)
                    }
                    is MissingListElement -> {
                        obj.target[obj.targetIndex] = value
                    }
                    else -> throw RuntimeException("Unknown missing reference type")
                }
            }
        } else LOGGER.warn("Got object with uuid 0: $value, it will be ignored")
    }

    fun addMissingReference(owner: Any, name: String, childPtr: Int){
        val list = missingReferences[childPtr]
        val entry = owner to name
        if(list != null){
            list += entry
        } else {
            missingReferences[childPtr] = arrayListOf(entry)
        }
    }

    fun assert(b: Boolean, msg: String){
        if(!b) throw RuntimeException(msg)
    }

    fun assert(isValue: Char, shallValue: Char){
        if(isValue != shallValue) throw RuntimeException("Expected $shallValue but got $isValue")
    }

    fun error(msg: String): Nothing = throw RuntimeException("[BaseReader] $msg")


}