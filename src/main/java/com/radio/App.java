package com.radio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

import java.io.*;
import java.util.*;

public class App extends Application {
    private final String DEFAULT_STATION = "http://s8.myradiostream.com/15672/listen.mp3";
    private final String DEFAULT_STATION_NAME = "Popz Place Radio";
    private final String FAVORITES_FILE = "radio_favorites.json";
    private Map<String, String> favorites = new HashMap<>();
    private Label nowPlayingLabel;
    private Label titleLabel;
    private Label artistLabel;
    private Button playPauseButton;
    private Slider volumeSlider;
    private AudioPlayerComponent mediaPlayer;
    private boolean isPlaying = false;
    private IcyMetadataReader metadataReader;

    @Override
    public void start(Stage primaryStage) {
        initializeVLC();
        mediaPlayer = new AudioPlayerComponent();

        loadFavorites();
        favorites.putIfAbsent(DEFAULT_STATION_NAME, DEFAULT_STATION);
        saveFavorites();

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.TOP_CENTER);

        // Labels for station and song info
        nowPlayingLabel = new Label("Ready to Play");
        titleLabel = new Label("Title: ");
        artistLabel = new Label("Artist: ");
        
        // Style the labels
        titleLabel.setStyle("-fx-font-weight: bold");
        artistLabel.setStyle("-fx-font-weight: bold");
        
        playPauseButton = new Button("Play");
        playPauseButton.setOnAction(e -> togglePlayPause());

        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        
        HBox volumeBox = new HBox(10);
        volumeBox.setAlignment(Pos.CENTER);
        volumeBox.getChildren().addAll(new Label("Volume:"), volumeSlider);

        ComboBox<String> favoritesCombo = new ComboBox<>();
        favoritesCombo.getItems().addAll(favorites.keySet());
        favoritesCombo.setValue(DEFAULT_STATION_NAME);
        favoritesCombo.setOnAction(e -> {
            String selected = favoritesCombo.getValue();
            if (selected != null) {
                playStation(favorites.get(selected), selected);
            }
        });

        TextField stationNameField = new TextField();
        stationNameField.setPromptText("Station Name");
        TextField stationUrlField = new TextField();
        stationUrlField.setPromptText("Station URL");
        Button addButton = new Button("Add to Favorites");
        
        addButton.setOnAction(e -> {
            String name = stationNameField.getText().trim();
            String url = stationUrlField.getText().trim();
            if (!name.isEmpty() && !url.isEmpty()) {
                favorites.put(name, url);
                saveFavorites();
                favoritesCombo.getItems().setAll(favorites.keySet());
                stationNameField.clear();
                stationUrlField.clear();
            }
        });

        root.getChildren().addAll(
            nowPlayingLabel,
            titleLabel,
            artistLabel,
            playPauseButton,
            volumeBox,
            new Label("Select Station:"),
            favoritesCombo,
            new Separator(),
            new Label("Add New Station:"),
            stationNameField,
            stationUrlField,
            addButton
        );

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.mediaPlayer().audio().setVolume((int) volumeSlider.getValue());
            }
        });

        Scene scene = new Scene(root, 400, 500);
        primaryStage.setTitle("Radio Player");
        primaryStage.setScene(scene);
        primaryStage.show();

        playStation(DEFAULT_STATION, DEFAULT_STATION_NAME);
    }

    private void togglePlayPause() {
        if (isPlaying) {
            mediaPlayer.mediaPlayer().controls().pause();
            playPauseButton.setText("Play");
            isPlaying = false;
        } else {
            mediaPlayer.mediaPlayer().controls().play();
            playPauseButton.setText("Pause");
            isPlaying = true;
        }
    }

    private void playStation(String url, String name) {
        try {
            mediaPlayer.mediaPlayer().controls().stop();
            
            if (metadataReader != null) {
                metadataReader.stop();
            }
            
            if (name.equals(DEFAULT_STATION_NAME)) {
                metadataReader = new IcyMetadataReader(url, (title, artist) -> {
                    titleLabel.setText("Title: " + title);
                    artistLabel.setText("Artist: " + artist);
                });
                metadataReader.start();
            } else {
                titleLabel.setText("Title: ");
                artistLabel.setText("Artist: ");
            }
            
            nowPlayingLabel.setText("Connecting to: " + name);
            mediaPlayer.mediaPlayer().media().play(url);
            mediaPlayer.mediaPlayer().audio().setVolume((int) volumeSlider.getValue());
            
            isPlaying = true;
            playPauseButton.setText("Pause");
            nowPlayingLabel.setText("Now Playing: " + name);

            mediaPlayer.mediaPlayer().events().addMediaPlayerEventListener(new uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter() {
                @Override
                public void error(MediaPlayer mediaPlayer) {
                    Platform.runLater(() -> {
                        nowPlayingLabel.setText("Error playing station: " + name);
                        playPauseButton.setText("Play");
                        isPlaying = false;
                    });
                }
            });

        } catch (Exception e) {
            nowPlayingLabel.setText("Error loading station: " + e.getMessage());
            playPauseButton.setText("Play");
            isPlaying = false;
        }
    }

    private void loadFavorites() {
        try {
            File file = new File(FAVORITES_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\\|");
                        if (parts.length == 2) {
                            favorites.put(parts[0], parts[1]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            favorites = new HashMap<>();
        }
    }

    private void saveFavorites() {
        try {
            try (PrintWriter writer = new PrintWriter(new FileWriter(FAVORITES_FILE))) {
                for (Map.Entry<String, String> entry : favorites.entrySet()) {
                    writer.println(entry.getKey() + "|" + entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.mediaPlayer().controls().stop();
            mediaPlayer.release();
        }
        if (metadataReader != null) {
            metadataReader.stop();
        }
    }

    private void initializeVLC() {
        boolean found = new NativeDiscovery().discover();
        System.out.println("VLC native discovery: " + (found ? "successful" : "failed"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
