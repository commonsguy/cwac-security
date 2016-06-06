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

import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public class SecureWindowManagerWrapper implements WindowManager {
  private final WindowManager wm;

  public SecureWindowManagerWrapper(WindowManager wm) {
    this.wm=wm;
  }

  @Override
  public Display getDefaultDisplay() {
    return(wm.getDefaultDisplay());
  }

  @Override
  public void removeViewImmediate(View view) {
    wm.removeViewImmediate(view);
  }

  @Override
  public void addView(View view, ViewGroup.LayoutParams params) {
    wm.addView(view, makeSecure(params));
  }

  @Override
  public void updateViewLayout(View view,
                               ViewGroup.LayoutParams params) {
    wm.updateViewLayout(view, makeSecure(params));
  }

  @Override
  public void removeView(View view) {
    wm.removeView(view);
  }

  private ViewGroup.LayoutParams makeSecure(ViewGroup.LayoutParams params) {
    if (params instanceof LayoutParams) {
      LayoutParams lp=(LayoutParams)params;

      lp.flags |= LayoutParams.FLAG_SECURE;
    }

    return(params);
  }
}