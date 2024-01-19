/*
 * Copyright (c) 2024-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("-File") // A leading '-' hides this class from Java.

package jayo.files

import java.net.URI
import java.nio.file.Path

/**
 * @return a `FileBuilder` that will allow to create or open the file this [Path] targets.
 */
public fun Path.buildFile(): File.FileBuilder = File.Builder().of(this)

/**
 * @return a `FileBuilder` that will allow to create or open the file this [java.io.File] targets.
 */
public fun java.io.File.buildFile(): File.FileBuilder = File.Builder().of(this)

/**
 * @return a `FileBuilder` that will allow to create or open the file this [URI] targets.
 */
public fun URI.buildFile(): File.FileBuilder = File.Builder().of(this)
