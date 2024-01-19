/*
 * Copyright (c) 2024-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.files.internal;

import jayo.files.Directory;
import org.jspecify.annotations.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class RealDirectory implements Directory {
    private final @NonNull Path path;

    public RealDirectory(final @NonNull Path path) {
        Objects.requireNonNull(path);
        // todo move in Directory interface
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Jayo's directory must be a directory");
        }
        this.path = Objects.requireNonNull(path);
    }
}
