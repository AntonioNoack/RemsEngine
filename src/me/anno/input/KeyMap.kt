package me.anno.input

/**
 * records, which key combinations were used to create which keys;
 * this makes that we can print Umlauts and special symbols correctly
 * */
object KeyMap {

    data class InputState(
        val control: Boolean,
        val shift: Boolean,
        val alt: Boolean,
        val superKey: Boolean,
        val keys: HashSet<Int>
    ) {
        constructor() : this(
            Input.isControlDown,
            Input.isShiftDown,
            Input.isAltDown,
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
            if (state !in inputMap) {
                stateId++
                inputMap[state] = codepoint
            }
        }
    }

}