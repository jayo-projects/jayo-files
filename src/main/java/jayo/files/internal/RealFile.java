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
import jayo.files.FileMetadata;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
    public @NonNull FileMetadata getMetadata() {
        if (!Files.exists(path)) {
            throw new JayoFileNotFoundException("file does not exist anymore");
        }
        try {
            final var attributes = Files.readAttributes(
                    path,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );
            final var symlinkTarget = (attributes.isSymbolicLink()) ? Files.readSymbolicLink(path) : null;
            return new RealFileMetadata(attributes, symlinkTarget);
        } catch (IOException e) {
            throw JayoException.buildJayoException(e);
        }
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

    public static final class FileBuilder implements File.FileBuilder {
        private final @NonNull Path path;

        public FileBuilder(final @NonNull Path path) {
            this.path = Objects.requireNonNull(path);
        }

        @Override
        public @NonNull File open() {
            return checkAndBuildFile(path);
        }

        @Override
        public @NonNull File create() {
            try {
                return checkAndBuildFile(Files.createFile(path));
            } catch (IOException e) {
                throw JayoException.buildJayoException(e);
            }
        }

        @Override
        public @NonNull File createIfNotExists() {
            if (Files.exists(path)) {
                return checkAndBuildFile(path);
            }
            try {
                return checkAndBuildFile(Files.createFile(path));
            } catch (IOException e) {
                throw JayoException.buildJayoException(e);
            }
        }

        private @NonNull File checkAndBuildFile(final @NonNull Path path) {
            Objects.requireNonNull(path);
            if (!Files.exists(path)) {
                throw new JayoFileNotFoundException("Path does not exist: " + path);
            }
            if (Files.isDirectory(path)) {
                throw new IllegalArgumentException("A Jayo's file cannot be a directory. Use `Directory` instead.");
            }
            if (path.getFileName() == null) {
                throw new IllegalArgumentException("Jayo prevent zero element files, meaning with no file name.");
            }
            return new RealFile(path);
        }
    }
}
