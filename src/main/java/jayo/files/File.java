/*
 * Copyright (c) 2024-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.files;

import jayo.ByteString;
import jayo.RawSink;
import jayo.RawSource;
import jayo.crypto.Digest;
import jayo.crypto.Hmac;
import jayo.exceptions.JayoException;
import jayo.exceptions.JayoFileNotFoundException;
import jayo.external.NonNegative;
import jayo.files.internal.RealFile;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A Jayo's File is guaranteed to be a real existing file.
 */
public sealed interface File permits RealFile {
    /**
     * @return a raw sink that writes to this file. {@code options} allow to specify how the file is opened.
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     */
    @NonNull RawSink sink(final @NonNull OpenOption @NonNull ... options);

    /**
     * @return a raw source that reads from this file.
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     */
    @NonNull RawSource source();

    /**
     * In general, one may expect that for a path like {@code Path.of("home", "Downloads", "file.txt")} the name is
     * {@code file.txt}.
     *
     * @return the file name of this file.
     */
    @NonNull String getName();

    /**
     * @return the file size (in bytes). The size of files that are not {@code regular} is unspecified, so this method
     * returns {@code -1L} for them.
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     */
    @NonNegative long getSize();

    /**
     * @return the {@code path} of this file.
     */
    @NonNull Path getPath();

    /**
     * @param digest the chosen message digest algorithm to use for hashing.
     * @return the hash of this File.
     */
    @NonNull ByteString hash(final @NonNull Digest digest);

    /**
     * @param hMac the chosen "Message Authentication Code" (MAC) algorithm to use.
     * @param key the key to use for this MAC operation.
     * @return the MAC result of this File.
     */
    @NonNull ByteString hmac(final @NonNull Hmac hMac, final @NonNull ByteString key);

    /**
     * Atomically moves or renames this file to {@code destination}, overriding {@code destination} if it already
     * exists.
     *
     * @param destination desired path name.
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     * @throws JayoException             if the move failed.
     */
    void atomicMove(final @NonNull Path destination);

    /**
     * Deletes this file
     *
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     * @throws JayoException             if the deletion failed.
     */
    void delete();
        
    final class Builder {
        /**
         * @return a {@code FileBuilder} that will allow to create or open the file this {@link Path} targets.
         */
        public @NonNull FileBuilder of(final @NonNull Path path) {
            Objects.requireNonNull(path);
            return new FileBuilder(path);
        }

        /**
         * @return a {@code FileBuilder} that will allow to create or open the file this {@link java.io.File} targets.
         */
        public @NonNull FileBuilder of(final /*@NonNull*/ java.io.File file) {
            Objects.requireNonNull(file);
            return of(file.toPath());
        }

        /**
         * @return a {@code FileBuilder} that will allow to create or open the file this {@link URI} targets.
         */
        public @NonNull FileBuilder of(final @NonNull URI uri) {
            Objects.requireNonNull(uri);
            return of(Path.of(uri));
        }

        /**
         * @return a {@code FileBuilder} that will allow to create or open a file by converting a path string, or a
         * sequence of strings that when joined form a path string.
         */
        public @NonNull FileBuilder of(final @NonNull String first, final @NonNull String @NonNull ... more) {
            Objects.requireNonNull(first);
            return of(Path.of(first, more));
        }
    }

    final class FileBuilder {
        private final @NonNull Path path;

        private FileBuilder(final @NonNull Path path) {
            this.path = Objects.requireNonNull(path);
        }

        /**
         * Opens this existing file, then returns it.
         * 
         * @return the opened file
         * @throws jayo.exceptions.JayoFileAlreadyExistsException if a file of that name already exists.
         * @throws JayoException                                  if an I/O error occurs or the parent directory does
         *                                                        not exist.
         */
        public @NonNull File open() {
            return checkAndBuildFile(path);
        }

        /**
         * Creates this non-existing file, then returns it.
         * 
         * @return the created file
         * @throws jayo.exceptions.JayoFileAlreadyExistsException if a file of that name already exists.
         * @throws JayoException                                  if an I/O error occurs or the parent directory does
         *                                                        not exist.
         */
        public @NonNull File create() {
            try {
                return checkAndBuildFile(Files.createFile(path));
            } catch (IOException e) {
                throw JayoException.buildJayoException(e);
            }
        }

        /**
         * Creates this file if it did not exist yet, else open it, then returns it.
         * 
         * @return the created or opened file
         * @throws JayoException if an I/O error occurs or the parent directory does not exist.
         */
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
