package me.anno.jvm.emojis

import me.anno.extensions.plugins.Plugin
import me.anno.fonts.LineSplitter

class JVMEmojiExtension : Plugin() {
    override fun onEnable() {
        LineSplitter.emojiCache = EmojiCache
    }
}