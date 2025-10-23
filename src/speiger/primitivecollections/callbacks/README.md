# Callback Interfaces

While Kotlin allows for (Int)->Int - types, they always use boxing, and we want to avoid unnecessary memory allocations.
To avoid the boxing, we define function interfaces for every single case.

Function-interfaces no longer allow inlining (*pain* -> I truly need my own Kotlin compiler one day), but at least they can be reused.