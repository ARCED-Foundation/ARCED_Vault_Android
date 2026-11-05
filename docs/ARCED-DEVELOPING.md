# Developing ARCEDians Vault for Android

For team members who want to build, test or release the Android app. Fork changes and the
release and upstream-update steps are in [`ARCED.md`](../ARCED.md).

## 1. Set up a Windows PC

1. Install [Android Studio](https://developer.android.com/studio). It brings the Android SDK, the
   emulator and a JDK 21 (`C:\Program Files\Android\Android Studio\jbr`); the build needs JDK 21,
   not a newer one.
2. In Android Studio: Settings → Languages & Frameworks → Android SDK → SDK Tools, tick
   "Android SDK Command-line Tools".
3. Create a GitHub personal access token (classic) with only the `read:packages` scope. The build
   downloads the Bitwarden SDK from Bitwarden's GitHub Packages registry, which needs a token even
   though it is public. Put it in `user.properties` in the repo root (gitignored):

   ```properties
   gitHubToken=ghp_...
   ```

   or set the `GITHUB_TOKEN` environment variable instead.
4. Keep the Gradle cache on the same drive as the repo. With the repo on `D:` and the cache in
   `C:\Users\<you>\.gradle`, KSP fails with "this and base files have different roots". Either
   build with `-Pksp.incremental=false` or set `GRADLE_USER_HOME` to a folder on `D:`.

## 2. Build

From the repo root, in Git Bash:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:assembleFdroidDebug -Pksp.incremental=false
```

The dev APK is `app/build/outputs/apk/fdroid/debug/foundation.arced.vault.dev-fdroid.apk`. It
installs next to the release app (ID `foundation.arced.vault.dev`) and shows the server picker,
so you can point it at a local server.

`./gradlew :app:assembleFdroidRelease` gives an unsigned release APK unless the `ARCED_KEYSTORE_*`
variables are set (see `app/build.gradle.kts`). Only CI signs releases.

## 3. Test against a local server

The local Vaultwarden from ARCED_Password_Manager (`deploy/`, section 5 of its
`ARCED-DEVELOPING.md`) runs at `https://localhost:8443` with a Caddy-signed certificate.

1. Start it: `cd ../ARCED_Password_Manager/deploy && docker compose up -d`.
2. Use an emulator image **without** Play Store ("Google APIs"), so `adb root` works.
3. Forward the port, so `localhost` inside the emulator reaches the host and matches the
   certificate name: `adb reverse tcp:8443 tcp:8443`.
4. Trust Caddy's root certificate as a user CA (the app trusts user CAs, like upstream):

   ```bash
   crt=../ARCED_Password_Manager/deploy/data/caddy/caddy/pki/authorities/local/root.crt
   hash=$(openssl x509 -subject_hash_old -noout -in "$crt")
   adb root && adb shell mkdir -p /data/misc/user/0/cacerts-added
   adb push "$crt" /data/misc/user/0/cacerts-added/$hash.0
   adb shell chmod 644 /data/misc/user/0/cacerts-added/$hash.0 && adb reboot
   ```

5. Dev build: Landing screen → "Logging in on" → Self-hosted → `https://localhost:8443`.
   Release builds have no server picker and always use production, so test server flows with
   the dev build.

## 4. Test list before a release

On an Android 10 emulator and on the newest Android:

- Fresh install: the app opens on the ARCED server with no region picker.
- Create account, log in, log out, log in again.
- Two-step login: authenticator app (TOTP), email code, and a passkey/security key (WebAuthn).
  None of them may open bitwarden.com.
- Add, edit and delete items; pull down on the vault to sync; change an item in the browser
  extension and check it arrives after pull-to-refresh.
- Autofill: Settings → Autofill → turn on the autofill service, fill a login in Chrome and in an
  app.
- Unlock with fingerprint (emulator: Extended controls → Fingerprint).
- Offline: airplane mode, unlock and read items.
- Upgrade: install the previous release APK, log in, then install the new APK over it. You stay
  logged in and the items are still there.
- Update check: with a `latest.json` that has a higher `versionCode`, the app shows
  "Update available" on start.
- About screen shows "© ARCED Foundation" and the Bitwarden GPL notice.
