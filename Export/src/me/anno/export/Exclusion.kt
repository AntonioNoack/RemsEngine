package me.anno.export

import me.anno.utils.structures.maps.Maps.removeIf

object Exclusion {

    fun excludeLWJGLFiles(sources: HashMap<String, ByteArray>, settings: ExportSettings) {
        excludeFiles(sources, settings.linuxPlatforms.any, "org/lwjgl/system/linux/")
        excludeFiles(sources, settings.linuxPlatforms.arm64, "linux/arm64/")
        excludeFiles(sources, settings.linuxPlatforms.arm32, "linux/arm32/")
        excludeFiles(sources, settings.linuxPlatforms.x64, "linux/x64/")

        excludeFiles(sources, settings.windowsPlatforms.any, "org/lwjgl/system/windows/")
        excludeFiles(sources, settings.windowsPlatforms.arm64, "windows/arm64/")
        excludeFiles(sources, settings.windowsPlatforms.x86, "windows/x86/")
        excludeFiles(sources, settings.windowsPlatforms.x64, "windows/x64/")

        excludeFiles(sources, settings.macosPlatforms.any, "org/lwjgl/system/macosx/")
        excludeFiles(sources, settings.macosPlatforms.arm64, "macos/arm64/")
        excludeFiles(sources, settings.macosPlatforms.x64, "macos/x64/")
    }

    fun excludeJNAFiles(sources: HashMap<String, ByteArray>, settings: ExportSettings) {
        val anyMacos = settings.macosPlatforms.any
        val anyLinux = settings.linuxPlatforms.any
        val anyWindows = settings.windowsPlatforms.any
        sources.removeIf { (key, _) ->
            if (key.startsWith("com/sun/jna/")) {
                key.startsWith("com/sun/jna/aix-") ||
                        key.startsWith("com/sun/jna/freebsd") ||
                        key.startsWith("com/sun/jna/linux-mips") ||
                        key.startsWith("com/sun/jna/linux-ppc") ||
                        key.startsWith("com/sun/jna/linux-s390x") ||
                        key.startsWith("com/sun/jna/darwin") ||
                        key.startsWith("com/sun/jna/sunos-") ||
                        key.startsWith("com/sun/jna/openbsd-") ||
                        key.startsWith("com/sun/jna/linux-armel/") ||
                        key.startsWith("com/sun/jna/linux-x86/") ||
                        key.startsWith("com/sun/jna/platform/wince") ||
                        key.startsWith("com/sun/jna/platform/unix/aix/") ||
                        key.startsWith("com/sun/jna/platform/unix/solaris/") ||
                        (!anyMacos && key.startsWith("com/sun/jna/platform/mac/")) ||
                        (!anyLinux && key.startsWith("com/sun/jna/platform/unix/")) || // correct??
                        (!anyLinux && key.startsWith("com/sun/jna/platform/linux/")) ||
                        (!anyWindows && key.startsWith("com/sun/jna/platform/win32/")) ||
                        (!settings.windowsPlatforms.arm64 && key.startsWith("com/sun/jna/win32-aarch64")) ||
                        (!settings.windowsPlatforms.x86 && key.startsWith("com/sun/jna/win32-x86/")) ||
                        (!settings.windowsPlatforms.x64 && key.startsWith("com/sun/jna/win32-x86-64/")) ||
                        (!settings.linuxPlatforms.arm64 && key.startsWith("com/sun/jna/linux-aarch64/")) ||
                        (!settings.linuxPlatforms.arm32 && key.startsWith("com/sun/jna/linux-arm/")) ||
                        (!settings.linuxPlatforms.x64 && key.startsWith("com/sun/jna/linux-x86-64/"))
            } else false
        }
    }

    fun excludeWebpFiles(sources: HashMap<String, ByteArray>, settings: ExportSettings) {
        excludeFiles(sources, settings.linuxPlatforms.any, "native/linux/")
        excludeFiles(sources, settings.windowsPlatforms.any, "native/win/")
        excludeFiles(sources, settings.macosPlatforms.any, "native/mac/")
    }

