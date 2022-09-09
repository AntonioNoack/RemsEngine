package me.anno.maths

@Suppress("unused")
object Permutations {

    fun <T> generatePermutations(elements: List<T>, onNextPermutation: (List<T>) -> Unit) {
        generatePermutations2(elements.toMutableList(), onNextPermutation)
    }

    fun <T> generatePermutations2(elements: MutableList<T>, onNextPermutation: (List<T>) -> Unit) {

        // Wikipedia https://en.wikipedia.org/wiki/Heap%27s_algorithm
        // c is an encoding of the stack state. c[k] encodes the for-loop counter for when generate(k+1, A) is called

        val size = elements.size
        val stack = IntArray(size)
        onNextPermutation(elements)

        var stackPointer = 0
        while (stackPointer < size) {
            if (stack[stackPointer] < stackPointer) {
                val i = if (stackPointer and 1 == 0) 0 else stack[stackPointer]
                val j = stackPointer
                val tmp = elements[i]
                elements[i] = elements[j]
                elements[j] = tmp
                onNextPermutation(elements)
                // Swap has occurred ending the for-loop.
                // Simulate the increment of the for-loop counter
                stack[stackPointer]++
                // Simulate recursive call reaching the base case by bringing the pointer
                // to the base case analog in the array
                stackPointer = 0
            } else {
                // Calling generate(i+1, A) has ended as the for-loop terminated.
                // Reset the state and simulate popping the stack by incrementing the pointer.
                stack[stackPointer++] = 0
            }
        }

    }

}