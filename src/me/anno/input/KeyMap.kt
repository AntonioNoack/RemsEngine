package me.anno.input

object KeyMap {

    // todo the input mapping could be used to detect y-z switched keyboards & such
    data class InputMapping(
        val codepoint: Int,
        val control: Boolean,
        val shift: Boolean,
        val alt: Boolean,
        val superKey: Boolean,
        val keys: HashSet<Int>
    )

    val inputMap = HashSet<InputMapping>()
    var maxKeys = 3

    fun onCharTyped(codepoint: Int) {
        if (Input.keysDown.size <= maxKeys) {
            inputMap.add(
                InputMapping(
                    codepoint,
                    Input.isControlDown,
                    Input.isShiftDown,
                    Input.isAltDown,
                    Input.isSuperDown,
                    Input.keysDown.keys.toHashSet()
                )
            )
        }
    }

}