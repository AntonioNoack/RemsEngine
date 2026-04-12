# EXR files

are files that support float data, like HDRs.
They support many compressions, with some being as complex as JPEG:

- RLE
- ZIPS, ZIP -> Zip/Inflater
- PIZ -> wavelet +  huffman
- PXR24 -> predictor + zlib
- B44, B44A -> lossy block codec
- DWA-A, DWA-B -> complex JPEG-style compression

Therefore, we really should use an external library.
TinyEXR doesn't support DWA, or at least my version doesn't,
and PolyHaven has some DWA files.