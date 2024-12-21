# Radio App

A JavaFX-based Internet Radio Player that allows you to search, play, and save your favorite radio stations.

## Features

- Search for radio stations by genre
- Play internet radio streams
- Save favorite stations for quick access
- Display current playing track information (for supported stations)
- Volume control
- Simple and intuitive interface

## Requirements

- Java 17 or higher
- VLC Media Player installed on your system
- Maven for building the project

## Building and Running

1. Clone the repository:
```bash
git clone https://github.com/michael5cents/RadioApp.git
cd RadioApp
```

2. Build with Maven:
```bash
mvn clean package
```

3. Run the application:
```bash
mvn exec:java
```

## Usage

1. **Search for Stations**
   - Enter a genre (e.g., rock, jazz, classical)
   - Click "Search Radio Stations"
   - Results will appear in the search results list

2. **Play a Station**
   - Click on any station in either the search results or favorites list
   - Use the Play/Pause button to control playback
   - Adjust volume using the slider

3. **Save Favorites**
   - Select a station from the search results
   - Click "Save Selected to Favorites"
   - The station will appear in your favorites list
   - Favorites are automatically loaded next time you start the app

## Recent Updates

- Added separate lists for search results and favorites
- Implemented station search using radio-browser.info API
- Added "Save to Favorites" functionality
- Fixed duplicate entries in search results
- Improved error handling and user feedback
- Added graceful shutdown with quit button

## License

This project is licensed under the MIT License - see the LICENSE file for details.
