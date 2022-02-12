package me.anno.utils.structures.arrays

/**
 * Represents a readable sequence of CodePoint values.
 */
interface IntSequence {

    /**
     * Returns the length of this character sequence.
     */
    val length: Int

    /**
     * Returns the character at the specified [index] in this character sequence.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this character sequence.
     */
    operator fun get(index: Int): Int

    /**
     * Returns a new character sequence that is a subsequence of this character sequence,
     * starting at the specified [startIndex] and ending right before the specified [endIndex].
     *
     * @param startIndex the start index (inclusive).
     * @param endIndex the end index (exclusive).
     */
    fun subSequence(startIndex: Int, endIndex: Int): IntSequence

    fun toDirtyCharSequence(startIndex: Int, endIndex: Int): DirtyCharSequence {
        return DirtyCharSequence(this, startIndex, endIndex)
    }

    val indices get() = 0 until length

}