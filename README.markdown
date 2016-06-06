CWAC Security: Helping You Help Your Users Defend Their Data
============================================================

This project contains utility code related to Android security
measures.

At present, it contains:
 
- a `PermissionUtils` class with a `checkCustomPermissions()`
static method, to help you detect if another app has defined your
custom permissions before your app was installed

- a `RuntimePermissionUtils` class to help you with the
Android 6.0+ runtime permission system

- a `TrustManagerBuilder` to help you create a custom `TrustManager`,
describing what sorts of SSL certificates you want to support in your
HTTPS operations

- a `SignatureUtils` class to help you determine the SHA-256 hash of the
signing key of some package, to compare against known values, to help
detect whether you are about to be communicating with some hacked version of an app

- a `ZipUtils` class with an `unzip()` method, that safely handles
a few types of malformed ZIP archives when attempting to unzip the
contents to your desired directory

- a `FlagSecureHelper` for working around
[Android framework bugs involving `FLAG_SECURE`](docs/FLAGSECURE.md)

This Android library project is 
[available as a JAR](https://github.com/commonsguy/cwac-security/releases)
or as an artifact for use with Gradle. To use that, add the following
blocks to your `build.gradle` file:

```groovy
repositories {
    maven {
        url "https://s3.amazonaws.com/repo.commonsware.com"
    }
}

dependencies {
    compile 'com.commonsware.cwac:security:0.7.+'
}
```

Or, if you cannot use SSL, use `http://repo.commonsware.com` for the repository
URL.

**NOTE**: The JAR name, as of v0.3.1, has a `cwac-` prefix, to help distinguish it from other JARs.

Usage: checkCustomPermissions()
------------------------------
Custom permissions in Android are "first one in wins". In other
words, whatever app first has a `<permission>` element for a
given `android:name` gets to define, for all subsequent apps,
what the details are for that permission. And, courtesy of Android's
rules for informing users about permissions, the app that
defined the permission can hold the permission without the user's
knowledge.

This has some security implications, which are covered in greater
detail [in this paper](PERMS.md).

The `checkCustomPermissions()` method is designed to help you detect
if another app has defined the same custom permissions that you are
defining. Typically, developers expect to be the first one to define
the custom permission, but that may not be true, with consequences
for the developers and their users.

Calling `checkCustomPermissions()` is easy: just pass it a `Context`,
such as your launcher `Activity`.

What it returns is a `HashMap<PackageInfo, ArrayList<PermissionLint>>`,
which will require some explanation.

What `checkCustomPermissions()` does is find all of the custom
permissions in your app that you have declared via `<permission>`
elements. Then, it scans all other apps on the device, finding all
*their* custom permissions, and sees if there is a match on the
permission name (`android:name` attribute).

Each entry in the `HashMap` represents one app that has redefined
one or more of your custom permissions, keyed by
[the `PackageInfo` object](https://developer.android.com/reference/android/content/pm/PackageInfo.html)
describing that application. Each permission that
has been so redefined will be in the `ArrayList`. So, if you define
two custom permissions, but some other app only redefined one, there
will only be one entry in the `ArrayList`.

Each `PermissionLint` in the `ArrayList` contains the following
`public` fields:

- `PermissionInfo perm` providing details of the permission
*as declared in the other app*

- `boolean wasDowngraded`, which will be `true` if you declared
the permissison to be `signature`, but the other app declared
it to be `normal` or `dangerous`

- `boolean signatureDiffers`, which will be `true` if you declared
the permission to be `signature`, and the other app also declared it
to be `signature`, and the other app is signed by a different signing key
than was your app

- `boolean wasUpgraded`, which will be `true` if you declared
the permission to be `normal` or `dangerous`, but the other
app declared it to be `signature`

- `boolean proseDiffers`, which will be `true` if, for the
user's configured device locale, the label or description of
the other app's edition of this permission differs from your
edition of this permission

Hence, if all four boolean fields are `false`, the permission
in the other app is functionally identical to your own definition,
at least for this user and this locale.

The expectation is that you would call `checkCustomPermissions()`
on the first run of your app after installation. If you get back
an empty `HashMap`, then you can continue your first run normally.
If you get back a non-empty `HashMap`, you can decide what to do
with the information, including:

- warning the user about possible data leakage to other apps

- sending information about the pre-defined permission to your
servers, so you can track possible malware attacks targeting
your application and users

Usage: `RuntimePermissionUtils`
-------------------------------
Create an instance using the constructor, passing in any handy
`Context`, such as your `Activity`:

```java
utils=new RuntimePermissionUtils(this);
```

From there, you can call the following on the instance, regardless
of API level of the device that you are on:

- `haveEverRequestedPermission()`, which takes the name of
a `dangerous` permission (e.g., `Manifest.permission.READ_CONTACTS`)
and returns `true` if you have ever called `markPermissionAsRequested()`
for that same permission. Use this to track whether or not you
have asked for permissions that you are not automatically asking
for on the first run of your app.

- `hasPermission()`, which takes the name of a `dangerous` permission
and returns `true` if the user has granted you the permission,
`false` otherwise.

- `shouldShowRationale()`, which takes an `Activity` plus the name
of a `dangerous` permission and returns `true` if you previously
requested the permission and the user denied it, or `false` otherwise.
Use this to determine if you should be showing some information to
the user to help convince them to grant you the permission in some
subsequent `requestPermissions()` call.

- `wasPermissionRejected()`, which takes an `Activity` plus the name
of a `dangerous` permission and returns `true` if you previously
requested the permission, the user denied it, and the use checked
the "don't ask again" checkbox. Use this to determine if you need
to direct the user to the Settings app in order to grant you the
permission manually.

- `netPermissions()`, which takes a `String[]` of permission names
that you want to pass to `requestPermissions()`, and returns the
subset of that array representing the permissions that you do not
yet hold. This allows you to declare the `String[]` as a
`final static` constant, yet does not force the user to have to click
through dialogs for permissions they have already granted.

The `demoRuntimePerms/` project in this repo demonstrates the use
of these methods.

Usage: `TrustManagerBuilder`
----------------------------
To keep this README to a sensible length, discussion of `TrustManagerBuilder`
is broken out into [its own page](https://github.com/commonsguy/cwac-security/blob/master/TrustManagerBuilder.markdown).

Usage: SignatureUtils
---------------------
To find out the SHA-256 hash of some app, call `SignatureUtils.getSignatureHash()`,
passing in some `Context` and the package name of the app. This returns a capitalized,
colon-delimited SHA-256 hash string... the same format that you get when using
Java 7's **`keytool`** to examine a signing key. You can then compare this value with
the expected value (e.g., a string resource in your own app), and take steps if they
do not match.

You can find out your own package's signature via the convenience method
`SignatureUtils.getOwnSignatureHash()`, just supplying a `Context` as a parameter.
While you might be tempted to use this for the purposes of seeing if *you* have been
tampered with, whoever does the tampering would likely remove your call to
`getOwnSignatureHash()` as a part of that tampering. Hence, this will only catch
stupid attackers, which may or may not be worth the investment in effort.

There is also a family of methods for validating an `Intent`,
to identify who will respond to it and ensuring that the app
housing that third-party comoponent is signed by an expected signing key.
These methods include:

- `validateActivityIntent()`
- `validateBroadcastIntent()`
- `validateServiceIntent()`

You supply:

- any `Context`
- an `Intent` that you intend to use, probably with
`setPackageName()` called on it to narrow it down to a single app
- the expected signature hash of that app (either as a single
`String` or as a `List<String>` if there are multiple possibles
hashes, such as a Play Store hash and an F-Droid hash)
- a `boolean` flag (`failIfHack`)
 
In general, there are three possible outcomes of calling this method:

1. You get a `SecurityException`, because `failIfHack` is true,
and we found some component whose app does not match the
desired hash. The user may have installed a repackaged
version of this app that is signed by the wrong key.

2. You get `null`. If `failIfHack` is `true`, this means that no
component was found that matches the `Intent`. If `failIfHack`
is `false`, this means that no component was found that matches
the `Intent` and has a valid matching signature.

3. You get an `Intent`. This means we found a matching component
that has a matching signature. The `Intent` will be a copy of
the passed-in `Intent`, with the component name set to the
matching component, so the `Intent` will only go to this
one component.

Usage: ZipUtils
---------------
Use the static `unzip()` methods to unzip a ZIP-style archive. Both methods
have the same first pair of parameters:

- a `File` pointing to the ZIP archive

- a `File` pointing to the destination directory where the ZIP archive
should be unzipped (note: this directory does not have to already exist;
if it does exist, it must be empty)

One `unzip()` method just takes those parameters. The other takes
a pair of additional integers:

- the maximum number of entries in the ZIP archive; archives with
more entries than this will be rejected

- the maximum size in bytes of the unzipped contents; archives
larger than this will be rejected

Both `unzip()` methods throw a `ZipUtils.UnzipException` if there
is a problem. If an `UnzipException` is thrown, `unzip()` will
also "roll back" any existing work and delete the destination
directory.

Both `unzip()` methods can throw an `IOException`. This will indicate
that the destination directory that you provided existed and was
not empty. In this case, the destination directory is left alone.

If you wish to unzip the archive, and have its contents go into
a directory that already has files in it, you will need to first
unzip to a temporary directory, then move over the files you
want to move.

The approach used here is based on
[CERT's suggested unzip code](https://www.securecoding.cert.org/confluence/display/java/IDS04-J.+Safely+extract+files+from+ZipInputStream),
with minor modifications to make it a bit more Android-friendly.

Usage: FlagSecureHelper
-----------------------
The documentation for `FlagSecureHelper` has been pulled out into
[a separate page](docs/FLAGSECURE.md).

Dependencies
------------
This project has no runtime dependencies. It is tested and supported on API Level 9 and
higher. It may well work on older devices, though that is unsupported and untested.
If you determine that
the library (not the demos) do not work on an older-yet-relevant
version of Android, please
file an [issue](https://github.com/commonsguy/cwac-security/issues).

Also note that testing of `TrustStoreBuilder`
has only been done using `HttpsURLConnection` and `OkHttp`. It should work with
`HttpClient` and other stacks.

Version
-------
This is version v0.7.0 of this module, meaning it is coming along
rather nicely.

Demo
----
In the `demoA/` sub-project you will find an application that uses
`checkCustomPermissions()` to see if some other app has already
defined a custom permission. The `demoB/` sub-project does not
use CWAC-Security, but defines that permission, so that you can
verify that `demoA` works as expected.

There is no demo app for `TrustStoreBuilder` at this time. If you are
aware of a public server that either uses a self-signed certificate or
a private certificate authority, one for which a demo app might make sense,
and one where the maintainer of the server will not mind, please
file an [issue](https://github.com/commonsguy/cwac-security/issues).

There is an instrumentation test suite in the `androidTest`
sourceset of the main `security` module. It contains a `ZipUtilsTest`
class that tests the `ZipUtils` code.

The `demoRuntimePerms/` project in this repo demonstrates the use
of `RuntimePermissionUtils`.

Additional Documentation
------------------------
[The Busy Coder's Guide to Android Development](https://commonsware.com/Android)
demonstrates everything from this library, over a series of chapters.

License
-------
The code in this project is licensed under the Apache
Software License 2.0, per the terms of the included LICENSE
file.

Questions
---------
If you have questions regarding the use of this code, please post a question
on [StackOverflow](http://stackoverflow.com/questions/ask) tagged with
`commonsware-cwac` and `android` after [searching to see if there already is an answer](https://stackoverflow.com/search?q=[commonsware-cwac]+camera). Be sure to indicate
what CWAC module you are having issues with, and be sure to include source code 
and stack traces if you are encountering crashes.

If you have encountered what is clearly a bug, or if you have a feature request,
please post an [issue](https://github.com/commonsguy/cwac-security/issues).
The [contribution guidelines](CONTRIBUTING.md)
provide some suggestions for how to create a bug report that will get
the problem fixed the fastest.

Do not ask for help via Twitter.

Also, if you plan on hacking
on the code with an eye for contributing something back,
please open an issue that we can use for discussing
implementation details. Just lobbing a pull request over
the fence may work, but it may not.
Again, the [contribution guidelines](CONTRIBUTING.md) should help here.

Release Notes
-------------
- v0.7.0: added `FlagSecureHelper`, demo project, and related docs
- v0.6.3: added more `Intent` validation options
- v0.6.2: added `validateBroadcastIntent()`
- v0.6.1: bug fix for unzipping some directory structures
- v0.6.0: added `RuntimePermissionUtils`
- v0.5.2: require the destination directory for `unzip()` to be empty or not exist
- v0.5.1: added `sync()` call to ensure stuff written to disk by the time `unzip()` returns
- v0.5.0: reorganized `security` into official Android Studio structure, added `ZipUtils`
- v0.4.1: updated for Android Studio 1.0 and new AAR publishing system
- v0.4.0: added signature check and `signatureDiffers` to `PermissionUtils`
- v0.3.1: added `cwac-` prefix to JAR
- v0.3.0: added certificate memorization to `TrustManagerBuilder`
- v0.2.1: added `SignatureUtils`
- v0.2.0: added `TrustManagerBuilder` and supporting classes
- v0.1.0: initial release

Who Made This?
--------------
<a href="http://commonsware.com">![CommonsWare](http://commonsware.com/images/logo.png)</a>

