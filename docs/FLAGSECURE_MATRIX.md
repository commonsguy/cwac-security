# FlagSecureHelper Compatibility Test Matrix

This table outlines what is being tried to confirm whether the
`FlagSecureHelper` does what it is supposed to do and doesn't crash
on various Android API levels. A version number indicates that it works
for that version, an issue link indicates that it is not working, and
a blank cell means it has not been tested.

|                              |6.0: Nexus 5                                    |5.1: Nexus 4                                    |5.0: Samsung Galaxy Note 3 SM-N900                |4.4: Motorola Moto E XT1022                     |
|:----------------------------:|:----------------------------------------------:|:----------------------------------------------:|:------------------------------------------------:|:----------------------------------------------:|
|`Spinner` (dropdown mode)     |0.0.3                                           |0.0.3                                           |0.0.3                                             |0.0.3                                           |
|`Spinner` (dialog mode)       |[2](//github.com/commonsguy/flagsecure/issues/2)|[2](//github.com/commonsguy/flagsecure/issues/2)|[2](//github.com/commonsguy/flagsecure/issues/2)  |[2](//github.com/commonsguy/flagsecure/issues/2)|
|`AutoCompleteTextView`        |0.0.3                                           |0.0.3                                           |0.0.3                                             |0.0.3                                           |
|`ShareActionProvider`         |0.0.3                                           |0.0.3                                           |0.0.3                                             |0.0.3                                           |
|native action bar overflow    |0.0.3                                           |0.0.3                                           |[11](//github.com/commonsguy/flagsecure/issues/11)|0.0.3                                           |
|`AlertDialog`                 |0.0.3                                           |0.0.3                                           |0.0.3                                             |0.0.3                                           |
|`Toast`                       |0.0.3                                           |0.0.3                                           |0.0.3                                             |0.0.3                                           |

Android 4.4 was tested using screen shots and screencasts taken via
Android Studio.

Android 5.0+ were tested using:

- media projection APIs for screenshots
- media projection APIs for screencasts

Android 6.0+ were also tested using:

- the Assist API (i.e., what Now On Tap uses)
