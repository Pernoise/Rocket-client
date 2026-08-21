rm -rf /home/farlaxfard/IdeaProjects/Rocket-client/build
echo "Building the gradle jar"
sleep 1
/home/farlaxfard/IdeaProjects/Rocket-client/gradlew jar copyDeps
sleep 1
echo "Jar built!"
sleep 0.2
echo "Packaging into an appimage..."
sleep 1
/home/farlaxfard/.jdks/temurin-21.0.12/bin/jpackage @/home/farlaxfard/IdeaProjects/Rocket-client/Appimagetools/jpackagerOptions.txt
rm -rf /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir
sleep 0.1
mkdir /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir
mkdir /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir/usr
mkdir /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir/usr/bin
cp /home/farlaxfard/IdeaProjects/Rocket-client/build/resources/main/icons/rocket-launch.png /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir
mv /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir/rocket-launch.png /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir/RocketClient.png
cat > RocketClient.AppDir/RocketClient.desktop << 'EOF'
[Desktop Entry]
Name=RocketClient
Exec=RocketClient
Icon=RocketClient
Type=Application
Categories=Game;
EOF

cat > RocketClient.AppDir/AppRun << 'EOF'
#!/bin/sh
HERE="$(dirname "$(readlink -f "${0}")")"
exec "$HERE/usr/bin/bin/RocketClient" "$@"
EOF
chmod +x RocketClient.AppDir/AppRun
cp -r /home/farlaxfard/IdeaProjects/Rocket-client/build/app-image/RocketClient/* /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir/usr/bin
echo "Running Appimagetools..."
sleep 1
VERSION="0.9.0-beta" /home/farlaxfard/IdeaProjects/Rocket-client/Appimagetools/appimagetool.AppImage /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir
mv /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient-0.9.0-beta-x86_64.AppImage   /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppImage
rm -rf /home/farlaxfard/IdeaProjects/Rocket-client/build
rm -rf /home/farlaxfard/IdeaProjects/Rocket-client/RocketClient.AppDir
echo "Jpackagefied! amaze amaze amaze!"