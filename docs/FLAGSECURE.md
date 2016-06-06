# About the FLAG_SECURE Child Window Issue

`FLAG_SECURE` can be placed on a `Window` to indicate that the
contents of this `Window` should be visible to the user but not captured
in screenshots and similar facilities.

Unfortunatately, the Android framework sometimes creates its own
`Window` instances, such as the drop-down in a `Spinner`. Even if you
set `FLAG_SECURE` on the `Window` for an activity, the Android framework
does not pass that flag to any other windows created on behalf of that
activity, and those windows show up in:

- Screenshots and screencasts taken by the media projection APIs on Android 5.0+

- The Assist API (e.g., Now On Tap) on Android 6.0+

- Android Studio screen recordings on Android 4.4+

They might also show up in earlier devices using
device-supplied screenshot options, though this has not yet been tested.

## The Scope of the Problem

This has been demonstrated to affect:

- `AutoCompleteTextView`
- `Spinner` (both dropdown and dialog modes)
- the overflow menu of the framework-supplied action bar
- `ShareActionProvider`
- `Dialog` and subclasses (e.g., `AlertDialog`)
- `Toast`

Of these, only the `Dialog` offers us access to its `Window`, on which
we could apply `FLAG_SECURE`, for developers that realize that this is
required.

For example, this screencast shows a sample app, without any use
of `FLAG_SECURE`:

![Demo App, Without `FLAG_SECURE`](insecure.gif)

This is the same sample app, where `FLAG_SECURE` has been applied to the
activity, but where other windows spawned by the aforementioned UI
elements still appear:

![Demo App, With `FLAG_SECURE`](bug.gif)

In addition to the aforementioned UI elements that exhibit this bug,
this bug might also affect:

- `appcompat-v7` ports of those elements (e.g., its own overflow)
- `Toolbar` (and its `appcompat-v7` port)
- `Snackbar`
- context menus

These have not yet been tested. Also, there are no official tests
yet for the underlying widgets used for many of these popups: `PopupWindow`,
`ListPopupWindow`, and `PopupMenu`.

