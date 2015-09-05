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

package com.commonsware.cwac.security.demo.rtp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.commonsware.cwac.cam2.CameraActivity;
import com.commonsware.cwac.cam2.VideoRecorderActivity;
import com.commonsware.cwac.security.RuntimePermissionUtils;
import java.io.File;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends Activity {
  private static final String[] PERMS_ALL={
      CAMERA,
      WRITE_EXTERNAL_STORAGE,
      RECORD_AUDIO
  };
  private static final String[] PERMS_TAKE_PICTURE={
      CAMERA,
      WRITE_EXTERNAL_STORAGE
  };
  private static final int RESULT_PICTURE_TAKEN=1337;
  private static final int RESULT_VIDEO_RECORDED=1338;
  private static final int RESULT_PERMS_INITIAL=1339;
  private static final int RESULT_PERMS_TAKE_PICTURE=1340;
  private static final int RESULT_PERMS_RECORD_VIDEO=1341;
  private static final String PREF_IS_FIRST_RUN="firstRun";
  private static final String STATE_BREADCRUST=
      "com.commonsware.android.perm.tutorial.breadcrust";
  private File rootDir;
  private View takePicture;
  private View recordVideo;
  private TextView breadcrust;
  private SharedPreferences prefs;
  private RuntimePermissionUtils utils;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    utils=new RuntimePermissionUtils(this);
    prefs=PreferenceManager.getDefaultSharedPreferences(this);

    takePicture=findViewById(R.id.take_picture);
    recordVideo=findViewById(R.id.record_video);
    breadcrust=(TextView)findViewById(R.id.breadcrust);

    File downloads=Environment
        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    rootDir=new File(downloads, "RuntimePermTutorial");
    rootDir.mkdirs();

    if (isFirstRun() && utils.useRuntimePermissions()) {
      requestPermissions(PERMS_TAKE_PICTURE,
        RESULT_PERMS_INITIAL);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    updateButtons();  // Settings does not terminate process
                      // if permission granted, only if revoked
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if (breadcrust.getVisibility()==View.VISIBLE) {
      outState.putCharSequence(STATE_BREADCRUST,
        breadcrust.getText());
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    CharSequence cs=savedInstanceState.getCharSequence(STATE_BREADCRUST);

    if (cs!=null) {
      breadcrust.setVisibility(View.VISIBLE);
      breadcrust.setText(cs);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {
    Toast t=null;

    if (resultCode==RESULT_OK) {
      if (requestCode==RESULT_PICTURE_TAKEN) {
        t=Toast.makeText(this, R.string.msg_pic_taken,
            Toast.LENGTH_LONG);
      }
      else if (requestCode==RESULT_VIDEO_RECORDED) {
        t=Toast.makeText(this, R.string.msg_vid_recorded,
            Toast.LENGTH_LONG);
      }

      t.show();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions,
                                         int[] grantResults) {
    updateButtons();

    if (requestCode==RESULT_PERMS_TAKE_PICTURE) {
      if (canTakePicture()) {
        takePictureForRealz();
      }
    }
    else if (requestCode==RESULT_PERMS_RECORD_VIDEO) {
      if (canRecordVideo()) {
        recordVideoForRealz();
      }
    }
  }

  public void takePicture(View v) {
    if (canTakePicture()) {
      takePictureForRealz();
    }
    else if (breadcrust.getVisibility()==View.VISIBLE) {
      breadcrust.setVisibility(View.GONE);
      requestPermissions(utils.netPermissions(PERMS_TAKE_PICTURE),
          RESULT_PERMS_TAKE_PICTURE);
    }
    else if (shouldShowTakePictureRationale()) {
      breadcrust.setText(R.string.msg_take_picture);
      breadcrust.setVisibility(View.VISIBLE);
    }
    else {
      throw new IllegalStateException(getString(R.string.msg_state));
    }
  }

  public void recordVideo(View v) {
    if (canRecordVideo()) {
      recordVideoForRealz();
    }
    else if (!utils.haveEverRequestedPermission(RECORD_AUDIO) ||
        breadcrust.getVisibility()==View.VISIBLE) {
      breadcrust.setVisibility(View.GONE);
      utils.markPermissionAsRequested(RECORD_AUDIO);
      requestPermissions(utils.netPermissions(PERMS_ALL),
          RESULT_PERMS_RECORD_VIDEO);
    }
    else if (shouldShowRecordVideoRationale()) {
      breadcrust.setText(R.string.msg_record_video);
      breadcrust.setVisibility(View.VISIBLE);
    }
    else {
      throw new IllegalStateException(getString(R.string.msg_state));
    }
  }

  private boolean isFirstRun() {
    boolean result=prefs.getBoolean(PREF_IS_FIRST_RUN, true);

    if (result) {
      prefs.edit().putBoolean(PREF_IS_FIRST_RUN, false).apply();
    }

    return(result);
  }

  private void updateButtons() {
    takePicture.setEnabled(couldPossiblyTakePicture());
    recordVideo.setEnabled(couldPossiblyRecordVideo());
  }

  private boolean wasAudioRejected() {
    return(!utils.hasPermission(RECORD_AUDIO) &&
            !utils.shouldShowRationale(this, RECORD_AUDIO) &&
            utils.haveEverRequestedPermission(RECORD_AUDIO));
  }

  private boolean canTakePicture() {
    return(utils.hasPermission(CAMERA) &&
      utils.hasPermission(WRITE_EXTERNAL_STORAGE));
  }

  private boolean canRecordVideo() {
    return(canTakePicture() && utils.hasPermission(RECORD_AUDIO));
  }

  private boolean shouldShowTakePictureRationale() {
    return(utils.shouldShowRationale(this, CAMERA) ||
      utils.shouldShowRationale(this, WRITE_EXTERNAL_STORAGE));
  }

  private boolean shouldShowRecordVideoRationale() {
    return(shouldShowTakePictureRationale() ||
      utils.shouldShowRationale(this, RECORD_AUDIO));
  }

  private boolean couldPossiblyTakePicture() {
    return(!utils.wasPermissionRejected(this, CAMERA) &&
        !utils.wasPermissionRejected(this,
          WRITE_EXTERNAL_STORAGE));
  }

  private boolean couldPossiblyRecordVideo() {
    return(couldPossiblyTakePicture() && !wasAudioRejected());
  }

  private void takePictureForRealz() {
    Intent i=new CameraActivity.IntentBuilder(MainActivity.this)
        .to(new File(rootDir, "test.jpg"))
        .updateMediaStore()
        .build();

    startActivityForResult(i, RESULT_PICTURE_TAKEN);
  }

  private void recordVideoForRealz() {
    Intent i=new VideoRecorderActivity.IntentBuilder(MainActivity.this)
        .quality(VideoRecorderActivity.Quality.HIGH)
        .sizeLimit(5000000)
        .to(new File(rootDir, "test.mp4"))
        .updateMediaStore()
        .forceClassic()
        .build();

    startActivityForResult(i, RESULT_VIDEO_RECORDED);
  }
}