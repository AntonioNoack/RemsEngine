package me.anno.io.base

import me.anno.audio.effects.SoundPipeline
import me.anno.audio.effects.falloff.ExponentialFalloff
import me.anno.audio.effects.falloff.LinearFalloff
import me.anno.audio.effects.falloff.SquareFalloff
import me.anno.audio.effects.impl.*
import me.anno.io.ISaveable
import me.anno.io.utils.StringMap
import me.anno.objects.*
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Keyframe
import me.anno.objects.animation.drivers.FunctionDriver
import me.anno.objects.animation.drivers.HarmonicDriver
import me.anno.objects.animation.drivers.PerlinNoiseDriver
import me.anno.objects.attractors.EffectColoring
import me.anno.objects.attractors.EffectMorphing
import me.anno.objects.distributions.*
import me.anno.objects.effects.MaskLayer
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.objects.meshes.Mesh
import me.anno.objects.particles.ParticleSystem
import me.anno.objects.particles.forces.impl.*
import me.anno.objects.text.Text
import me.anno.objects.text.Timer
import me.anno.studio.history.History
import me.anno.studio.history.HistoryState
import me.anno.ui.custom.data.CustomListData
import me.anno.ui.custom.data.CustomPanelData
import me.anno.ui.editor.sceneView.SceneTabData
import org.apache.logging.log4j.LogManager

abstract class BaseReader {

    val content = HashMap<Int, ISaveable>()
    val missingReferences = HashMap<Int, ArrayList<Pair<Any, String>>>()
    val sortedContent get() = content.entries.sortedBy { it.key }.map { it.value }.toList()

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
            "Keyframe" -> Keyframe<Any>()
            "HarmonicDriver" -> HarmonicDriver()
            "PerlinNoiseDriver" -> PerlinNoiseDriver()
            "CustomDriver", "FunctionDriver" -> FunctionDriver()
            "CustomListData" -> CustomListData()
            "CustomPanelData" -> CustomPanelData()
            "SceneTabData" -> SceneTabData()
            "ColorAttractor", "EffectColoring" -> EffectColoring()
            "UVAttractor", "EffectMorphing" -> EffectMorphing()
            "SoundPipeline" -> SoundPipeline()
            "EchoEffect" -> EchoEffect()
            "AmplitudeEffect" -> AmplitudeEffect()
            "EqualizerEffect" -> EqualizerEffect()
            "PitchEffect" -> PitchEffect()
            "SquareFalloffEffect" -> SquareFalloff()
            "LinearFalloffEffect" -> LinearFalloff()
            "ExponentialFalloffEffect" -> ExponentialFalloff()
            "AnimatedDistribution" -> AnimatedDistribution()
            "GaussianDistribution" -> GaussianDistribution()
            "ConstantDistribution" -> ConstantDistribution()
            "UniformDistribution" -> UniformDistribution()
            "SphereHullDistribution" -> SphereHullDistribution()
            "SphereVolumeDistribution" -> SphereVolumeDistribution()
            "GlobalForce" -> GlobalForce()
            "GravityField" -> GravityField()
            "LorentzForce" -> LorentzForce()
            "MultiGravityForce" -> MultiGravityForce()
            "TornadoField" -> TornadoField()
            "VelocityFrictionForce" -> VelocityFrictionForce()
            "History" -> History()
            "HistoryState" -> HistoryState()
            else -> {
                // just for old stuff; AnimatedProperties must not be loaded directly; always just copied into
                if(clazz.startsWith("AnimatedProperty<")) AnimatedProperty.any()
                else ISaveable.objectTypeRegistry[clazz]?.invoke() ?: throw RuntimeException("Unknown class '$clazz'")
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

    fun assert(isValue: Char, shallValue: Char, context: String){
        if(isValue != shallValue) throw RuntimeException("Expected $shallValue but got $isValue for $context")
    }

    fun error(msg: String): Nothing = throw RuntimeException("[BaseReader] $msg")

    abstract fun readObject(): ISaveable
    abstract fun readAllInList()

    companion object {
        private val LOGGER = LogManager.getLogger(BaseReader::class)
    }


}