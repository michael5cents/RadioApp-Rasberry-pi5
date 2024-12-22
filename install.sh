#!/bin/bash

# Exit on error
set -e

echo "Installing Radio App for Raspberry Pi..."

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo "Please run as root (use sudo)"
    exit 1
fi

# Update package lists
echo "Updating package lists..."
apt-get update

# Install dependencies
echo "Installing dependencies..."
apt-get install -y vlc openjdk-17-jdk openjdk-17-jre maven openjfx

# Build the application
echo "Building application..."
cd "$(dirname "$0")"
mvn clean package

# Create installation directory
echo "Creating installation directory..."
INSTALL_DIR="/opt/radio-app"
mkdir -p "$INSTALL_DIR"
cp target/radio-app.jar "$INSTALL_DIR/"
cp target/lib/* "$INSTALL_DIR/"

# Copy icon
echo "Copying icon..."
cp icon.svg "$INSTALL_DIR/icon.svg"

# Create launcher script
echo "Creating launcher script..."
cat > /usr/local/bin/radio-app << 'EOL'
#!/bin/bash
exec 1>/tmp/radio-app.log 2>&1
/usr/lib/jvm/java-17-openjdk-arm64/bin/java \
  --module-path /usr/share/openjfx/lib \
  --add-modules javafx.controls,javafx.graphics,javafx.media \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  -Djavafx.platform=gtk \
  -Dprism.forceGPU=true \
  -jar /opt/radio-app/radio-app.jar
EOL
chmod +x /usr/local/bin/radio-app

# Create desktop integration
echo "Creating desktop integration..."
cat > /usr/share/applications/radio-app.desktop << EOL
[Desktop Entry]
Name=Radio App
Comment=Internet Radio Player
Exec=/usr/local/bin/radio-app
Icon=/opt/radio-app/icon.svg
Terminal=false
Type=Application
Categories=Audio;Player;
StartupWMClass=com.radio.App
EOL

echo "Installation complete! You can now find Radio App in your applications menu."
