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

import android.content.Context;
import android.content.ContextWrapper;

public class SecureContextWrapper extends ContextWrapper {
  final private boolean wrapAppContext;
  final private boolean wrapLayoutInflater;

  public SecureContextWrapper(Context base) {
    this(base, false);
  }

  public SecureContextWrapper(Context base, boolean wrapAppContext) {
    this(base, wrapAppContext, false);
  }

  public SecureContextWrapper(Context base, boolean wrapAppContext,
                              boolean wrapLayoutInflater) {
    super(base);

    this.wrapAppContext=wrapAppContext;
    this.wrapLayoutInflater=wrapLayoutInflater;
  }

  @Override
  public Object getSystemService(String name) {
    return(FlagSecureHelper
      .getWrappedSystemService(super.getSystemService(name), name,
        wrapLayoutInflater));
  }

  @Override
  public Context getApplicationContext() {
    Context result=super.getApplicationContext();

    if (wrapAppContext) {
      result=new SecureContextWrapper(result, true);
    }

    return(result);
  }
}