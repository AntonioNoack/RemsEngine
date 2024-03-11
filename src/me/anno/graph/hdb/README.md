# Hierarchical Database

Hierarchical databases are usually represented by using the file system.
However, when having many small files, that has a quite big overhead. Some operating systems like
Windows, and some drives like HDDs and USB sticks have issues with a high number of files (because of random access), too.

This package implements a similar interface, but within a few files.
Folders are loaded as one, so requesting siblings is usually lower cost than requesting an unrelated file.