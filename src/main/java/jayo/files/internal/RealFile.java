/*
 * Copyright (c) 2024-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.files.internal;

import jayo.ByteString;
import jayo.Jayo;
import jayo.RawSink;
import jayo.RawSource;
import jayo.crypto.Digest;
import jayo.crypto.Hmac;
import jayo.exceptions.JayoException;
import jayo.exceptions.JayoFileNotFoundException;
import jayo.external.NonNegative;
import jayo.files.File;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.System.Logger.Level.DEBUG;

public final class RealFile implements File {
    private static final System.Logger LOGGER = System.getLogger("jayo.files.File");

    private final @NonNull Path path;

    public RealFile(final @NonNull Path path) {
        this.path = Objects.requireNonNull(path);
    }

    @Override
    public @NonNull RawSink sink(final @NonNull OpenOption @NonNull ... options) {
        if (!Files.exists(path)) {
            throw new JayoFileNotFoundException("file does not exist anymore");
        }
        final Set<OpenOption> optionsSet = new HashSet<>();
        for (final var option : options) {
            if (option == StandardOpenOption.CREATE || option == StandardOpenOption.CREATE_NEW) {
                LOGGER.log(DEBUG, "Ignoring CREATE and CREATE_NEW options. " +
                        "A Jayo file is always already existing.");
                continue;
            }
            optionsSet.add(option);
        }
        return Jayo.sink(path, optionsSet.toArray(new OpenOption[0]));
    }

    @Override
    public @NonNull RawSource source() {
        if (!Files.exists(path)) {
            throw new JayoFileNotFoundException("file does not exist anymore");
        }
        return Jayo.source(path);
    }

    @Override
    public @NonNull String getName() {
        final var fileNamePath = path.getFileName();
        if (fileNamePath == null) {
            throw new IllegalStateException("Jayo prevent zero element files, meaning with no file name.");
        }
        return fileNamePath.toString();
    }

    @Override
    public @NonNegative long getSize() {
        if (!Files.exists(path)) {
            throw new JayoFileNotFoundException("file does not exist anymore");
        }
        try {
            return (Files.isRegularFile(path)) ? Files.size(path) : -1L;
        } catch (IOException e) {
            throw JayoException.buildJayoException(e);
        }
    }

    @Override
    public @NonNull Path getPath() {
        return path;
    }

    @Override
    public @NonNull ByteString hash(@NonNull Digest digest) {
        Objects.requireNonNull(digest);
        return Jayo.hash(source(), digest);
    }

    @Override
    public @NonNull ByteString hmac(@NonNull Hmac hMac, @NonNull ByteString key) {
        Objects.requireNonNull(hMac);
        Objects.requireNonNull(key);
        return Jayo.hmac(source(), hMac, key);
    }

    // shared with Directory

    @Override
    public void atomicMove(final @NonNull Path destination) {
        if (!Files.exists(path)) {
            throw new JayoFileNotFoundException("file does not exist anymore");
        }
        try {
            Files.move(path, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw JayoException.buildJayoException(e);
        }
    }

    @Override
    public void delete() {
        if (!Files.exists(path)) {
            throw new JayoFileNotFoundException("file does not exist anymore");
        }
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw JayoException.buildJayoException(e);
        }
    }

//    public boolean isAbsolute() {
//        return path.isAbsolute();
//    }
}
