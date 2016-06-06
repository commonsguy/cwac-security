/**
 * Copyright (c) 2012-2016 CommonsWare, LLC
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

package com.commonsware.cwac.security.demo.flagsecure;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import com.commonsware.cwac.security.flagsecure.FlagSecureHelper;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SampleDialogFragment extends DialogFragment {
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View form=
        getActivity().getLayoutInflater()
                     .inflate(R.layout.main, null);

    AutoCompleteTextView autocomplete=
      (AutoCompleteTextView)form.findViewById(R.id.autocomplete);

    autocomplete.setAdapter(new ArrayAdapter<>(getActivity(),
      android.R.layout.simple_dropdown_item_1line,
      MainActivity.ITEMS));

    Spinner spinner=(Spinner)form.findViewById(R.id.spinner);
    ArrayAdapter<String> adapter=
      new ArrayAdapter<>(getActivity(),
        android.R.layout.simple_spinner_item,
        MainActivity.ITEMS);

    adapter
      .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);

    spinner=(Spinner)form.findViewById(R.id.spinner2);
    adapter=
      new ArrayAdapter<>(getActivity(),
        android.R.layout.simple_spinner_item,
        MainActivity.ITEMS);
    adapter
      .setDropDownViewResource(
        android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);

    AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
    Dialog dlg=builder
      .setTitle(R.string.activity_label)
      .setView(form)
      .setNegativeButton(android.R.string.cancel, null)
      .create();

    if (MainActivity.fixFlagSecure()) {
      dlg=FlagSecureHelper.markDialogAsSecure(dlg);
    }

    return(dlg);
  }
}
