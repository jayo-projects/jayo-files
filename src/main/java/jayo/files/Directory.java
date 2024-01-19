/*
 * Copyright (c) 2024-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.files;

import jayo.files.internal.RealDirectory;

/**
 * A Jayo's Directory is guaranteed to be a real existing directory.
 */
public sealed interface Directory permits RealDirectory {
}
