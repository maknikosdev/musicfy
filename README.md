# MusicFy

My music streaming app for Android, written in Kotlin with Jetpack Compose. Listen to music, skip between songs as often as you like, download tracks or whole albums, and play them offline.

## Open in Android Studio
1. File → Open → select the `MusicFy` folder (Android Studio Ladybug or newer, JDK 17).
2. Wait for the Gradle sync. It downloads Gradle 8.9 and all libraries automatically.
3. Add your catalogue key (see below).
4. Run the app on a device or emulator (Android 8.0+).
5. For an APK/AAB: Build → Generate Signed App Bundle / APK.

## Music catalogue and API key
Music comes from the Jamendo API, a legal catalogue of independent artists (Creative Commons) that allows streaming and downloading.

1. Create a free account at https://devportal.jamendo.com and create an application.
2. Copy the **Client ID**. The Client Secret is not needed and must never go into the code.
3. Add it in one of two ways:
   - in `local.properties`: `jamendo.client_id=YOUR_CLIENT_ID`
   - or inside the app, on the Home screen, in the `client_id` field.

## Features
- Playback with notification and lock-screen controls, also in the background (Media3).
- Unlimited next/previous, shuffle, repeat (all or one), seek bar and play queue.
- Search for songs, albums and artists, album and artist pages, and genres on the Home screen.
- Download a track or a whole album. Downloads show up in the Library and play offline inside the app. Files are kept in the app's private storage.
- Android back button: closes the full-screen player first, then clears the search text, then goes back screen by screen (album → search → home → exit).

## Limitations
- The catalogue is Jamendo's, not the major labels'. Downloading only works for tracks that allow it; the rest are stream-only.
- Downloads run while the app is open (there is no separate download service).
- No playlists or liked songs yet.
- Before publishing on the Play Store I need to check Jamendo's terms for commercial use, add a privacy policy and run a trademark check on the name.
