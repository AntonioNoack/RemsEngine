package me.anno.ui.input.components

import me.anno.config.DefaultStyle
import me.anno.gpu.Cursor
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.language.spellcheck.Spellchecking
import me.anno.language.spellcheck.Suggestion
import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style
import kotlin.math.min

abstract class CorrectingTextInput(style: Style) : TextPanel("", style) {

    init {
        instantTextLoading = true
    }

    var drawingOffset = 0
    var allowFirstLowercase = true

    override fun getVisualState(): Any? = suggestions

    open val enableSpellcheck = true
    private val suggestions get() =
        if (enableSpellcheck && !isShowingPlaceholder){
            Spellchecking.check(text, allowFirstLowercase)
        } else null

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        loadTexturesSync.push(true)
        super.onDraw(x0, y0, x1, y1)
        drawSuggestionLines()
        loadTexturesSync.pop()
    }

    fun drawSuggestionLines(){
        val suggestions = suggestions
        if (suggestions != null && suggestions.isNotEmpty()) {
            // display all suggestions
            suggestions.forEach { s ->
                // todo wavy line
                val startX = getX(s.start)
                val endX = getX(s.end)
                val theY = this.y + this.h - padding.bottom - 1
                DrawRectangles.drawRect(startX, theY, endX - startX, 1, 0xffff00 or DefaultStyle.black)
            }
            if(isHovered && !isInFocus){
                requestFocus()
                // setCursor(text.length)
            }
        }
        if (!isHovered) lastSuggestion = null
    }

    // todo better tool tip element to list all options
    // todo on tab or keys, open a menu with the options
    private var lastSuggestion: Suggestion? = null
    override fun getTooltipText(x: Float, y: Float): String? {
        val suggestions = suggestions
        if (suggestions != null) {
            for (s in suggestions) {
                val startX = getX(s.start)
                val endX = getX(s.end)
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
        return super.getTooltipText(x, y)
    }

    abstract fun onCharTyped2(x: Float, y: Float, key: Int)
    abstract fun onEnterKey2(x: Float, y: Float)

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        val suggestion = lastSuggestion
        if (key == '\t'.code && suggestion?.improvements?.isNotEmpty() == true) {
            applySuggestion(suggestion, suggestion.improvements[0])
        } else {
            onCharTyped2(x, y, key)
        }
    }

    override fun onEnterKey(x: Float, y: Float) {
        val suggestion = lastSuggestion
        if (suggestion != null && suggestion.improvements.isNotEmpty()) {
            applySuggestion(suggestion, suggestion.improvements[0])
        } else {
            onEnterKey2(x, y)
        }
    }

    private fun getX(charIndex: Int) = x + padding.left + drawingOffset + if (charIndex <= 0) -1 else
        getTextSizeX(font, text.substring(0, min(charIndex, text.length)), -1, -1) - 1

    // todo find synonyms by clicking on stuff, I think this library can do that
    // todo automatically show hints, when the user is typing
    private fun applySuggestion(suggestion: Suggestion, choice: String) {
        // todo the indexing still isn't completely correct... (example: Eine Löwenfuß -> Ein Löwenfußß)
        val text = text
        val bytes = text.toByteArray()
        val begin = if (suggestion.start == 0) "" else String(bytes, 0, suggestion.start)
        val end = if (suggestion.end >= bytes.size) "" else String(bytes, suggestion.end, bytes.size - suggestion.end)
        this.text = begin + choice + end
        updateChars(true)
        setCursor((begin + choice).codePoints().count().toInt()) // set the cursor to after the edit
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