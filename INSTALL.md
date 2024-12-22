# Installing Radio App on Raspberry Pi 5

This guide will help you install Radio App on your Raspberry Pi 5 running Raspberry Pi OS.

## Prerequisites

- Raspberry Pi 5 running Raspberry Pi OS
- Internet connection
- Terminal access

## Installation Steps

1. First, copy the entire `radio-app-backup` folder to your Raspberry Pi. You can use `scp` or any other file transfer method:
   ```bash
   scp -r radio-app-backup pi@your-raspberry-pi:/home/pi/
   ```

2. SSH into your Raspberry Pi or open a terminal if you're using the desktop interface.

3. Navigate to the radio-app-backup directory:
   ```bash
   cd ~/radio-app-backup
   ```

4. Make the installation script executable:
   ```bash
   chmod +x install.sh
   ```

5. Run the installation script with sudo:
   ```bash
   sudo ./install.sh
   ```

   The script will:
   - Install required dependencies (VLC, Java 17, and OpenJFX)
   - Build the application
   - Install the application to /opt/radio-app
   - Create a launcher script and desktop integration

6. Once installation is complete, you can:
   - Launch from terminal: `radio-app`
   - Find "Radio App" in your applications menu under "Audio" category
   - Or double-click the desktop icon if created

## Troubleshooting

If you encounter any issues:

1. Check the application logs:
   ```bash
   cat /tmp/radio-app.log
   ```

2. Make sure VLC is installed:
   ```bash
   sudo apt-get install vlc
   ```

3. Verify Java 17 and JavaFX are installed:
   ```bash
   java -version
   dpkg -l | grep openjfx
   ```

4. Try running the app from terminal to see any error messages:
   ```bash
   /usr/local/bin/radio-app
   ```

5. Verify all files are in place:
   ```bash
   ls -l /opt/radio-app/
   ls -l /usr/local/bin/radio-app
   ls -l /usr/share/applications/radio-app.desktop
   ```

## Uninstallation

To uninstall the app:
```bash
sudo rm -rf /opt/radio-app
sudo rm /usr/local/bin/radio-app
sudo rm /usr/share/applications/radio-app.desktop
```

## Support

If you encounter any issues, please file a bug report in the project's issue tracker.
