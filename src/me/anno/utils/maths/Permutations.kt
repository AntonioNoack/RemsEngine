package me.anno.utils.maths

object Permutations {

    fun <T> generatePermutations(elements: List<T>, onNextPermutation: (List<T>) -> Unit) {
        generatePermutations2(elements.toMutableList(), onNextPermutation)
    }

    fun <T> generatePermutations2(elements: MutableList<T>, onNextPermutation: (List<T>) -> Unit) {

        // Wikipedia https://en.wikipedia.org/wiki/Heap%27s_algorithm
        // c is an encoding of the stack state. c[k] encodes the for-loop counter for when generate(k+1, A) is called

        val n = elements.size
        val c = IntArray(n)

        onNextPermutation(elements)

        fun swap(i: Int, j: Int) {
            val t = elements[i]
            elements[i] = elements[j]
            elements[j] = t
        }

        // i acts similarly to the stack pointer
        var i = 0
        while (i < n) {
            if (c[i] < i) {
                if (i % 2 == 0) {// even
                    swap(0, i)
                } else {
                    swap(c[i], i)
                }
                onNextPermutation(elements)
                // Swap has occurred ending the for-loop.
                // Simulate the increment of the for-loop counter
                c[i]++
                // Simulate recursive call reaching the base case by bringing the pointer
                // to the base case analog in the array
                i = 0
            } else {
                // Calling generate(i+1, A) has ended as the for-loop terminated.
                // Reset the state and simulate popping the stack by incrementing the pointer.
                c[i++] = 0
            }
        }

    }

}