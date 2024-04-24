package jayo.files.internal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Objects;

final class Utils {
    // un-instantiable
    private Utils() {
    }

    static @Nullable Instant instantFromFileTime(final @NonNull FileTime fileTime) {
        Objects.requireNonNull(fileTime);
        final var instant = fileTime.toInstant();
        return Instant.EPOCH.equals(instant) ? null : instant;
    }
}
