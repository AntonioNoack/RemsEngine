package me.anno.objects.text

import me.anno.input.Input.setClipboardContent
import me.anno.language.translation.NameDesc
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.roundToInt

class Chapter(parent: Transform?) : GFXTransform(parent) {

    // todo show their title on the timeline (?)

    constructor() : this(null)

    override fun getStartTime(): Double = 0.0
    override fun getEndTime(): Double = 1.0

    fun getChapterTime(): Double {
        // calculate which time results in zero of this element
        return listOfInheritance.sumOf { it.timeOffset.value }
    }

    fun formatInt(int: Int): String {
        return if (int < 10) return "0$int"
        else "$int"
    }

    fun createTimestamp(seconds: Int): String {
        if (seconds < 0) return "-" + createTimestamp(-seconds)
        val s = seconds % 60
        val mx = seconds / 60
        val m = mx % 60
        val h = mx / 60
        return if (h == 0) {
            "${formatInt(m)}:${formatInt(s)}"
        } else {
            "${formatInt(h)}:${formatInt(m)}:${formatInt(s)}"
        }
    }

    fun createChapterSummary(): List<String> {
        val chapters = root.listOfAll.filterIsInstance<Chapter>().map { it.name to it.getChapterTime() }
        val sortedChapters = chapters.sortedBy { it.second }
        // first chapter must start at zero
        // (YouTube's guidelines)
        val result = ArrayList<String>()
        val minimumChapterLengthSeconds = 10
        var lastTime = -minimumChapterLengthSeconds
        for ((name, time0) in sortedChapters) {
            val time = max(lastTime, time0.roundToInt())
            if (time >= lastTime + minimumChapterLengthSeconds) {
                lastTime = time
                // print the chapter
                val value = createTimestamp(time) + " " + name.trim()
                result.add(value)
            }
        }
        return result
    }

    fun getNiceText(): String {
        return "Chapters:\n" + createChapterSummary().joinToString("\n")
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        list += UpdatingTextPanel(500, style) { "Start time: ${createTimestamp(getChapterTime().roundToInt())}" }
        list += UpdatingTextPanel(500, style) { getNiceText() }
            .addRightClickListener {
                openMenu(listOf(MenuOption(NameDesc("Copy to clipboard")) {
                    setClipboardContent(getNiceText())
                }))
            }
    }

    override val className: String = "Chapter"

}