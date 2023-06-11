package me.anno.tests.utils

import me.anno.cache.instances.PDFCache
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.OS.desktop
import org.apache.logging.log4j.LogManager
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

fun main() {

    // to do create master thesis pdf from markdown like file
    // to do what we need:
    //  - text
    //  - images
    //  - boxes: images and text in one, non-splittable
    //  - references
    //  - sections
    //  - numbers

    PDFCache.disableLoggers()
    HiddenOpenGLContext.createOpenGL()

    val doc = PDDocument()

    val page = PDPage()
    doc.addPage(page)

    val text = pbrModelShader.value.fragmentSource

    val w = page.bBox.width
    val h = page.bBox.height

    val pxl = 10f
    val pxr = 10f
    val pyt = 30f
    val pyb = 30f

    val font = PDType1Font.HELVETICA
    val stream = PDPageContentStream(doc, page)
    stream.beginText()
    stream.setFont(font, 12f)
    stream.setNonStrokingColor(255, 0, 0)
    stream.moveTextPositionByAmount(pxl, h - pyt)
    for (line in text.split('\n')) {
        stream.moveTextPositionByAmount(0f, -12f)
        stream.drawString(line)
    }
    stream.endText()
    stream.drawImage(
        PDImageXObject.createFromFile(desktop.getChild("checker.png").absolutePath, doc),
        10f, h - 100f - 10f, 100f, 100f
    )
    stream.drawLine(10f, 10f, 100f, 100f)
    stream.close()

    val section = ArrayList<Int>()

    fun formatSection(): String =
        section.joinToString(".")

    fun nextLine() {}

    fun write(
        text: String, size: Float,
        alignment: AxisAlignment,
        indent: Float = 0f,
        color: Int = -1
    ) {
        if ('\n' in text) {
            for (line in text.split('\n')) {
                write(line, size, alignment)
                nextLine()
            }
        } else {
            // todo measure line length
            // todo split line if too long
            stream.beginText()
            stream.setFont(font, size)
            stream.setNonStrokingColor(color.r(), color.g(), color.b())
            stream.moveTextPositionByAmount(pxl, h - pyt)
            stream.drawString(text)
            stream.endText()
        }
    }

    fun nextPage() {
        val fs = 12f
        // todo chapter number
        stream.drawLine(pxl, h - (pyt + fs), w - pxr, h - (pyt + fs))
        stream.drawLine(pxl, (pyb + fs), w - pxr, (pyb + fs))
        // todo page number
    }

    fun nextChapter(title: String) {
        val lastSection = section.firstOrNull() ?: 0
        section.clear()
        section.add(lastSection + 1)
        nextPage()
        write(title + "\n", 18f, AxisAlignment.MIN, 0f)
    }

    fun nextSection(title: String) {}

    fun nextSubsection(title: String) {}

    // todo parse documents
    val home = File(System.getProperty("user.home"))
    val folder = File(home, "Documents/IdeaProjects/Thesis/thesis")
    val children = folder.listFiles()!!
        .filter { it.name.endsWith(".tex") && it.name.split("-").first().toIntOrNull() != null }
        .sortedBy { it.name.split("-").first().toInt() }
    val data = children.joinToString("") { it.readText() }
    val lines = data.split('\n')
        .filter { !it.trim().startsWith("\\usepackage{") }
    val allLines = lines.joinToString("\n")

    for (sectionText in allLines.split("\n\n")) {
        // todo replace all macros
        val trim = sectionText.trim()
        when {
            trim.startsWith("\\chapter") -> nextChapter(sectionText)
            trim.startsWith("\\section") -> nextSection(sectionText)
            trim.startsWith("\\subsection") -> nextSubsection(sectionText)
            else -> {
                write(sectionText, 12f, AxisAlignment.FILL, 0f, -1)
            }
        }
    }


    doc.save(desktop.getChild("thesis.pdf").absolutePath)
    doc.close()

}