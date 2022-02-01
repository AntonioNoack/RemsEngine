package me.anno.graph.knn;

import me.anno.utils.structures.lists.Lists;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Heap {

    // https://www.geeksforgeeks.org/building-heap-from-array/

    // To heapify a subtree rooted with node i which is
    // an index in arr[].Nn is size of heap
    static <V> void maxHeapify(ArrayList<V> arr, int i, Comparator<V> comparator) {
        int largest = i; // Initialize largest as root
        int l = 2 * i + 1; // left = 2*i + 1
        int r = 2 * i + 2; // right = 2*i + 2

        int n = arr.size();
        // If left child is larger than root
        if (l < n && comparator.compare(arr.get(l), arr.get(largest)) > 0)
            largest = l;

        // If right child is larger than largest so far
        if (r < n && comparator.compare(arr.get(r), arr.get(largest)) > 0)
            largest = r;

        // If largest is not root
        if (largest != i) {
            V swap = arr.get(i);
            arr.set(i, arr.get(largest));
            arr.set(largest, swap);

            // Recursively heapify the affected sub-tree
            maxHeapify(arr, largest, comparator);
        }
    }

    // To heapify a subtree rooted with node i which is
    // an index in arr[].Nn is size of heap
    static <V> void minHeapify(ArrayList<V> arr, int i, Comparator<V> comparator) {
        int largest = i; // Initialize largest as root
        int l = 2 * i + 1; // left = 2*i + 1
        int r = 2 * i + 2; // right = 2*i + 2

        int n = arr.size();
        // If left child is larger than root
        if (l < n && comparator.compare(arr.get(l), arr.get(largest)) < 0)
            largest = l;

        // If right child is larger than largest so far
        if (r < n && comparator.compare(arr.get(r), arr.get(largest)) < 0)
            largest = r;

        // If largest is not root
        if (largest != i) {
            V swap = arr.get(i);
            arr.set(i, arr.get(largest));
            arr.set(largest, swap);

            // Recursively heapify the affected sub-tree
            minHeapify(arr, largest, comparator);
        }
    }

    public static <V> V extractMax(ArrayList<V> arr, Comparator<V> comparator) {
        V value = arr.get(0);
        if (arr.size() > 1) {
            arr.set(0, arr.remove(arr.size() - 1));
            maxHeapify(arr, 0, comparator);
        } else {
            arr.clear();
        }
        return value;
    }

    public static <V> V extractMin(ArrayList<V> arr, Comparator<V> comparator) {
        V value = arr.get(0);
        if (arr.size() > 1) {
            arr.set(0, arr.remove(arr.size() - 1));
            minHeapify(arr, 0, comparator);
        } else {
            arr.clear();
        }
        return value;
    }

    // Function to build a Max-Heap from the Array
    public static <V> void buildMaxHeap(ArrayList<V> arr, Comparator<V> comparator) {
        // Index of last non-leaf node
        int startIdx = (arr.size() / 2) - 1;

        // Perform reverse level order traversal
        // from last non-leaf node and heapify
        // each node
        for (int i = startIdx; i >= 0; i--) {
            maxHeapify(arr, i, comparator);
        }
    }

    // Function to build a Min-Heap from the Array
    public static <V> void buildMinHeap(ArrayList<V> arr, Comparator<V> comparator) {
        // Index of last non-leaf node
        int startIdx = (arr.size() / 2) - 1;

        // Perform reverse level order traversal
        // from last non-leaf node and heapify
        // each node
        for (int i = startIdx; i >= 0; i--) {
            minHeapify(arr, i, comparator);
        }
    }

    // A utility function to print the array
    // representation of Heap
    static <V> void printHeap(List<V> arr) {
        System.out.println("Array representation of Heap is:");
        System.out.println(arr);
    }

    static <V> void printSortedMax(ArrayList<V> arr, Comparator<V> comparator) {
        System.out.println("Sorted elements are:");
        for (int i = 0, l = arr.size(); i < l; i++) {
            V value = extractMax(arr, comparator);
            System.out.print(value + " ");
        }
        System.out.println();
    }

    static <V> void printSortedMin(ArrayList<V> arr, Comparator<V> comparator) {
        System.out.println("Sorted elements are:");
        for (int i = 0, l = arr.size(); i < l; i++) {
            V value = extractMin(arr, comparator);
            System.out.print(value + " ");
        }
        System.out.println();
    }

    // Driver Code
    public static void main(String[] args) {
        // Binary Tree Representation
        // of input array
        //              1
        //           /     \
        //         3         5
        //      /    \     /  \
        //     4      6   13  10
        //    / \    / \
        //   9   8  15 17
        int[] arr0 = {1, 3, 5, 4, 6, 13, 10, 9, 8, 15, 17};

        ArrayList<Integer> arr = new ArrayList<>();
        for (int value : arr0) arr.add(value);
        System.out.println(Lists.INSTANCE.smallestKElements(arr, 10, Integer::compareTo));

        buildMaxHeap(arr, Integer::compareTo);
        printHeap(arr);
        printSortedMax(arr, Integer::compareTo);

    }
}