# Radio App

**Author:** [Michael5cents](https://github.com/michael5cents)

A Java-based internet radio player with ICY metadata support for streaming radio stations. This application allows you to listen to your favorite online radio stations while displaying real-time metadata like song titles and artist information.

[View Author Details](AUTHORS)

## Features

- 🎵 Stream online radio stations
- 📝 Display real-time ICY metadata (song titles, artist information)
- ⭐ Save and manage favorite radio stations
- 🎨 Simple and intuitive interface
- 🔄 Automatic metadata updates
- 💾 Persistent favorites storage

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

## Installation

1. Clone the repository:
```bash
git clone https://github.com/michael5cents/RadioApp.git
cd RadioApp
```

2. Build the project using Maven:
```bash
mvn clean package
```

3. Run the application:
```bash
java -jar target/radio-app-1.0-SNAPSHOT.jar
```

## Usage

1. **Adding a Radio Station**
   - Enter the stream URL in the input field
   - Click "Add Station" to save it to favorites

2. **Playing a Station**
   - Select a station from your favorites
   - Click "Play" to start streaming
   - The current song information will display automatically

3. **Managing Favorites**
   - Your favorite stations are automatically saved
   - Remove stations using the "Remove" button
   - Favorites are stored in `radio_favorites.json`

## Technical Details

### Components

- **App.java**: Main application class handling the UI and playback
- **IcyMetadataReader.java**: Handles ICY metadata parsing from radio streams

### Architecture

The application uses a simple but effective architecture:

- JavaFX for the user interface
- Custom ICY metadata parser for stream information
- JSON-based persistence for favorites
- Thread-safe audio playback handling

### Configuration

Radio station favorites are stored in `radio_favorites.json` in the following format:

```json
{
  "stations": [
    {
      "name": "Station Name",
      "url": "http://stream.url"
    }
  ]
}
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to all the open-source radio stations
- JavaFX community for the UI framework
- Contributors and users of the application
