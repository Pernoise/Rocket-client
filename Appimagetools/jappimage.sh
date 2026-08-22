#!/bin/sh
# Prior to running this, ensure javafx-jmods-21 is located in your home directory (~/javafx-jmods-21).
#Do not forget to replace the --module path with /home/<yourusername>/javafx-jmods-21
#Set the JPACKAGE_CMD variable to a java 21 jpackage installed on your system.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR" || exit 1

sudo rm -rf build

chmod +x ./gradlew

echo "Building the gradle jar..."
sleep 1
./gradlew jar copyDeps || { echo "Gradle build failed!"; exit 1; }
sleep 1
echo "Jar built!"

echo "Packaging into an appimage..."
sleep 1

JPACKAGE_CMD="/home/farlaxfard/.jdks/temurin-21.0.12/bin/jpackage"

$JPACKAGE_CMD @"$SCRIPT_DIR/jpackagerOptions.txt" || { echo "jpackage failed!"; exit 1; }

rm -rf RogueClient.AppDir

mkdir -p RogueClient.AppDir/usr/bin

cp build/resources/main/icons/rogue-launch.png RogueClient.AppDir/RogueClient.png

cat > RogueClient.AppDir/RogueClient.desktop << 'EOF'
[Desktop Entry]
Name=RogueClient
Exec=RogueClient
Icon=RogueClient
Type=Application
Categories=Game;
EOF

cat > RogueClient.AppDir/AppRun << 'EOF'
#!/bin/sh
HERE="$(dirname "$(readlink -f "${0}")")"
exec "$HERE/usr/bin/bin/RogueClient" "$@"
EOF
chmod +x RogueClient.AppDir/AppRun

cp -r build/app-image/RogueClient/* RogueClient.AppDir/usr/bin

echo "Running Appimagetools..."
sleep 1

chmod +x "$SCRIPT_DIR/appimagetool.AppImage"

VERSION="0.9.0-beta" "$SCRIPT_DIR/appimagetool.AppImage" RogueClient.AppDir || { echo "appimagetool failed!"; exit 1; }
mv RogueClient-0.9.0-beta-x86_64.AppImage RogueClient.AppImage

rm -rf build
rm -rf RogueClient.AppDir

echo "Jpackagefied! amaze amaze amaze!"