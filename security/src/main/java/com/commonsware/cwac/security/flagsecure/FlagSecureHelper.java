/**
 * Copyright (c) 2016 CommonsWare, LLC
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.security.flagsecure;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;

public class FlagSecureHelper {
  public static Object getWrappedSystemService(Object service,
                                               String name) {
    return(getWrappedSystemService(service, name, false));
  }

  public static Object getWrappedSystemService(Object service,
                                               String name,
                                               boolean wrapLayoutInflater) {
    if (Context.WINDOW_SERVICE.equals(name)) {
      boolean goAhead=true;

      for (StackTraceElement entry : Thread.currentThread().getStackTrace()) {
        try {
          Class cls=Class.forName(entry.getClassName());

          if (Dialog.class.isAssignableFrom(cls)) {
            goAhead=false;
            break;
          }
        }
        catch (ClassNotFoundException e) {
          // ???
        }
      }

      if (goAhead) {
        service=new SecureWindowManagerWrapper((WindowManager)service);
      }
    }
    else if (Context.LAYOUT_INFLATER_SERVICE.equals(name) && wrapLayoutInflater) {
      LayoutInflater original=(LayoutInflater)service;
      Context securified=
        new SecureContextWrapper(original.getContext(), true, true);

      service=original.cloneInContext(securified);
    }

    return(service);
  }

  public static Dialog markDialogAsSecure(Dialog dlg) {
    dlg.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
      WindowManager.LayoutParams.FLAG_SECURE);

    return(dlg);
  }

  public static Toast makeSecureToast(Context ctxt, CharSequence text,
                                      int duration) {
    return(Toast.makeText(new SecureContextWrapper(ctxt, true, true),
      text, duration));
  }

  public static Toast makeSecureToast(Context ctxt,
                                      @StringRes int resId,
                                      int duration) {
    return(makeSecureToast(ctxt, ctxt.getString(resId), duration));
  }
}
