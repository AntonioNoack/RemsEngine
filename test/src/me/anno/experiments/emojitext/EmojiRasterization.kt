package me.anno.experiments.emojitext

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Codepoints
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.IEmojiCache
import me.anno.jvm.emojis.EmojiCache
import me.anno.jvm.emojis.EmojiCache.getEmojiImage
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue

fun main() {
    OfficialExtensions.initForTests()

    assertEquals(EmojiCache, IEmojiCache.emojiCache)

    val text = "\uD83D\uDCC1"
    val codepoints = text.codepoints()
    val emojiId = Codepoints.getEmojiId(codepoints[0])
    assertTrue(emojiId >= 0, "$text is not a valid emoji")
    getEmojiImage(emojiId, 64).waitFor { image ->
        image!!.write(desktop.getChild("Emoji.png"))
    }
}