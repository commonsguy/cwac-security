/**
 * Copyright (c) 2015 CommonsWare, LLC
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.security;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// inspired by https://www.securecoding.cert.org/confluence/display/java/IDS04-J.+Safely+extract+files+from+ZipInputStream

/**
 * Utilities for safely unzipping ZIP-style archives.
 */
public class ZipUtils {
  private static final int BUFFER_SIZE=16384;
  private static final int DEFAULT_MAX_ENTRIES=1024;
  private static final int DEFAULT_MAX_SIZE=1024*1024*64;

  /**
   * Unzips a ZIP-style archive to the designated directory.
   * Reproduces entire directory tree from the ZIP archive.
   * Entire operation fails if ZIP file is invalid (attempts
   * to write outside of designated directory, too big, or
   * too many entries). The "too big" and "too many entries"
   * rules are hard-coded in this method; use the four-parameter
   * unzip() method to configure those.
   *
   * If an UnzipException is thrown, anything previously written
   * to the designated directory is deleted, as is the designated
   * directory itself.
   *
   * @param zipFile the ZIP archive to unzip
   * @param destDir the directory to unzip the contents to
   * @throws UnzipException if something goes haywire
   */
  public static void unzip(File zipFile, File destDir) throws UnzipException {
    unzip(zipFile, destDir, DEFAULT_MAX_ENTRIES, DEFAULT_MAX_SIZE);
  }

  /**
   * Unzips a ZIP-style archive to the designated directory.
   * Reproduces entire directory tree from the ZIP archive.
   * Entire operation fails if ZIP file is invalid (attempts
   * to write outside of designated directory, too big, or
   * too many entries). The "too big" and "too many entries"
   * rules are hard-coded in this method; use the four-parameter
   * unzip() method to configure those.
   *
   * If an UnzipException is thrown, anything previously written
   * to the designated directory is deleted, as is the designated
   * directory itself.
   *
   * @param zipFile the ZIP archive to unzip
   * @param destDir the directory to unzip the contents to
   * @param maxEntries throw an UnzipException if the ZIP
   *                   archive contains more than this number
   *                   of entries
   * @param maxSize throw an UnzipException if any individual
   *                entry in the ZIP archive is bigger than
   *                this size in bytes
   * @throws UnzipException if something goes haywire
   */
  public static void unzip(File zipFile, File destDir,
                           int maxEntries, int maxSize) throws UnzipException {
    try {
      final FileInputStream fis=new FileInputStream(zipFile);
      final ZipInputStream zis=new ZipInputStream(new BufferedInputStream(fis));
      ZipEntry entry;
      int entries=0;
      long total=0;

      destDir.mkdir();

      try {
        while ((entry=zis.getNextEntry()) != null) {
          int bytesRead;
          final byte data[]=new byte[BUFFER_SIZE];
          final String zipCanonicalPath=validateZipEntry(entry.getName(), destDir);

          if (entry.isDirectory()) {
            new File(zipCanonicalPath).mkdir();
          }
          else {
            final FileOutputStream fos=new FileOutputStream(zipCanonicalPath);
            final BufferedOutputStream dest=new BufferedOutputStream(fos, BUFFER_SIZE);

            while (total + BUFFER_SIZE <= maxSize && (bytesRead=zis.read(data, 0, BUFFER_SIZE)) != -1) {
              dest.write(data, 0, bytesRead);
              total+=bytesRead;
            }

            dest.flush();
            fos.getFD().sync();
            dest.close();

            if (total + BUFFER_SIZE > maxSize) {
              throw new IllegalStateException("Too much output from ZIP");
            }
          }

          zis.closeEntry();
          entries++;

          if (entries > maxEntries) {
            throw new IllegalStateException("Too many entries in ZIP");
          }
        }
      }
      finally {
        zis.close();
      }
    }
    catch (Throwable t) {
      if (destDir.exists()) {
        delete(destDir);
      }

      throw new UnzipException("Problem in unzip operation, rolling back", t);
    }
  }

  // inspired by http://pastebin.com/PqJyzQUx

  /**
   * Recursively deletes a directory and its contents.
   *
   * @param f The directory (or file) to delete
   * @return true if the delete succeeded, false otherwise
   */
  public static boolean delete(File f) {
    if (f.isDirectory()) {
      for (File child : f.listFiles()) {
        if (!delete(child)) {
          return(false);
        }
      }
    }

    return(f.delete());
  }

  private static String validateZipEntry(String zipEntryRelativePath,
                                         File destDir) throws IOException {
    File zipEntryTarget=new File(destDir, zipEntryRelativePath);
    String zipCanonicalPath=zipEntryTarget.getCanonicalPath();

    if (zipCanonicalPath.startsWith(destDir.getCanonicalPath())) {
      return(zipCanonicalPath);
    }

    throw new IllegalStateException("ZIP entry tried to write outside destination directory");
  }

  /**
   * Exception raised if something goes wrong in the unzip
   * work. Use getCause() to examine the underlying exception
   * (e.g., IllegalStateException).
   */
  public static class UnzipException extends Exception {
    public UnzipException(String detailMessage, Throwable throwable) {
      super(detailMessage, throwable);
    }
  }
}
