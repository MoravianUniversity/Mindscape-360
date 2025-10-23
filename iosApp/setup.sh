#!/usr/bin/env bash
# This script compiles the static library for the Cardboard SDK for iOS. After compilation, it will
# create a static library `libGfxPluginCardboard.a` in the `cardboard/build/Release-*` directories.

# TODO: Would like to integrate this into the build process of the iosApp, but for now we need to
#       run this script manually after cloning the repository.

# After you compile, you need to open the iosApp xcode project:
# - Go to iosApp > Targets: iosApp > Build Phases > Link Binary With Libraries:
#   Add AVFoundation.framework and CoreMotion.framework
# - Go to iosApp > Targets: iosApp > Build Settings > Linking - General > Other Linker Flags:
#   Add -lGfxPluginCardboard
# - Go to iosApp > Targets: iosApp > Build Settings > Search Paths > Library Search Paths:
#   Add $(SRCROOT)/../cardboard/build/Release-$(PLATFORM_NAME)

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR/../cardboard"

pod install
for SDK in iphoneos iphonesimulator; do
    xcodebuild -workspace Cardboard.xcworkspace -scheme sdk -sdk "$SDK" -config Release build
    BUILD_DIR="$(xcodebuild -workspace Cardboard.xcworkspace -scheme sdk -sdk "$SDK" -config Release -showBuildSettings 2>&1 | grep -m 1 "CONFIGURATION_BUILD_DIR" | grep -oEi "\/.*")"
    mkdir -p "build/Release-$SDK"
    libtool -static -o "build/Release-$SDK/libGfxPluginCardboard.a" "$BUILD_DIR/"*.a "$BUILD_DIR/Protobuf-C++/"*.a
done
