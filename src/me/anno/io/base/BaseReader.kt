package me.anno.io.base

import me.anno.audio.effects.SoundPipeline
import me.anno.audio.effects.falloff.ExponentialFalloff
import me.anno.audio.effects.falloff.LinearFalloff
import me.anno.audio.effects.falloff.SquareFalloff
import me.anno.audio.effects.impl.AmplitudeEffect
import me.anno.audio.effects.impl.EchoEffect
import me.anno.audio.effects.impl.EqualizerEffect
import me.anno.audio.effects.impl.PitchEffect
import me.anno.io.ISaveable
import me.anno.io.InvalidFormatException
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
import me.anno.objects.forces.impl.*
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.objects.meshes.Mesh
import me.anno.objects.particles.ParticleSystem
import me.anno.objects.particles.TextParticles
import me.anno.objects.documents.pdf.PDFDocument
import me.anno.objects.geometric.LinePolygon
import me.anno.objects.text.Text
import me.anno.objects.text.Timer
import me.anno.studio.history.History
import me.anno.studio.history.HistoryState
import me.anno.ui.editor.sceneView.SceneTabData
import me.anno.utils.structures.arrays.EfficientBooleanArray
import org.apache.logging.log4j.LogManager

abstract class BaseReader {

    val content = HashMap<Int, ISaveable>()
    val sortedContent get() = content.entries.sortedBy { it.key }.map { it.value }.toList()
    private val missingReferences = HashMap<Int, ArrayList<Pair<Any, String>>>()

    fun register(value: ISaveable, ptr: Int) {
        if (ptr != 0) {
            content[ptr] = value
            missingReferences[ptr]?.forEach { (obj, name) ->
                when (obj) {
                    is ISaveable -> {
                        obj.readObject(name, value)
                    }
                    is MissingListElement -> {
                        obj.target[obj.targetIndex] = value
                    }
                    else -> throw RuntimeException("Unknown missing reference type")
                }
            }
        } else if (ptr == 0) LOGGER.warn("Got object with uuid $ptr: $value, it will be ignored")
    }

    fun addMissingReference(owner: Any, name: String, childPtr: Int) {
        val list = missingReferences[childPtr]
        val entry = owner to name
        if (list != null) {
            list += entry
        } else {
            missingReferences[childPtr] = arrayListOf(entry)
        }
    }

    fun assert(b: Boolean) {
        if (!b) throw InvalidFormatException("Assertion failed")
    }

    fun assert(b: Boolean, msg: String) {
        if (!b) throw InvalidFormatException(msg)
    }

    fun assert(isValue: String, shallValue: String) {
        if (!isValue.equals(shallValue, true)) {
            throw InvalidFormatException("Expected $shallValue but got $isValue")
        }
    }

    fun assert(isValue: Char, shallValue: Char) {
        if (isValue != shallValue.toLowerCase() && isValue != shallValue.toUpperCase()) {
            throw InvalidFormatException("Expected $shallValue but got $isValue")
        }
    }

    fun assert(isValue: Char, shallValue: Char, context: String) {
        if (isValue != shallValue.toLowerCase() && isValue != shallValue.toUpperCase()) {
            throw InvalidFormatException("Expected $shallValue but got $isValue for $context")
        }
    }

    abstract fun readObject(): ISaveable
    abstract fun readAllInList()

    companion object {

        private val LOGGER = LogManager.getLogger(BaseReader::class)

        fun error(msg: String): Nothing = throw InvalidFormatException("[BaseReader] $msg")
        fun error(msg: String, appended: Any?): Nothing = throw InvalidFormatException("[BaseReader] $msg $appended")

        fun getNewClassInstance(clazz: String): ISaveable {
            return when (clazz) {
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
                "UniformDistribution", // replaced
                "CuboidDistribution" -> CuboidDistribution()
                "CuboidHullDistribution" -> CuboidHullDistribution()
                "SphereHullDistribution" -> SphereHullDistribution()
                "SphereDistribution",
                "SphereVolumeDistribution" -> SphereVolumeDistribution()
                "GlobalForce" -> GlobalForce()
                "GravityField" -> GravityField()
                "LorentzForce" -> LorentzForce()
                "NoisyLorentzForce" -> NoisyLorentzForce()
                "MultiGravityForce" -> BetweenParticleGravity()
                "TornadoField" -> TornadoField()
                "VelocityFrictionForce" -> VelocityFrictionForce()
                "History" -> History()
                "HistoryState" -> HistoryState()
                "BoolArray" -> EfficientBooleanArray()
                "TextParticles" -> TextParticles()
                "SoftLink" -> SoftLink()
                "PDFDocument" -> PDFDocument()
                "LinePolygon" -> LinePolygon()
                else -> {
                    // just for old stuff; AnimatedProperties must not be loaded directly; always just copied into
                    if (clazz.startsWith("AnimatedProperty<")) AnimatedProperty.any()
                    else ISaveable.objectTypeRegistry[clazz]?.invoke() ?: throw UnknownClassException(clazz)
                }
            }
        }
    }


}