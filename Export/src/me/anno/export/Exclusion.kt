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

        excludeFiles(sources, settings.macosPlatforms.any, "org/lwjgl/system/macos/")
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

    fun excludeNonMinimalUI(sources: HashMap<String, ByteArray>) {
        excludeFiles(sources, "me/anno/ui/editor", listOf("me/anno/ui/editor/stacked/Option.class"))
        excludeFiles(sources, "me/anno/ui/input")
        excludeFiles(sources, "me/anno/ui/debug", listOf("me/anno/ui/debug/FrameTimings"))
        excludeFiles(sources, "me/anno/ui/anim")
        excludeFiles(sources, "me/anno/ui/custom", listOf("me/anno/ui/custom/CustomPanelType", "me/anno/ui/custom/UITypeLibrary"))
        excludeFiles(sources, "me/anno/ui/base/image")
        excludeFiles(sources, "me/anno/ui/base/buttons")
        excludeFiles(sources, "textures") // I'm not too sure about this...
        excludeFiles(sources, "assets/org/apache/commons") // what is this used for???
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
            srcFile.startsWith(path) && except.none { srcFile.startsWith(it) }
        }
    }
}