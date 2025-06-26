package me.anno.utils.files

import me.anno.io.files.FileReference
import me.anno.utils.OS

object LinkCreator {
    fun createLink(original: FileReference, linkLocation: FileReference) {
        val data = if (OS.isWindows) {
            // create .url file
            // are they supported for static files, too???
            if (original.absolutePath.contains("://")) {
                "[InternetShortcut]\n" +
                        "URL=$original\n"
            } else {
                "[InternetShortcut]\n" +
                        "URL=file://$original\n"
            }
        } else {
            // create .desktop file
            // sample data by https://help.ubuntu.com/community/UnityLaunchersAndDesktopFiles:
            "[Desktop Entry]\n" +
                    "Version=1.0\n" +
                    "Name=${original.name}\n" +
                    "Exec=${original.absolutePath}\n" +
                    "Icon=${original.absolutePath}\n" +
                    "Type=Link\n"
        }
        linkLocation.writeText(data)
    }
}