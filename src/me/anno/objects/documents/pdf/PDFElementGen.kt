package me.anno.objects.documents.pdf

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.objects.documents.SiteSelection.parseSites
import me.anno.objects.lists.ElementGenerator
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.files.LocalFile.toGlobalFile
import org.apache.logging.log4j.LogManager
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import kotlin.math.max
import kotlin.math.min

open class PDFElementGen(var file: File) : ElementGenerator() {

    var selectedSites = ""

    var padding = AnimatedProperty.float()

    var direction = AnimatedProperty.rotY()

    var editorQuality = 3f
    var renderQuality = 3f

    override fun getDefaultDisplayName(): String {
        return if (file == null || file.name.isBlank()) "PDF"
        else file.name
    }

    override fun getClassName(): String = "PDFGenerator"
    override fun getSymbol(): String = ""

    fun getSelectedSitesList() = parseSites(selectedSites)

    val meta get() = getMeta(file, true)
    val forcedMeta get() = getMeta(file, false)!!

    fun getMeta(src: File, async: Boolean): PDDocument? {
        return PDFCache.getDocument(src, async)
    }

    fun getPage(index: Int): PDFDocument {
        val doc = PDFDocument()
        doc.file = file
        doc.selectedSites = (index + 1).toString()
        doc.renderQuality = renderQuality
        doc.editorQuality = editorQuality
        doc.direction = direction
        doc.padding = padding
        return doc
    }

    override fun generateEntry(index: Int): Transform? {
        val meta = forcedMeta
        val numberOfPages = meta.numberOfPages
        val pages = getSelectedSitesList()
        var index0 = 0
        for (range in pages) {
            val min = max(0, range.first)
            val max = min(numberOfPages - 1, range.last)
            val size = max - min + 1
            if (size > 0) {
                if (index - index0 in 0 until size) {
                    // found it :)
                    val pageNumber = index - index0 + min
                    return getPage(pageNumber)
                }
                index0 += size
            }
        }
        return null
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val doc = getGroup("Document", "", "docs")
        doc += vi("Path", "", null, file, style) { file = it }
        doc += vi("Pages", "", null, selectedSites, style) { selectedSites = it }
        doc += vi("Padding", "", padding, style)
        doc += vi("Direction", "Top-Bottom/Left-Right in Degrees", direction, style)
        doc += vi("Editor Quality", "", Type.FLOAT_PLUS, editorQuality, style) { editorQuality = it }
        doc += vi("Render Quality", "", Type.FLOAT_PLUS, renderQuality, style) { renderQuality = it }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
        writer.writeObject(this, "padding", padding)
        writer.writeString("selectedSites", selectedSites)
        writer.writeObject(this, "direction", direction)
        writer.writeFloat("editorQuality", editorQuality)
        writer.writeFloat("renderQuality", renderQuality)
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "editorQuality" -> editorQuality = value
            "renderQuality" -> renderQuality = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "file" -> file = value.toGlobalFile()
            "selectedSites" -> selectedSites = value
            else -> super.readString(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "padding" -> padding.copyFrom(value)
            "direction" -> direction.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(PDFElementGen::class)
        val timeout = 20_000L
    }

}