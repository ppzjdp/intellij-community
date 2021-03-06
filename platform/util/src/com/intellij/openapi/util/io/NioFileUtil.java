// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.util.io;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public final class NioFileUtil {
  /**
   * A drop-in replacement for {@link Files#createDirectories} that doesn't stumble upon symlinks.
   */
  public static @NotNull Path createDirectories(@NotNull Path path) throws IOException {
    if (!Files.isDirectory(path)) {
      if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
        throw new FileAlreadyExistsException(path.toString(), null, "already exists");
      }
      else {
        createDirectories(path.getParent());
        Files.createDirectory(path);
      }
    }
    return path;
  }
}
