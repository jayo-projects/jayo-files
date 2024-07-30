/*
 * Copyright (c) 2024-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.files;

import jayo.ByteString;
import jayo.RawWriter;
import jayo.RawReader;
import jayo.crypto.Digest;
import jayo.crypto.Hmac;
import jayo.exceptions.JayoException;
import jayo.exceptions.JayoFileAlreadyExistsException;
import jayo.exceptions.JayoFileNotFoundException;
import jayo.external.NonNegative;
import jayo.files.internal.RealFile;
import org.jspecify.annotations.NonNull;

import java.net.URI;
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
    @NonNull
    RawWriter writer(final @NonNull OpenOption @NonNull ... options);

    /**
     * @return a raw source that reads from this file.
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     */
    @NonNull
    RawReader reader();

    /**
     * In general, one may expect that for a path like {@code Path.of("home", "Downloads", "file.txt")} the name is
     * {@code file.txt}.
     *
     * @return the file name of this file.
     */
    @NonNull
    String getName();

    /**
     * @return the number of readable bytes in this file. The amount of storage resources consumed by this file may be
     * larger (due to block size overhead, redundant copies for RAID, etc.), or smaller (due to file system compression,
     * shared inodes, etc.). The size of files that are not {@code regular} is unspecified, so this method returns
     * {@code -1L} for them.
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     */
    @NonNegative
    long getSize();

    /**
     * @return the {@code path} of this file.
     */
    @NonNull
    Path getPath();

    /**
     * @return the metadata of this file.
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     * @throws JayoException             if this file cannot be accessed due to a connectivity problem, permissions
     *                                   problem, or other issue.
     */
    @NonNull
    FileMetadata getMetadata();

    /**
     * @param digest the chosen message digest algorithm to use for hashing.
     * @return the hash of this File.
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     */
    @NonNull
    ByteString hash(final @NonNull Digest digest);

    /**
     * @param hMac the chosen "Message Authentication Code" (MAC) algorithm to use.
     * @param key  the key to use for this MAC operation.
     * @return the MAC result of this File.
     * @throws JayoFileNotFoundException if the file does not exist anymore.
     */
    @NonNull
    ByteString hmac(final @NonNull Hmac hMac, final @NonNull ByteString key);

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

    /**
     * @return a {@code FileBuilder} that will allow to create or open the file this {@link Path} targets.
     */
    static @NonNull FileBuilder from(final @NonNull Path path) {
        Objects.requireNonNull(path);
        return new RealFile.FileBuilder(path);
    }

    /**
     * @return a {@code FileBuilder} that will allow to create or open the file this {@link java.io.File} targets.
     */
    static @NonNull FileBuilder from(final java.io.@NonNull File file) {
        Objects.requireNonNull(file);
        return from(file.toPath());
    }

    /**
     * @return a {@code FileBuilder} that will allow to create or open the file this {@link URI} targets.
     */
    static @NonNull FileBuilder from(final @NonNull URI uri) {
        Objects.requireNonNull(uri);
        return from(Path.of(uri));
    }

    /**
     * @return a {@code FileBuilder} that will allow to create or open a file by converting a path string, or a
     * sequence of strings that when joined form a path string.
     */
    static @NonNull FileBuilder from(final @NonNull String first, final @NonNull String @NonNull ... more) {
        Objects.requireNonNull(first);
        return from(Path.of(first, more));
    }

    sealed interface FileBuilder permits RealFile.FileBuilder {
        /**
         * Opens this existing file, then returns it.
         *
         * @return the opened file
         * @throws JayoFileAlreadyExistsException if a file of that name already exists.
         * @throws JayoException                  if an I/O error occurs or the parent directory does not exist.
         */
        @NonNull
        File open();

        /**
         * Creates this non-existing file, then returns it.
         *
         * @return the created file
         * @throws JayoFileAlreadyExistsException if a file of that name already exists.
         * @throws JayoException                  if an I/O error occurs or the parent directory does not exist.
         */
        @NonNull
        File create();

        /**
         * Creates this file if it did not exist yet, else open it, then returns it.
         *
         * @return the created or opened file
         * @throws JayoException if an I/O error occurs or the parent directory does not exist.
         */
        @NonNull
        File createIfNotExists();
    }
}
