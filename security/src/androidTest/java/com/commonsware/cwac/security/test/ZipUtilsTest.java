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

package com.commonsware.cwac.security.test;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.commonsware.cwac.security.ZipUtils;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class ZipUtilsTest {
  private static final String TEST_DIR="test";
  private static File externalRoot;

  @BeforeClass
  public static void initOnce() {
    externalRoot=new File(InstrumentationRegistry.getTargetContext().getExternalCacheDir(), TEST_DIR);
  }

  @Before
  public void init() {
    if (externalRoot.exists()) {
      ZipUtils.delete(externalRoot);
    }

    externalRoot.mkdirs();
  }

  @Test
  public void unzipNormalExternal() throws IOException, ZipUtils.UnzipException {
    File asset=new File(externalRoot, "asset.zip");
    File destDir=new File(externalRoot, "result");

    copyAsset("WarOfTheWorlds.zip", asset);
    ZipUtils.unzip(asset, destDir);
    assertWarOfTheWorlds(destDir);
  }

  @Test
  public void unzipNormalExternalDirExists() throws IOException, ZipUtils.UnzipException {
    File asset=new File(externalRoot, "asset.zip");
    File destDir=new File(externalRoot, "result");

    destDir.mkdirs();

    copyAsset("WarOfTheWorlds.zip", asset);
    ZipUtils.unzip(asset, destDir);
    assertWarOfTheWorlds(destDir);
  }

  @Test
  public void unzipHugeExternal() throws IOException {
    File asset=new File(externalRoot, "asset.zip");
    File destDir=new File(externalRoot, "result");

    copyAsset("huge.zip", asset);

    try {
      ZipUtils.unzip(asset, destDir);
      Assert.fail("Did not get ZipUtils.UnzipException!");
    }
    catch (ZipUtils.UnzipException e) {
      Assert.assertTrue(e.getCause() instanceof IllegalStateException);
      Assert.assertFalse(destDir.exists());
    }
  }

  @Test
  public void unzipLotsExternal() throws IOException {
    File asset=new File(externalRoot, "asset.zip");
    File destDir=new File(externalRoot, "result");

    copyAsset("lots.zip", asset);

    try {
      ZipUtils.unzip(asset, destDir);
      Assert.fail("Did not get ZipUtils.UnzipException!");
    }
    catch (ZipUtils.UnzipException e) {
      Assert.assertTrue(e.getCause() instanceof IllegalStateException);
      Assert.assertFalse(destDir.exists());
    }
  }

  @Test
  public void unzipOutsideExternal() throws IOException {
    File asset=new File(externalRoot, "asset.zip");
    File destDir=new File(externalRoot, "result");

    copyAsset("outside.zip", asset);

    try {
      ZipUtils.unzip(asset, destDir);
      Assert.fail("Did not get ZipUtils.UnzipException!");
    }
    catch (ZipUtils.UnzipException e) {
      Assert.assertTrue(e.getCause() instanceof IllegalStateException);
      Assert.assertFalse(destDir.exists());
    }
  }

  @Test
  public void unzipDirectoryNotEmpty() throws IOException {
    File asset=new File(externalRoot, "asset.zip");
    File destDir=new File(externalRoot, "result");

    copyAsset("WarOfTheWorlds.zip", asset);

    destDir.mkdirs();
    new File(destDir, "thisExists.txt").createNewFile();

    try {
      ZipUtils.unzip(asset, destDir);
      Assert.fail("Did not get IOException!");
    }
    catch (IOException e) {
      Assert.assertTrue(destDir.exists());
      Assert.assertEquals(1, destDir.listFiles().length);
    }
    catch (ZipUtils.UnzipException e2) {
      Assert.fail("Got ZipUtils.UnzipException!");
    }
  }

  private void assertWarOfTheWorlds(File destDir) {
    Assert.assertEquals(1, destDir.listFiles().length);

    File book=new File(destDir, "book");

    Assert.assertEquals(9, book.listFiles().length);

    File f=new File(book, "pgepub.css");

    Assert.assertEquals(402, f.length());

    f=new File(book, "contents.json");
    Assert.assertEquals(519, f.length());

    f=new File(book, "0.css");
    Assert.assertEquals(222, f.length());

    f=new File(book, "0.htm");
    Assert.assertEquals(73472, f.length());

    f=new File(book, "1.htm");
    Assert.assertEquals(73779, f.length());

    f=new File(book, "2.htm");
    Assert.assertEquals(67239, f.length());

    f=new File(book, "3.htm");
    Assert.assertEquals(75613, f.length());

    f=new File(book, "4.htm");
    Assert.assertEquals(59420, f.length());

    f=new File(book, "5.htm");
    Assert.assertEquals(21350, f.length());
  }

  static private void copyAsset(String asset, File dst) throws IOException {
    InputStream in=InstrumentationRegistry.getContext().getAssets().open(asset);
    FileOutputStream out=new FileOutputStream(dst);
    byte[] buf=new byte[1024];
    int len;

    while ((len=in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }

    in.close();
    out.close();
  }
}
