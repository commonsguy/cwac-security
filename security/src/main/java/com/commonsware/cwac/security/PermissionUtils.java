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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

public class PermissionUtils {
  // official one is API Level 16, so defined here
  // to avoid setting that minimum SDK requirement

  private static final int PROTECTION_MASK_BASE=0x0000000f;

  public static HashMap<PackageInfo, ArrayList<PermissionLint>> checkCustomPermissions(Context ctxt) {
    HashMap<PackageInfo, ArrayList<PermissionLint>> results=
        new HashMap<PackageInfo, ArrayList<PermissionLint>>();
    PackageManager mgr=ctxt.getPackageManager();

    try {
      PackageInfo self=
          mgr.getPackageInfo(ctxt.getPackageName(),
                             PackageManager.GET_PERMISSIONS);

      for (PackageInfo pkg : mgr.getInstalledPackages(PackageManager.GET_PERMISSIONS)) {
        if (!pkg.packageName.equals(self.packageName)
            && pkg.permissions != null) {
          ArrayList<PermissionLint> lints=
              new ArrayList<PermissionLint>();

          for (PermissionInfo perm : pkg.permissions) {
            PermissionInfo myPerm=
                getPermissionForName(self.permissions, perm.name);

            if (myPerm != null) {
              PermissionLint lint=new PermissionLint(perm);

              int base=perm.protectionLevel & PROTECTION_MASK_BASE;
              int myBase=myPerm.protectionLevel & PROTECTION_MASK_BASE;

              if (base < myBase) {
                lint.wasDowngraded=true;
              }
              else if (base > myBase) {
                lint.wasUpgraded=true;
              }

              if (base == PermissionInfo.PROTECTION_SIGNATURE
                  || base == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM) {
                if (myBase == PermissionInfo.PROTECTION_SIGNATURE
                    || myBase == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM) {
                  lint.signaturesDiffer=true;

                  try {
                    if (SignatureUtils.getOwnSignatureHash(ctxt)
                                      .equals(SignatureUtils.getSignatureHash(ctxt,
                                                                              pkg.packageName))) {
                      lint.signaturesDiffer=false;
                    }
                  }
                  catch (Exception e) {
                    Log.e("PermissionUtils",
                          "Exception comparing signatures", e);
                  }
                }
              }

              CharSequence desc=perm.loadDescription(mgr);
              CharSequence myDesc=myPerm.loadDescription(mgr);

              if (!TextUtils.equals(desc, myDesc)) {
                lint.proseDiffers=true;
              }
              else {
                CharSequence label=perm.loadLabel(mgr);
                CharSequence myLabel=myPerm.loadLabel(mgr);

                if (!TextUtils.equals(label, myLabel)) {
                  lint.proseDiffers=true;
                }
              }

              lints.add(lint);
            }
          }

          if (lints.size() > 0) {
            results.put(pkg, lints);
          }
        }
      }
    }
    catch (NameNotFoundException e) {
      throw new RuntimeException("We do not exist?!?", e);
    }

    return(results);
  }

  private static PermissionInfo getPermissionForName(PermissionInfo[] perms,
                                                     String name) {
    for (PermissionInfo perm : perms) {
      if (name.equals(perm.name)) {
        return(perm);
      }
    }

    return(null);
  }
}