    fun excludeNonMinimalUI(sources: HashMap<String, ByteArray>, customReflections: Boolean) {
        // UI in engine
        sources.removeIf { it.key.endsWith(".md") }
        excludeFiles(sources, "me/anno/ui/editor/", listOf("stacked/Option.class", "OptionBar"))
        excludeFiles(sources, "me/anno/engine/ui/input/")
        excludeFiles(sources, "me/anno/ui/input/")
        excludeFiles(sources, "me/anno/ui/debug/", listOf("FrameTimings"))
        excludeFiles(sources, "me/anno/ui/anim/")
        excludeFiles(sources, "me/anno/ui/custom/", listOf("CustomPanelType", "UITypeLibrary"))
        excludeFiles(sources, "me/anno/ui/base/image/")
        excludeFiles(sources, "me/anno/ui/base/buttons/")
        excludeFiles(
            sources, "me/anno/ui/base/groups/",
            listOf("PanelGroup", "PanelContainer", "PanelList", "PanelStack", "NineTilePanel", "ListSizeCalculator")
        )
        excludeFiles(sources, "me/anno/image/thumbs/") // mostly just used in UI
        excludeFiles(sources, "me/anno/engine/ui/ECSTreeView")
        excludeFiles(sources, "me/anno/engine/ui/ECSFileExplorer")
        excludeFiles(sources, "me/anno/engine/ui/scenetabs")
        excludeFiles(sources, "me/anno/engine/ui/control/Blender")
        excludeFiles(sources, "me/anno/engine/ui/control/Dragging")
        excludeFiles(sources, "me/anno/maths/", listOf("Maths", "bvh", "Packing"))
        excludeFiles(sources, "me/anno/network/")
        // other engine things
        excludeFiles(sources, "textures") // I'm not too sure about this...
        excludeFiles(sources, "assets/org/apache/commons") // what is this used for???
        // other things...
        excludeFiles(sources, "net/boeckling/crc/")
        excludeFiles(sources, "com/sun/jna") // moving to trash is quite niche when not using UI
        excludeFiles(sources, "me/anno/jvm/utils/CommandLineUtils")
        excludeFiles(sources, "org/apache/commons/cli/")
        excludeFiles(sources, "org/jtransforms/")
        excludeFiles(sources, "pl/edu/icm/jlargearrays/")
        excludeFiles(sources, "org/apache/commons/math3/")
        // kotlin standard library
        excludeFiles(sources, "kotlin/coroutines/")
        excludeFiles(sources, "kotlin/time/")
        excludeFiles(sources, "kotlin/streams/")
        excludeFiles(
            sources, "kotlin/text/", listOf(
                "Regex", "StringsKt", "CharsKt", "Charsets", "DelimitedRangesSequence",
                "ScreenFloatValueRegEx"
            )
        )
        excludeFiles(sources, "kotlin/io/", listOf("CloseableKt", "ByteStreamsKt", "FilesKt"))
        excludeFiles(sources, "kotlin/collections/unsigned/")
        excludeFiles(sources, "kotlin/test/")
        excludeFiles(sources, "kotlin/ranges/U")
        // opengl
        excludeFiles(sources, "org/lwjgl/opengl/", listOf("GL", "WGL"))
        excludeFiles(sources, "org/lwjgl/opengl/WGLA")
        excludeFiles(sources, "org/lwjgl/opengl/GLX")
        // more of kotlin standard library
        if (customReflections) {
            excludeFiles(sources, "kotlin/sequences/", listOf("Sequence"))
            excludeFiles(sources, "kotlin/collections/builders/")
            // used by toFloatOrNull()
            // excludeFiles(sources, "kotlin/text/Regex")
        }
    }

    fun excludeFiles(sources: HashMap<String, ByteArray>, flag: Boolean, path: String) {
        if (!flag) excludeFiles(sources, path)
    }

    fun excludeFiles(sources: HashMap<String, ByteArray>, path: String) {
        sources.removeIf {
            it.key.startsWith(path)
        }
    }

    fun excludeFiles(sources: HashMap<String, ByteArray>, path: String, except: List<String>) {
        sources.removeIf { (srcFile, _) ->
            val i = path.length
            srcFile.startsWith(path) && except.none { srcFile.startsWith(it, i) }
        }
    }
}