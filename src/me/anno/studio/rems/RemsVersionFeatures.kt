package me.anno.studio.rems

import me.anno.config.VersionFeatures
import me.anno.io.utils.StringMap
import me.anno.objects.FourierTransform
import me.anno.objects.Transform
import me.anno.objects.documents.pdf.PDFDocument
import me.anno.objects.geometric.LinePolygon
import me.anno.objects.text.Chapter
import me.anno.studio.StudioBase

class RemsVersionFeatures(oldVersion: Int) : VersionFeatures(oldVersion) {

    constructor(config: StringMap) : this(config["version", -1])

    fun addInstance(config: StringMap, name: String, value: Transform) {
        val list = config["createNewInstancesList"] as? StringMap ?: return
        list[name, value]
    }

    fun removeInstance(config: StringMap, name: String) {
        val list = config["createNewInstancesList"] as? StringMap ?: return
        list.remove(name)
    }

    override fun addNewPackages(config: StringMap) {

        // when new stuff is added, it can be forced upon the user
        // DefaultConfig["createNewInstancesList"].removeAll { it is NewType }
        // DefaultConfig["createNewInstancesList"].add("newName" to NewType())

        addVersion(10002) {
            addInstance(config, "PDF Document", PDFDocument())
        }

        addVersion(10102) {
            addInstance(config, "Line", LinePolygon())
        }

        addVersion(10104) {
            addInstance(config, "Fourier Transform", FourierTransform())
        }

        addVersion(10106) {
            addInstance(config, "Chapter", Chapter())
        }

        config["version"] = StudioBase.instance?.versionNumber ?: 0

    }

}