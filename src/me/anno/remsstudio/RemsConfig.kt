package me.anno.remsstudio

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.utils.StringMap
import me.anno.remsstudio.objects.*
import me.anno.remsstudio.objects.attractors.EffectColoring
import me.anno.remsstudio.objects.attractors.EffectMorphing
import me.anno.remsstudio.objects.effects.MaskLayer
import me.anno.remsstudio.objects.geometric.Circle
import me.anno.remsstudio.objects.geometric.Polygon
import me.anno.remsstudio.objects.meshes.MeshTransform
import me.anno.remsstudio.objects.modes.UVProjection
import me.anno.remsstudio.objects.particles.ParticleSystem
import me.anno.remsstudio.objects.particles.TextParticles
import me.anno.remsstudio.objects.text.Text
import me.anno.remsstudio.objects.text.Timer
import me.anno.utils.Clock
import me.anno.utils.OS
import org.joml.Vector3f

object RemsConfig {

    fun init() {

        DefaultConfig.apply {

            // I'm not sure about that one ;)
            this["ffmpeg.path", getReference(OS.downloads, "lib\\ffmpeg\\bin\\ffmpeg.exe")]

            this["lastUsed.fonts.count", 5]
            this["default.video.nearest", false]
            this["default.image.nearest", false]

            this["format.svg.stepsPerDegree", 0.1f]
            this["objects.polygon.maxEdges", 1000]

            this["rendering.resolutions.default", "1920x1080"]
            this["rendering.resolutions.defaultValues", "1920x1080,1920x1200,720x480,2560x1440,3840x2160"]
            this["rendering.resolutions.sort", 1] // 1 = ascending order, -1 = descending order, 0 = don't sort
            this["rendering.frameRates", "24,30,60,90,120,144,240,300,360"]

            this["rendering.useMSAA", true] // should not be deactivated, unless... idk...
            this["ui.editor.useMSAA", true] // can be deactivated for really weak GPUs

            defineDefaultFileAssociations()
            addImportMappings("Transform", "json")
            this["import.mapping.*", "Text"]

            newInstances()
        }

        RemsVersionFeatures(DefaultConfig).addNewPackages(DefaultConfig)

    }

    fun newInstances() {

        val tick = Clock()

        val newInstances: Map<String, Transform> = mapOf(
            "Mesh" to MeshTransform(getReference(OS.documents, "monkey.obj"), null),
            "Array" to GFXArray(),
            "Image / Audio / Video" to Video(),
            "Polygon" to Polygon(null),
            "Rectangle" to Rectangle.create(),
            "Circle" to Circle(null),
            "Folder" to Transform(),
            // "Linked Object" to SoftLink(), // non-default, can be created using drag n drop
            "Mask" to MaskLayer.create(null, null),
            "Text" to Text("Text"),
            "Timer" to Timer(),
            "Cubemap" to run {
                val cube = Video()
                cube.uvProjection *= UVProjection.TiledCubemap
                cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                cube
            },
            "Cube" to run {
                val cube = Polygon()
                cube.name = "Cube"
                cube.autoAlign = true
                cube.is3D = true
                cube.vertexCount.set(4)
                cube
            },
            "Camera" to Camera(),
            "Particle System" to run {
                val ps = ParticleSystem()
                ps.name = "Particles"
                Circle(ps)
                ps.timeOffset.value = -5.0
                ps
            },
            "Text Particles" to TextParticles(),
            "Effect: Coloring" to EffectColoring(),
            "Effect: Morphing" to EffectMorphing()
        )

        val value = StringMap(16, false).addAll(newInstances)
        DefaultConfig["createNewInstancesList", value]

        tick.stop("new instances list")

    }
}