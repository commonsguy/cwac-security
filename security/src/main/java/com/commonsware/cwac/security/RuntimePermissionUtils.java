/***
 Copyright (c) 2015 CommonsWare, LLC

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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.ArrayList;

/**
 * Utility class for Android 6.0+ runtime permissions.
 */
public class RuntimePermissionUtils {
  final SharedPreferences prefs;
  final Context ctxt;

  /**
   * Standard constructor.
   *
   * @param ctxt any Context will do (e.g., Activity)
   */
  public RuntimePermissionUtils(Context ctxt) {
    this.ctxt=ctxt.getApplicationContext();
    prefs=ctxt.getSharedPreferences("com.commonsware.cwac.security",
      Context.MODE_PRIVATE);
  }

  /**
   * @return true if runtime permissions are supported on this
   * version of Android, false otherwise
   */
  public boolean useRuntimePermissions() {
    return(Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1);
  }

  /**
   * Checks to see if you already hold a particular permission.
   *
   * @param perm name of the permission (ideally pulled from
   *             Manifest.permission)
   * @return true if you have the permission, false otherwise
   */
  public boolean hasPermission(String perm) {
    if (useRuntimePermissions()) {
      return(ctxt.checkSelfPermission(perm)==PackageManager.PERMISSION_GRANTED);
    }

    return(true);
  }

  /**
   * Determines if the user has denied this permission previously,
   * but has not yet checked the "don't ask again" checkbox. Use
   * this do determine if you should explain to the user more why
   * you want a permission, given that the user already denied
   * it.
   *
   * @param host the Activity associated with this UI
   * @param perm name of the permission (ideally pulled from
   *             Manifest.permission)
   * @return true if you should show rationale, false otherwise
   */
  public boolean shouldShowRationale(Activity host, String perm) {
    if (useRuntimePermissions()) {
      return(!hasPermission(perm) &&
        host.shouldShowRequestPermissionRationale(perm));
    }

    return(false);
  }

  /**
   * Indicates if the user has permanently rejected this permission,
   * meaning any call to requestPermissions() for it will fail.
   * Use this to steer the user to Settings to grant you the
   * permission.
   *
   * This method assumes that you have already asked for the
   * permission. If you are requesting this permission on first
   * run of the app, you are good to go. If you are requesting this
   * permission on demand (e.g., when the user clicks a particular
   * action bar item), then you might use haveEverRequestedPermission()
   * and markPermissionAsRequested() to track whether or not you
   * have asked for the permission before.
   *
   * @param host the Activity associated with this UI
   * @param perm name of the permission (ideally pulled from
   *             Manifest.permission)
   * @return true if you should route the user to Settings to grant
   * you the permission, false otherwise
   */
  public boolean wasPermissionRejected(Activity host, String perm) {
    return(!hasPermission(perm) && !shouldShowRationale(host, perm));
  }

  /**
   * Wrapper around SharedPreferences for tracking whether you
   * have asked for a permission before, since the Android 6.0
   * API does not tell you this.
   *
   * @param perm name of the permission (ideally pulled from
   *             Manifest.permission)
   * @return true if you have asked for this permission before,
   * false otherwise
   */
  public boolean haveEverRequestedPermission(String perm) {
    return(prefs.getBoolean(perm, false));
  }

  /**
   * Wrapper around SharedPreferences for tracking whether you
   * have asked for a permission before, since the Android 6.0
   * API does not tell you this. Call this when you have asked
   * (or are about to ask) for the permission, so that future
   * calls to haveEverRequestedPermission() will return true.
   *
   * @param perm name of the permission (ideally pulled from
   *             Manifest.permission)
   */
  public void markPermissionAsRequested(String perm) {
    prefs.edit().putBoolean(perm, true).apply();
  }

  /**
   * Android 6.0 will happily allow you to request a permission
   * that the user has already granted, showing the user that
   * permission in the permission-request dialog again. This is
   * bad for the user (extra confusing clicks) and bad for you
   * (what if the user granted it before and now rejects it?).
   *
   * Given the permission array that you would ordinarily pass
   * to requestPermissions(), this method will return the
   * subset of that array representing permissions that you do
   * not already hold. If you are sure that 1+ permissions in the
   * array are not yet held, you can just pass the result of
   * netPermissions() right along to requestPermissions(). Otherwise,
   * check the returned array length; if it is zero, you have all
   * the permissions and do not need to call requestPermissions().
   *
   * @param wanted array of permission names
   * @return array of permission names, for the permissions from
   * the input array that you do not already hold
   */
  public String[] netPermissions(String[] wanted) {
    ArrayList<String> result=new ArrayList<String>();

    for (String perm : wanted) {
      if (!hasPermission(perm)) {
        result.add(perm);
      }
    }

    return(result.toArray(new String[result.size()]));
  }
}