Google has officially stated that
[all of this is working as intended](https://code.google.com/p/android/issues/detail?id=210590).

## Dealing with the Issue

If you are using `FLAG_SECURE`, you should thoroughly exercise your app's
UI on Android 4.4+ while recording a screencast &mdash; the Android Studio screen recorder
would be a simple tool to use. Then, play back that screencast, see what
windows show up, and identify those that contain sensitive information
that should not appear. Some of the windows that appear will not contain
sensitive information &mdash; here, the risk is that you might add
sensitive information to them in the future but forget about this bug.

Then, you have two main courses of action: rewrite your UI to avoid
the UI elements that are leaking this information, or attempt to patch
the problem via some utility code in this library.

The `security` library in this project offers a `FlagSecureHelper` that
wraps up various mitigation utilities to help work around the `FLAG_SECURE`
issue. Specifically, it attempts to add `FLAG_SECURE` to all windows
that are created "behind your back" by the Android framework. Instructions
on [the project home page](https://github.com/commonsguy/cwac-security/)
show you how to add this library to your project.

Then, in your `Activity` subclass, override the `getSystemService(String name)`
method to look like this:

```java
@Override
public Object getSystemService(String name) {
  Object result=super.getSystemService(name);

  return(FlagSecureHelper.getWrappedSystemService(result, name));
}
```

This will handle several of the cases, notably `AutoCompleteTextView`
and `Spinner`.

If you have your own dialogs, in your `onCreate()` method of your
`DialogFragment`, after you create your `Dialog`, pass the `Dialog`
to `FlagSecureHelper.markDialogAsSecure()`. This returns your
`Dialog` to you, so you can pass it along (e.g., as the return value
from `onCreate()`):

```java
AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
Dialog dlg=builder
  .setTitle(R.string.activity_label)
  .setView(form)
  .setNegativeButton(android.R.string.cancel, null)
  .create();

return(FlagSecureHelper.markDialogAsSecure(dlg));
```

To raise a secure `Toast`, use `FlagSecureHelper.makeSecureToast()` in much
the same way as you would use `Toast.makeText()`:

```java
FlagSecureHelper
  .makeSecureToast(this, R.string.msg_toast, Toast.LENGTH_LONG)
  .show();
```

For example, here is a screencast of the same sample app as shown
above, but where `FlagSecureHelper` has been applied:

![Demo App, With `FLAG_SECURE` Bug Mitigation Code](secure.gif)

As you can see (or, rather, *not* see), all windows used in the app
are marked with `FLAG_SECURE`.

Since the issue is seen most on Android 4.4+, those are the versions
of Android that this code is tested on. You are welcome to try using
`FlagSecureHelper` on older Android versions, or only apply it when
`Build.VERSION.SDK_INT` is 19 or higher.

[This matrix](FLAGSECURE_MATRIX.md) lists the test environments, what
works, what does not work, and what has not yet been tested.

## Lessons for Widget Authors

If you create a widget or other UI element in Android, and you directly
or indirectly display a separate window that is logically tied to the
activity, you need to do one of two things:

The best solution, in the author's opinion, is for you to check to see
if the hosting activity has `FLAG_SECURE` on its window, and if so apply
`FLAG_SECURE` to the window(s) that you create. These helper methods
let you find out if `FLAG_SECURE` is applied to a `Window` or
`Activity`:

```java
public static boolean usesFlagSecure(Context ctxt) {
  if (ctxt instanceof Activity) {
    return(usesFlagSecure((Activity)ctxt));
  }

  throw new IllegalArgumentException("Not an activity context!");
}

public static boolean usesFlagSecure(Activity a) {
  return(usesFlagSecure(a.getWindow()));
}

public static boolean usesFlagSecure(Window w) {
  int flags=w.getAttributes().flags;

  return((flags & WindowManager.LayoutParams.FLAG_SECURE)!=0);
}
```

Alternatively, you could have a `public` API for your UI element that
exposes access to the `Window`, so developers using your UI element can
apply `FLAG_SECURE` if desired.

## The Demo App

The `demoFlagSecure/` module contains demonstration activities for confirming
that the utility code does what it says it does. An early version of this
sample app was used for the screencasts shown earlier in this document.

This module has three product flavors:

- `insecure` does not apply `FLAG_SECURE`

- `bug` applies `FLAG_SECURE` to the activity, but does not do anything
beyond that

- `secure` applies `FLAG_SECURE` to the activity and applies the
mitigation utility code outlined in this repository

## About the Implementation

The mitigation utility code relies upon wrapping the `WindowManager`
system service, to inject `FLAG_SECURE` on any windows it is asked to
create. It also wraps the `LayoutManager` system service, to ensure
that it uses a `Context` that employs the wrapped `WindowManager`. This
is all handled inside of `FlagSecureHelper.getWrappedSystemService()`;
any requests for system services other than those two are left
unmodified.

Also, due to [this bug](https://code.google.com/p/android/issues/detail?id=211022),
we cannot use the wrapped `WindowManager` inside of a dialog.

As a result, this code is *seriously nasty*. If it were not because
this represents a privacy and security issue, the author would never
dream of trying to use these techniques, as the author expects a
never-ending parade of compatibility issues.

## Support

Please [file an issue](https://github.com/commonsguy/cwac-security/issues)
if you encounter problems using this code (e.g.,
it crashes in certain scenarios) or if you find other framework-created
child windows that need to be made secure.

While you are welcome to submit a pull request, it might be used more
as inspiration for a separate implementation rather than being used
directly.

## Acknowledgments

The author would like to acknowledge the assistance of an anonymous
contributor who pointed out this issue.

## Timeline

Here are the key events related to this issue, in chronological order:

- 2016/05/19: The problem comes to the author's attention
- 2016/05/20
  - The author reproduces the problem
  - The author files a security bug report following Google's guidelines
  - Google assigns an internal tracking ID to the issue
- 2016/05/26: Google assigns a severity of "Moderate" to the issue
- 2016/05/31: Google retracts the severity and states that they "have concluded this is currently working as intended for previous versions of Android"
- 2016/06/01
  - The author notifies Google about disclosure plans, since they have declined the issue
  - Google opens [the issue](https://code.google.com/p/android/issues/detail?id=210590) to the public
- 2016/06/06: The author opens this repository to the public

## About the Author

Mark Murphy is the founder and owner of [CommonsWare](https://commonsware.com)
and is the author of [The Busy Coder's Guide to Android Development](https://commonsware.com/Android).
