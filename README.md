[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://www.java.com/en/download/help/whatis_java.html)

# jayo-files

A `java.io.File` can indeed be a file. \
But... it can also be a directory; weird. \
Furthermore, it can also refer to a file or a directory that does not exist; now it gets even weirder ! \
A `java.nio.file.Path` is conceptually exactly the same as a `java.io.File`, but it does have a better naming.

Jayo tries to make things clearer. To do that, this module only provides :
* `File`, that is guaranteed to be a real existing file.
* `Directory`, that is guaranteed to be a real existing directory.

Operations on files and directories are not the same, it is necessary to distinguish between them with two distinct
types.
