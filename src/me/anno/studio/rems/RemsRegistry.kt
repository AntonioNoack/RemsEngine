package me.anno.studio.rems

import me.anno.animation.AnimatedProperty
import me.anno.animation.Keyframe
import me.anno.animation.drivers.FunctionDriver
import me.anno.animation.drivers.HarmonicDriver
import me.anno.animation.drivers.PerlinNoiseDriver
import me.anno.audio.effects.SoundPipeline
import me.anno.audio.effects.falloff.ExponentialFalloff
import me.anno.audio.effects.falloff.LinearFalloff
import me.anno.audio.effects.falloff.SquareFalloff
import me.anno.audio.effects.impl.AmplitudeEffect
import me.anno.audio.effects.impl.EchoEffect
import me.anno.audio.effects.impl.EqualizerEffect
import me.anno.audio.effects.impl.PitchEffect
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.SaveableArray
import me.anno.io.utils.StringMap
import me.anno.objects.*
import me.anno.objects.attractors.EffectColoring
import me.anno.objects.attractors.EffectMorphing
import me.anno.objects.distributions.*
import me.anno.objects.documents.pdf.PDFDocument
import me.anno.objects.effects.MaskLayer
import me.anno.objects.forces.impl.*
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.LinePolygon
import me.anno.objects.geometric.Polygon
import me.anno.objects.meshes.MeshTransform
import me.anno.objects.particles.ParticleSystem
import me.anno.objects.particles.TextParticles
import me.anno.objects.text.Chapter
import me.anno.objects.text.Text
import me.anno.objects.text.Timer
import me.anno.studio.history.History
import me.anno.studio.history.HistoryState
import me.anno.ui.editor.sceneView.SceneTabData

object RemsRegistry {

    fun init() {

        registerCustomClass(StringMap())
        registerCustomClass(SaveableArray())
        registerCustomClass(Transform())
        registerCustomClass(Text())
        registerCustomClass(Circle())
        registerCustomClass(Polygon())
        registerCustomClass(Video())
        registerCustomClass(GFXArray())
        registerCustomClass(MaskLayer())
        registerCustomClass(ParticleSystem())
        registerCustomClass(Camera())
        registerCustomClass(MeshTransform())
        registerCustomClass(Timer())
        registerCustomClass(AnimatedProperty<Any>())
        registerCustomClass(Keyframe<Any>())
        registerCustomClass(HarmonicDriver())
        registerCustomClass(PerlinNoiseDriver())
        registerCustomClass(FunctionDriver())
        registerCustomClass(SceneTabData())
        registerCustomClass(EffectColoring())
        registerCustomClass(EffectMorphing())
        registerCustomClass(SoundPipeline())
        registerCustomClass(EchoEffect())
        registerCustomClass(AmplitudeEffect())
        registerCustomClass(EqualizerEffect())
        registerCustomClass(PitchEffect())
        registerCustomClass(SquareFalloff())
        registerCustomClass(LinearFalloff())
        registerCustomClass(ExponentialFalloff())
        registerCustomClass(AnimatedDistribution())
        registerCustomClass(GaussianDistribution())
        registerCustomClass(ConstantDistribution())
        registerCustomClass(CuboidDistribution())
        registerCustomClass(CuboidHullDistribution())
        registerCustomClass(SphereHullDistribution())
        registerCustomClass(SphereVolumeDistribution())
        registerCustomClass(GlobalForce())
        registerCustomClass(GravityField())
        registerCustomClass(LorentzForce())
        registerCustomClass(NoisyLorentzForce())
        registerCustomClass(BetweenParticleGravity())
        registerCustomClass(TornadoField())
        registerCustomClass(VelocityFrictionForce())
        registerCustomClass(History())
        registerCustomClass(HistoryState())
        registerCustomClass(TextParticles())
        registerCustomClass(SoftLink())
        registerCustomClass(PDFDocument())
        registerCustomClass(LinePolygon())
        registerCustomClass(FourierTransform())
        registerCustomClass(Chapter())

    }

}