package me.anno.studio

import me.anno.config.DefaultConfig
import me.anno.utils.OS
import java.io.File

object RemsConfig {

    fun init(){

        DefaultConfig.createDefaults = {
            it.apply {

                this["ffmpeg.path"] = File(OS.downloads, "lib\\ffmpeg\\bin\\ffmpeg.exe") // I'm not sure about that one ;)
                this["lastUsed.fonts.count"] = 5
                this["default.video.nearest"] = false
                this["default.image.nearest"] = false

                this["format.svg.stepsPerDegree"] = 0.1f
                this["objects.polygon.maxEdges"] = 1000

                this["rendering.resolutions.default"] = "1920x1080"
                this["rendering.resolutions.defaultValues"] = "1920x1080,1920x1200,720x480,2560x1440,3840x2160"
                this["rendering.resolutions.sort"] = 1 // 1 = ascending order, -1 = descending order, 0 = don't sort
                this["rendering.frameRates"] = "24,30,60,90,120,144,240,300,360"

                this["rendering.useMSAA"] = true // should not be deactivated, unless... idk...
                // this["ui.editor.useMSAA"] = true // can be deactivated for really weak GPUs

                addImportMappings("Transform", "json")
                addImportMappings(
                    "Image",
                    "png", "jpg", "jpeg", "tiff", "webp", "svg", "ico", "psd"
                )
                addImportMappings("Cubemap-Equ", "hdr")
                addImportMappings(
                    "Video",
                    "mp4", "m4p", "m4v", "gif",
                    "mpeg", "mp2", "mpg", "mpe", "mpv", "svi", "3gp", "3g2", "roq",
                    "nsv", "f4v", "f4p", "f4a", "f4b",
                    "avi", "flv", "vob", "wmv", "mkv", "ogg", "ogv", "drc",
                    "mov", "qt", "mts", "m2ts", "ts", "rm", "rmvb", "viv", "asf", "amv"
                )
                addImportMappings("Text", "txt")
                addImportMappings("Mesh", "obj", "fbx", "dae")
                // not yet supported
                // addImportMappings("Markdown", "md")
                addImportMappings("Audio", "mp3", "wav", "m4a")

                this["import.mapping.*"] = "Text"

                newInstances()
            }
        }

        DefaultConfig.init()

    }
}