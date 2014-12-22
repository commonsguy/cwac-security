# The Custom Permission Problem

Android, since the beginning, has offered both platform permissions
(defined by the framework) and custom permissions (defined by apps).

Unfortunately, custom permissions have some undocumented limitations
that make them intrinsically risky. Specifically, custom permissions
can be defined by anyone, at any time, and "first one in wins", which
opens up the possibility of unexpected behavior.

Here, we will walk through some
scenarios and show where the problems arise, plus discuss how to
mitigate them as best we can.

**UPDATE 2014-08-04**: The "L" Developer Preview has different behavior
with respect to this issue, described [later in this document](https://github.com/commonsguy/cwac-security/blob/master/PERMS.md#l-developer-preview-behavior).

## Scenarios

All of the following scenarios focus on three major app profiles.

App A is an app that defines a custom permission in its manifest,
such as:

```xml
<permission
	android:name="com.commonsware.cwac.security.demo.OMG"
	android:description="@string/perm_desc"
	android:label="@string/perm_label"
	android:protectionLevel="normal"/>
```

App A also defends a component using the `android:permission`
attribute, referencing the custom permission:

```xml
<provider
	android:name="FileProvider"
	android:authorities="com.commonsware.cwac.security.demo.files"
	android:exported="true"
	android:grantUriPermissions="false"
	android:permission="com.commonsware.cwac.security.demo.OMG">
	<grant-uri-permission android:path="/test.pdf"/>
</provider>
```

App B has a `<uses-permission>` element to declare to the user that
it wishes to access components defended by that permission:

```xml
<uses-permission android:name="com.commonsware.cwac.security.demo.OMG"/>
```

App C has the same `<uses-permission>` element. The difference is
that App B *also* has the `<permission>` element, just as App A
does, albeit with different descriptive information (e.g.,
`android:description`) and, at times, a different protection level.

All three apps are signed with different signing keys, because
in the real world they would be from different developers.

So, to recap:

- A defines a permission and uses it for defense
- B defines the same permission and requests to hold it
- C just requests to hold this permission

With all that in mind, let's walk through some possible scenarios,
focusing on two questions:

1. What is the user told, when the app is installed through
normal methods (i.e., not via **`adb`**), regarding this
permission?

2. What access, if any, does App B or App C have to the `ContentProvider`
from App A?

### The Application SDK Case (A, Then C)

Suppose the reason why App A has defined a custom permission is because
it wants third-party apps to have the ability to access its secured
components... but only with user approval. By defining a custom
permission, and having third-party apps request that permission,
the user should be informed about the requested permission and can
make an informed decision.

Conversely, if an app tries to access a secured component but
has not requested the permission, the access attempt should fail.

App C has requested the custom permission via the `<uses-permission>`
element. If the permission -- defined by App A -- has an
`android:protectionLevel` of `normal` or `dangerous`, the user
will be informed about the requested permission at install time. If
the user continues with the installation, App C can access the
secured component.

If, however, the `android:protectionLevel` is `signature`, the user
is not informed about the requested permission at install time, as
the system can determine on its own whether or not the permission
should be granted. In this case, App A and App C are signed with
different signing keys, so Android silently ignores the permission
request. If the user continues with installation, then App C tries
to access App A's secured component, App C crashes with a
`SecurityException`.

In other words, this all works as expected.

### The Application SDK Problem Case (C, Then A)

However, in many cases, there is nothing forcing the user to install
App A before App C. This is particularly true for publicly-distributed
apps on common markets, like the Play Store.

When the user installs App C, the user is not informed about the
request for the custom permission, presumably because that permission has not
yet been defined. If the user later installs App A, App C is not
retroactively granted the permission, and so App C's attempts
to use the secured component fail.

This works as expected, though it puts a bit of a damper on custom
permissions. One way to work around this would be for the user to
uninstall App C, then install it again (with App A already installed).
This returns us to the original scenario from the preceding section.
However, if the user has data in App C, losing that data may be a problem
(as in a "let's give App C, or perhaps App A, one-star ratings on
the Play Store" sort of problem).

### The Peer Apps Case, Part One (A, Then B)

Suppose now we augment our SDK-consuming app (formerly App C) to
declare the same permission that App A does, in an attempt to allow
the two apps to be installed in either order. That is what App B is:
the same app as App C, but where it has the same `<permission>`
element as does App A in its manifest.

This scenario is particularly important where both apps could be
of roughly equal importance to the user. In cases where App C is
some sort of plugin for App A, it is not unreasonable for the
author of App A to require App A to be installed first. But, if
Twitter and Facebook wanted to access components of each others' apps,
it would be unreasonable for either of those firms to mandate that
their app must be installed first. After all, if Twitter wants to
be installed first, and Facebook wants to be installed first, one
will be disappointed.

If the user installs App A (the app defending a component with
the custom permission) before App B, the user will be notified at
install time about App B's request for this permission. Notably,
the information shown on the installation security screen will
contain App A's description of the permission. And, if the user goes
ahead and installs App B, App B can indeed access App A's secured
component, since it was granted permission by the user.

Once again, everything is working as expected. Going back to the
two questions:

1. The user is informed when App B or App C requests the permission
defined by App A.

2. App B and App C can hold that permission if and only if they meet the
requirements of the protection level

### The Peer Apps Case, Part Two (B, Then A)

What happens if we reverse the order of installation? After all, if
App A and App B are peers, from the standpoint of the user, there
is roughly a 50% chance that the user will install App B before
App A.

Here is where things go off the rails.

**The user is not informed about App B's request for the custom permission.**

The user will be informed about any platform permissions that the
app requests via other `<uses-permission>` elements. If there are none,
the user is told that App B requests no permissions... despite the
fact that it does.

When the user installs App A, the same thing occurs. Of course, since
App A does not have a `<uses-permission>` element, this is not all
that surprising.

However, at this point, **even though the user was not informed**, App B
holds the custom permission and can access the secured component.

This is bad enough when both parties are ethical. App B could be
a piece of malware, though, designed to copy the data from App A,
ideally without the user's knowledge. And, if App B is installed before
App A, that would happen.

So, going to the two questions:

1. The user is **not** informed about App B's request for the permission...

2. ...but App B gets it anyway and can access the secured component

### The Downgraded-Level Malware Case (B, Then A, Again)

You might think that the preceding problem would only be for
`normal` or `dangerous` protection levels. If App A defines
a permission as requiring a matching `signature`, and App A marks a
component as being defended by that permission, Android must require
the signature match, right?

Wrong.

The behavior is identical to the preceding case. Android does
not use the *defender's* protection level. It uses the *definer's*
protection level, meaning the protection level of whoever was installed
first and had the `<permission>` element.

So, if App A has the custom permission defined as `signature`, and
App B has the custom permission defined as `normal`, if App B is
installed first, the behavior is as shown in the preceding section:

1. The user is **not** informed about App B's request for the permission...

2. ...but App B gets it anyway and can access the secured component,
despite the signatures not matching

### The Peer Apps Case With a Side Order of C

What happens if we add App C back into the mix? Specifically, what
if App B is installed first, then App A, then App C?

When App C eventually gets installed, the user is prompted for the
custom permission that App C requests via `<uses-permission>`.
However, the description that the user sees is from App B, the one
that first defined the custom `<permission>`. Moreover, the
protection level is whatever App B defined it to be. So if App B
downgraded the protection level from App A's intended `signature`
to be `normal`, App C can hold that permission and access the
secured App A component, even if it is signed by another signing key.

Not surprisingly, the same results occur if you install App B,
then App C, then App A.

## Behavior Analysis

The behavior exhibited in these scenarios is consistent with
two presumed implementation "features" of Android's permission system:

1. First one in wins. In other words, the first app (or framework,
in the case of the OS's platform permissions) that defines a
`<permission>` for a given `android:name` gets to determine what
the description is and what the protection level is.

2. The user is only prompted to confirm a permission if the
app being installed has a `<uses-permission>` element, the
permission was already defined by some other app, and the
protection level is not `signature`.

## Risk Assessment

The "first one in wins" rule is a blessing and a curse. It is a
curse, insofar as it opens up the possibility for malware to hold
a custom permission without the user's awareness of that, and even
to downgrade a `signature`-level permission to `normal`. However,
it is a blessing, in that the malware would have to be installed first;
if it is installed second, either its request to hold the permission
will be seen by the user (`normal` or `dangerous`) or the request to
hold the permission will be rejected (`signature`).

This makes it somewhat unlikely for a piece of malware to try to 
sneakily make off with data. Eventually, if enough users start to
ask publicly why App B needs access to App A's data (for cases where
App A was installed first and the user knows about the permission
request), somebody in authority may eventually realize that this
is a malware attack. Of course, "eventually" may be a rather long time.

However, there are some situations where Android's custom permission
behavior presents risk even greater than that. If the attacker has
a means of being *sure* that their app was installed first, they can
hold any permission from any third-party app they want to that was
known at install time.

For example:

- Somebody could sell a used Android device, and the buyer could
neglect to factory-reset it, and the malware could be installed
by the seller

- Somebody could sell a used Android device with a ROM mod
preinstalled, based off of a normal ROM mod (e.g., CyanogenMod), but
with an additional bit of malware installed, to prevent a factory
reset from foiling the attack'

- Somebody could distribute devices to users who might think the device
is "factory clean" and not laden with malware (e.g., devices given
as gifts)

- Somebody could distribute devices to users who might think that
the pre-installed malware is actually a legitimate app (e.g.,
devices given to employees by an employer wishing to monitor usage
by examining protected data from third-party apps)

## Mitigation

The "first one in wins" rule also leads us to a mitigation strategy:
On first run of our app, see if any other app has defined permissions
that we have defined. If that has happened, then we are at risk, and
take appropriate steps. If, however, no other app has defined our
custom permissions, then the Android permission system should work
for us, and we can proceed as normal.

[The CWAC-Security library](https://github.com/commonsguy/cwac-security)
provides some helper code to detect
other apps defining the same custom permissions that you define.

## "L" Developer Preview Behavior

The "L" Developer Preview only allows apps signed with the same signing key to define the same `<permission>`
element. If a user tries to install an app that defines the same `<permission>` element as does some other already-installed
app, and the two apps are not signed by the same signing key, the second app's installation fails with an
`INSTALL_FAILED_DUPLICATE_PERMISSION` error. The actual `protectionLevel` of the `<permission>` does not
matter in this case -- even a `normal` permission has this effect. Similarly, this occurs even if the
`<permission>` elements have the same definition, down to the same values for the same string resources
for the label and description.

On the plus side, this avoids the permission sneak attacks that are described in this document.

However, this puts more emphasis on getting the installation order right. For example, a plugin can no longer
define the `<permission>` that the host app defines, unless the host and the plugin are signed by the
same signing key, eliminating third-party plugins. Instead, the host must be the only app that
defines the `<permission>`, which in turn means that the plugin must be installed after the host for
it to get the permission.

## Acknowledgements

The author (Mark Murphy) would like to thank:

- [Mark Carter](http://twitter.com/marcardar), whose
[comments on a StackOverflow question](https://stackoverflow.com/questions/11730085/android-custom-permission-fails-based-on-app-install-order/11730133#comment32392353_11730133)
brought this vulnerability to my attention,
and who also provided the first proof of concept implementation

- ["Justin Case"](https://twitter.com/TeamAndIRC), *nom de plume* of
an Android security researcher, for helping to confirm that this issue
was known, albeit perhaps not that widely, and for providing the
inspiration for the `PermissionUtils` class in the CWAC-Security
library
