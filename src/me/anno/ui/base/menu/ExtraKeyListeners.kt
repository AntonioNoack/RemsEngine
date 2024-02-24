package me.anno.ui.base.menu

import me.anno.maths.Maths.hasFlag

/**
 * utility class for defining extra-keys for menus:
 * distributed to the first actions by their name
 * */
class ExtraKeyListeners {

    private var usedLetters = 0
    val listeners = HashMap<Char, () -> Boolean>()

    fun findNextFreeIndex(name: String): Int {
        for (i in name.indices) {
            val letter = name[i]
            val lcLetter = letter.lowercaseChar()
            if (lcLetter in 'a'..'z') {
                val mask = 1 shl (lcLetter.code - 'a'.code)
                if (!usedLetters.hasFlag(mask)) {
                    usedLetters = usedLetters or mask
                    return i
                }
            }
        }
        return -1
    }

    fun bind(char: Char, action: () -> Boolean) {
        listeners[char] = action
    }

    fun execute(char: Char): Boolean? {
        return listeners[char]?.invoke()
    }
}