
# Coding Style

When programming in Rem's Engine, there's two main considerations that may be atypical in other Java/Kotlin projects:
- avoid allocations
- avoid throwing exceptions
- never wait on the main thread

## Allocations

More allocations raise memory pressure, because the engine is using garbage collection. See keeping the total number
of objects low is improving performance on the devices that need it most.

For calculations using vectors, quaternions and matrices, use JomlPool to temporarily create them, and then free them afterward.

LinkedLists should be avoided in 99.9% of cases. When iterating over lists, iterate over their indices:
this prevents eventual ConcurrentModificationExceptions, and saves the iterator-allocation.

## Exceptions

Throwing exceptions and catching them later is pretty expensive:
- the stack needs to be collected at all times during runtime (if you use the stack)
- on some targets, the return type becomes more complicated (pointer to thrown), throwables need to be handled for every
  single function call, and the flow-complexity in functions with catch-blocks increases (-> may need not-natively supported jumps; makes reasoning/optimization harder)

## Main-Thread stalling

Waiting on the main-thread is immediately noticeable by the user.
Instead, use callbacks or coroutines.

Coroutines haven't been used in the engine yet, because their binary size was a few MB. A few MB can be some seconds
that the user needs to wait when loading a game in their browser.