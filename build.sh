#!/bin/sh
# Builds Autosim.apk without Gradle: aapt (R.java) -> javac -> dx -> aapt -> zipalign -> apksigner
set -e
cd "$(dirname "$0")"
SDK=/usr/lib/android-sdk/platforms/android-23/android.jar
rm -rf build/classes build/apk build/gen && mkdir -p build/classes build/apk build/gen
aapt package -f -m -J build/gen -M AndroidManifest.xml -S res -I "$SDK"
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$SDK" -Xlint:-options -d build/classes \
    $(find src -name "*.java") build/gen/nl/rarevogel/autosim/R.java
dalvik-exchange --dex --output=build/apk/classes.dex build/classes
aapt package -f -0 arsc -0 ttf -0 png -0 ogg -M AndroidManifest.xml -S res -A assets -I "$SDK" -F build/unsigned.apk
(cd build/apk && aapt add ../unsigned.apk classes.dex >/dev/null)
zipalign -f -p 4 build/unsigned.apk build/aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
    -alias autosim -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Autosim" >/dev/null 2>&1
apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
    --out Autosim.apk build/aligned.apk
apksigner verify Autosim.apk && ls -la Autosim.apk
