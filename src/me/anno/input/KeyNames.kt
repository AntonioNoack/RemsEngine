package me.anno.input

/**
 * records, which key combinations were used to create what keys;
 * this makes that we can print Umlauts and special symbols correctly
 * */
object KeyNames {

    data class InputState(
        val control: Boolean,
        val shift: Boolean,
        val alt: Boolean,
        val superKey: Boolean,
        val keys: HashSet<Key>
    ) {
        constructor() : this(
            Input.isControlDown,
            Input.isShiftDown,
            Input.isKeyDown(Key.KEY_RIGHT_ALT),
            Input.isSuperDown,
            Input.keysDown.keys.toHashSet()
        )
    }

    @JvmField
    val inputMap = HashMap<InputState, Int>()

    @JvmField
    var maxKeys = 3

    @JvmField
    var stateId = 0

    @JvmStatic
    fun onCharTyped(codepoint: Int) {
        if (Input.keysDown.size <= maxKeys) {
            val state = InputState()
            inputMap.getOrPut(state) {
                stateId++
                codepoint
            }
        }
    }
}