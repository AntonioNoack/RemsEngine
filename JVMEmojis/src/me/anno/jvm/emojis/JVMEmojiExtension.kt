package me.anno.jvm.emojis

import me.anno.extensions.plugins.Plugin
import me.anno.fonts.IEmojiCache

class JVMEmojiExtension : Plugin() {
    override fun onEnable() {
        IEmojiCache.emojiCache = EmojiCache
    }
}