# Recon map: premium music streaming app (MusicFy)

Scope: core loop = search -> play -> skip freely -> download -> listen offline. Platform: Android.
Sources: public help centers and the user's own knowledge of the category. No private APIs, no code of the original read.

## Screens
| ID | screen | purpose |
| --- | --- | --- |
| S01 | Home | popular + new tracks, genre chips |
| S02 | Search | tracks / albums / artists tabs |
| S03 | Library | downloaded tracks, play all, shuffle |
| S04 | Album | cover, play, shuffle, download album, track list |
| S05 | Artist | popular tracks, albums |
| S06 | Mini player | persistent bar |
| S07 | Now playing | art, seek, shuffle/repeat, queue, download |

## Flows
- F01 Find and play: S02 -> S06/S07. Back closes player, clears search, then pops.
- F02 Download a track: any row -> download button -> S03.
- F03 Download an album: S04 -> download album -> S03.
- F04 Offline listening: S03 -> S07, no network.

## Cannot be cloned (skip)
- The original's licensed catalogue (major-label music), its recommendations, social features, podcasts, Connect, lyrics.
- Streaming/downloading the original's content: not allowed by its terms. MusicFy uses a licensed open catalogue (Jamendo) instead.
