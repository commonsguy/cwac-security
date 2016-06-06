# FlagSecureHelper Compatibility Test Matrix

This table outlines what is being tried to confirm whether the
`FlagSecureHelper` does what it is supposed to do and doesn't crash
on various Android API levels. A version number indicates that it works
for that version, an issue link indicates that it is not working, and
a blank cell means it has not been tested.

|                                                                    |6.0: Nexus 5                                         |5.1: Nexus 4                                         |5.0: Samsung Galaxy Note 3 SM-N900                    |4.4: Motorola Moto E XT1022                          |
|:------------------------------------------------------------------:|:---------------------------------------------------:|:---------------------------------------------------:|:----------------------------------------------------:|:---------------------------------------------------:|
|`Spinner` (dropdown mode)                                           |0.7.0                                                |0.7.0                                                |0.7.0                                                 |0.7.0                                                |
|`Spinner` (dialog mode)                                             |[13](//github.com/commonsguy/cwac-security/issues/13)|[13](//github.com/commonsguy/cwac-security/issues/13)|[13](//github.com/commonsguy/cwac-security/issues/13) |[13](//github.com/commonsguy/cwac-security/issues/13)|
|`AutoCompleteTextView`                                              |0.7.0                                                |0.7.0                                                |0.7.0                                                 |0.7.0                                                |
|`ShareActionProvider`                                               |0.7.0                                                |0.7.0                                                |0.7.0                                                 |0.7.0                                                |
|native action bar overflow                                          |0.7.0                                                |0.7.0                                                |[14](//github.com/commonsguy/cwac-security/issues/14) |0.7.0                                                |
|`AlertDialog`                                                       |0.7.0                                                |0.7.0                                                |0.7.0                                                 |0.7.0                                                |
|`Toast`                                                             |0.7.0                                                |0.7.0                                                |0.7.0                                                 |0.7.0                                                |
|[native `Toolbar`](//github.com/commonsguy/cwac-security/issues/15) |                                                     |                                                     |                                                      |                                                     |
|[`appcompat-v7`](//github.com/commonsguy/cwac-security/issues/16)   |                                                     |                                                     |                                                      |                                                     |
|[`Snackbar`](//github.com/commonsguy/cwac-security/issues/17)       |                                                     |                                                     |                                                      |                                                     |
|[context menus](//github.com/commonsguy/cwac-security/issues/18)    |                                                     |                                                     |                                                      |                                                     |
|[`EditText` FAM](//github.com/commonsguy/cwac-security/issues/19)   |                                                     |                                                     |                                                      |                                                     |
|[`PopupWindow`](//github.com/commonsguy/cwac-security/issues/20)    |                                                     |                                                     |                                                      |                                                     |
|[`ListPopupWindow`](//github.com/commonsguy/cwac-security/issues/20)|                                                     |                                                     |                                                      |                                                     |
|[`PopupMenu`](//github.com/commonsguy/cwac-security/issues/20)      |                                                     |                                                     |                                                      |                                                     |

Android 4.4 was tested using screen shots and screencasts taken via
Android Studio.

Android 5.0+ were tested using:

- media projection APIs for screenshots
- media projection APIs for screencasts

Android 6.0+ were also tested using:

- the Assist API (i.e., what Now On Tap uses)
