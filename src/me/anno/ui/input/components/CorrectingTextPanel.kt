package me.anno.ui.input.components

import me.anno.gpu.Cursor
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.language.spellcheck.Spellchecking
import me.anno.language.spellcheck.Suggestion
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.code.CodeEditor.Companion.drawSquiggles1
import me.anno.ui.Style
import me.anno.utils.Color.black
import me.anno.utils.types.Strings.joinChars
import kotlin.streams.toList

abstract class CorrectingTextPanel(style: Style) : TextPanel("", style) {

    var drawingOffset = 0
    var allowFirstLowercase = true

    override fun getVisualState(): Any? = suggestions

    var enableSpellcheck = true

    private val suggestions
        get() =
            if (enableSpellcheck && !isShowingPlaceholder) {
                Spellchecking.check(text, allowFirstLowercase)
            } else null

    private var loadTextSync = false
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        loadTexturesSync.push(loadTextSync)
        instantTextLoading = true
        super.onDraw(x0, y0, x1, y1)
        drawSuggestionLines()
        loadTexturesSync.pop()
    }

    fun drawSuggestionLines() {
        val suggestions = suggestions
        if (!suggestions.isNullOrEmpty()) {
            // display all suggestions
            for (si in suggestions.indices) {
                val x0 = x + padding.left + drawingOffset
                val s = suggestions[si]
                val startX = x0 + getXOffset(s.start)
                val endX = x0 + getXOffset(s.end)
                val theY = this.y + this.height - padding.bottom - 1
                // wavy line
                val color = 0xffff00 or black
                drawSquiggles1(startX, endX, theY, 3, color)
                // DrawRectangles.drawRect(startX, theY, endX - startX, 1, color)
            }
            val window = window
            if (isHovered && !isInFocus &&
                window == window?.windowStack?.peek()
            ) {
                requestFocus()
                // setCursor(text.length)
            }
        }
        if (!isHovered) lastSuggestion = null
    }

    // todo better tooltip element to list all options
    // todo on tab or keys, open a menu with the options
    private var lastSuggestion: Suggestion? = null
    override fun getTooltipText(x: Float, y: Float): String? {
        val suggestions = suggestions
        if (suggestions != null) {
            for (s in suggestions) {
                val x0 = this.x + padding.left + drawingOffset
                val startX = x0 + getXOffset(s.start)
                val endX = x0 + getXOffset(s.end)
                if (x.toInt() in startX..endX) {
                    lastSuggestion = s
                    return if (s.improvements.isEmpty()) s.clearMessage else s.clearMessage + "\n" +
                            "Suggestions: " + s.improvements.withIndex().joinToString { (index, s) ->
                        if (index == 0) "$s <Tab>" else s
                    }
                }
            }
        }
        lastSuggestion = null
        return null
    }

    abstract fun onCharTyped2(x: Float, y: Float, key: Int)
    abstract fun onEnterKey2(x: Float, y: Float)

    fun tryApplySuggestion(): Boolean {
        val suggestion = lastSuggestion
        return if (suggestion != null && suggestion.improvements.isNotEmpty()) {
            if (suggestion.improvements.size > 1) {
                Menu.openMenu(windowStack,
                    suggestion.improvements.map {
                        MenuOption(it, "") {
                            applySuggestion(suggestion, it)
                        }
                    }
                )
            } else {
                applySuggestion(suggestion, suggestion.improvements[0])
            }
            true
        } else false
    }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        if (!(codepoint == '\t'.code && tryApplySuggestion())) {
            onCharTyped2(x, y, codepoint)
        }
    }

    override fun onEnterKey(x: Float, y: Float) {
        if (!tryApplySuggestion()) {
            onEnterKey2(x, y)
        }
    }

    // todo find synonyms by clicking on stuff, I think this library can do that
    // todo automatically show hints, when the user is typing
    private fun applySuggestion(suggestion: Suggestion, choice: String) {
        val text = text
        val bytes = text.codePoints().toList()
        val start = if (suggestion.start == 0) emptyList() else bytes.subList(0, suggestion.start)
        val end = if (suggestion.end >= bytes.size) emptyList() else bytes.subList(suggestion.end, bytes.size - suggestion.end)
        this.text = (start + choice.codePoints().toList() + end).joinChars().toString()
        updateChars(true)
        setCursor(start.size + choice.codePoints().toList().size) // set the cursor to after the edit
    }

    abstract fun updateChars(notify: Boolean)
    abstract fun setCursor(position: Int)

    override var enableHoverColor: Boolean
        get() = text.isNotEmpty()
        set(_) {}

    override fun getCursor() = Cursor.editText
    override fun isKeyInput() = true

    override fun acceptsChar(char: Int): Boolean = true

    abstract val isShowingPlaceholder: Boolean

}