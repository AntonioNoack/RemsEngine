package me.anno.ui.base.menu

/**
 * utility class for defining extra-keys for menus:
 * distributed to the first actions by their name
 * */
class ExtraKeyListeners {

    private var usedLetters = 0
    val listeners = HashMap<Char, () -> Boolean>()

    fun findNextFreeIndex(name: String): Int {
        for (i in name.indices) {
            val mask = getMask(name[i])
            if (mask != 0) {
                usedLetters = usedLetters or mask
                return i
            }
        }
        return -1
    }

    fun getMask(letter: Char): Int {
        val lcLetter = letter.lowercaseChar()
        return if (lcLetter in 'a'..'z') {
            1 shl (lcLetter.code - 'a'.code)
        } else 0
    }

    fun remove(char: Char) {
        listeners.remove(char)
        usedLetters = usedLetters.and(getMask(char).inv())
    }

    fun bind(char: Char, action: () -> Boolean) {
        listeners[char] = action
    }

    fun execute(char: Char): Boolean? {
        return listeners[char]?.invoke()
    }
}