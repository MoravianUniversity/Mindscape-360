# Mindscape-360

Mindscape 360VR delivers brief, guided mindfulness sessions embedded within nature-based 360-degree
environments and is compatible with widely available Google Cardboard headset viewers.

## Google Cardboard Usage

The core of this app is based on [MultiplatformComposeCardboard](https://github.com/MoravianUniversity/MultiplatformComposeCardboard),
which demos cross-platform Google Cardboard rendering of scenes, stills, and videos in Kotlin
Compose. However, there are several modifications and corrections that should be backported to the
original library. These include:
- Fixing audio playback issues when the silent mode is enabled on the device
- Improving video playback on iOS devices
- Improving QR code scanning on iOS devices
- Updating API usages to be compatible with newer versions of Android and iOS

## Setup

To set up the project, follow these steps:

```bash
git clone https://github.com/MoravianUniversity/Mindscape-360.git
cd Mindscape-360
git submodule update --init --recursive
ln -s cardboard/proto

# should build with ./iosApp/setup.sh but that seems to be broken at the moment
cd cardboard
pod install
wget "https://github.com/MoravianUniversity/Mindscape-360/releases/download/v1.0.1/cardboard-build-ios.tar.gz"
tar -xzf cardboard-build-ios.tar.gz
cd ..
```

To build the Android App, open the root directory in Android Studio and build/run it. There is
likely a `gradle` that works as well (like `gradle :androidApp:assembleDebug`), but I haven't tested
it.

To build the iOS App, open `iosApp/iosApp.xcodeproj` in Xcode and build/run it. Running in the
simulator may run into some issues during playback, but physical devices should work fine.

## Available Videos

There are no video in the app itself. Instead, they are downloaded and cached from an AWS S3
bucket. The list of available videos and their metadata/previews are stored in a JSON file in the
S3 bucket as well.

These videos were recorded by a 360-degree camera and then transcoded using ffmpeg:

```bash
ffmpeg -i input.mp4 -vf scale=2160:-1 -c:v libx264 -preset veryslow -crf 25 -c:a aac -b:a 128k -movflags +faststart output.mp4
```

Also, a scale of 2560:-1 was attempted, but it didn't seem to improve the quality much while
significantly increasing the file size, so 2160:-1 was used.

## Todo

- Move all okio code to kotlinx-io
- iOS: support antiliasing and mipmaps
- iOS: fix partial video caching
- iOS: when leaving the VR view, the top status bar is messed up and scrolls funny
- Android: when the app is put in the background for >8 secs the renderer dies
- The panoramic "dome" isn't quite right? (doesn't take into account the floor height?)
- Remember scroll position in the video list when going back from the VR view

## License

This code is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.
