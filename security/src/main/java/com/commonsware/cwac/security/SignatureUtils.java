/***
  Copyright (c) 2014 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.security;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SignatureUtils {
  public static String getOwnSignatureHash(Context ctxt)
                                                        throws NameNotFoundException,
                                                        NoSuchAlgorithmException {
    return(getSignatureHash(ctxt, ctxt.getPackageName()));
  }

  public static String getSignatureHash(Context ctxt, String packageName)
                                                                         throws NameNotFoundException,
                                                                         NoSuchAlgorithmException {
    MessageDigest md=MessageDigest.getInstance("SHA-256");
    Signature sig=
        ctxt.getPackageManager()
            .getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures[0];

    return(toHexStringWithColons(md.digest(sig.toByteArray())));
  }

  // based on https://stackoverflow.com/a/2197650/115145

  public static String toHexStringWithColons(byte[] bytes) {
    char[] hexArray=
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F' };
    char[] hexChars=new char[(bytes.length * 3) - 1];
    int v;

    for (int j=0; j < bytes.length; j++) {
      v=bytes[j] & 0xFF;
      hexChars[j * 3]=hexArray[v / 16];
      hexChars[j * 3 + 1]=hexArray[v % 16];

      if (j < bytes.length - 1) {
        hexChars[j * 3 + 2]=':';
      }
    }

    return new String(hexChars);
  }
}
