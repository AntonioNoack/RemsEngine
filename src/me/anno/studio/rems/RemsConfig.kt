package me.anno.studio.rems

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS

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
}