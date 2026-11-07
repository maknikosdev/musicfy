# MusicFy

My music streaming app for Android, written in Kotlin with Jetpack Compose. Listen to music, skip between songs as often as you like, download tracks or whole albums, and play them offline.

## Open in Android Studio
1. File → Open → select the `MusicFy` folder (Android Studio Ladybug or newer, JDK 17).
2. Wait for the Gradle sync. It downloads Gradle 8.9 and all libraries automatically.
3. Add your catalogue key (see below).
4. Run the app on a device or emulator (Android 8.0+).
5. For an APK/AAB: Build → Generate Signed App Bundle / APK.

## Music sources
MusicFy has two catalogues. Switch between them with the **Jamendo / Audius** chips on the Home and Search screens.

- **Jamendo**: legal catalogue of independent artists (Creative Commons). Streaming **and** downloads for offline. Needs a free API key (below).
- **Audius**: open catalogue of independent artists. Streaming only (no downloads). No API key needed.

### Jamendo API key

1. Create a free account at https://devportal.jamendo.com and create an application.
2. Copy the **Client ID**. The Client Secret is not needed and must never go into the code.
3. Add it in one of two ways:
   - in `local.properties`: `jamendo.client_id=YOUR_CLIENT_ID`
   - or inside the app, on the Home screen, in the `client_id` field.

## Features
- Playback with notification and lock-screen controls, also in the background (Media3).
- Unlimited next/previous, shuffle, repeat (all or one), seek bar and play queue.
- Two music sources (Jamendo and Audius) with a switch on the Home and Search screens.
- Search for songs, albums and artists, album and artist pages, and genres on the Home screen.
- Playlists: create and name your own playlists from the Library, add any song with the playlist button next to it (also in the full-screen player), rename or delete playlists and remove songs. Playlists are saved on the device.
- Download a track or a whole album. Downloads show up in the Library and play offline inside the app. Files are kept in the app's private storage.
- Polished UI: press animations on every button, row and card, skeleton loading, animated screen transitions, and a colour-washed full-screen player.
- Song changes slide sideways (forward = slide left, back = slide right) in the player and mini player. You can also swipe the cover or the mini player left/right to skip.
- Android back button: closes the full-screen player first, then clears the search text, then goes back screen by screen (album → search → home → exit).

## Limitations
- The catalogues are Jamendo's and Audius', not the major labels'. Downloading only works for Jamendo tracks that allow it; Audius tracks are stream-only.
- Downloads run while the app is open (there is no separate download service).
- No liked songs yet.
- Before publishing on the Play Store I need to check Jamendo's terms for commercial use, add a privacy policy and run a trademark check on the name.
