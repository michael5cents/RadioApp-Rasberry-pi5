package com.radio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;

import java.io.*;
import java.util.*;

public class App extends Application {
    private final String DEFAULT_STATION = "http://s8.myradiostream.com/15672/listen.mp3";
    private final String DEFAULT_STATION_NAME = "Popz Place Radio";
    private final String FAVORITES_FILE = "radio_favorites.json";
    private Map<String, RadioStation> favorites = new HashMap<>();
    private Label nowPlayingLabel;
    private Label titleLabel;
    private Label artistLabel;
    private Button playPauseButton;
    private Slider volumeSlider;
    private AudioPlayerComponent mediaPlayer;
    private boolean isPlaying = false;
    private IcyMetadataReader metadataReader;
    private ListView<RadioStation> searchResultsListView;
    private ObservableList<RadioStation> searchResultsList;
    private ListView<RadioStation> favoritesListView;
    private ObservableList<RadioStation> favoritesList;

    private static class RadioStation {
        String name;
        String url;
        String genre;

        RadioStation(String name, String url, String genre) {
            this.name = name;
            this.url = url;
            this.genre = genre;
        }

        @Override
        public String toString() {
            return name + " (" + genre + ")";
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            initializeVLC();
            mediaPlayer = new AudioPlayerComponent();
        } catch (Exception e) {
            System.err.println("Failed to initialize VLC: " + e.getMessage());
            return;
        }

        loadFavorites();
        favorites.putIfAbsent(DEFAULT_STATION_NAME, new RadioStation(DEFAULT_STATION_NAME, DEFAULT_STATION, "Pop"));
        saveFavorites();

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.TOP_CENTER);

        // Player controls section
        VBox playerControls = new VBox(5);
        playerControls.setAlignment(Pos.CENTER);
        
        nowPlayingLabel = new Label("Ready to Play");
        titleLabel = new Label("Title: ");
        artistLabel = new Label("Artist: ");
        
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

        playerControls.getChildren().addAll(
            nowPlayingLabel,
            titleLabel,
            artistLabel,
            playPauseButton,
            volumeBox
        );

        // Search section
        VBox searchSection = new VBox(5);
        searchSection.setAlignment(Pos.CENTER);

        Label searchLabel = new Label("Find Radio Stations");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        TextField genreField = new TextField();
        genreField.setPromptText("Enter genre (e.g., jazz, rock, classical)");
        
        Button searchButton = new Button("Search Radio Stations");
        ProgressIndicator searchProgress = new ProgressIndicator();
        searchProgress.setVisible(false);
        searchProgress.setMaxSize(20, 20);
        
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getChildren().addAll(searchButton, searchProgress);

        searchButton.setOnAction(e -> {
            String genre = genreField.getText().trim();
            searchProgress.setVisible(true);
            searchButton.setDisable(true);

            CompletableFuture.runAsync(() -> {
                try {
                    String apiUrl = "https://de1.api.radio-browser.info/json/stations/bytagexact/" + 
                                  URLEncoder.encode(genre.isEmpty() ? "pop" : genre, StandardCharsets.UTF_8.toString());
                    
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(apiUrl))
                            .header("User-Agent", "RadioPlayer/1.0")
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    JSONArray stations = new JSONArray(response.body());
                    
                    Set<String> addedNames = new HashSet<>();
                    List<RadioStation> uniqueStations = new ArrayList<>();
                    
                    for (int i = 0; i < stations.length() && uniqueStations.size() < 20; i++) {
                        var station = stations.getJSONObject(i);
                        String name = station.getString("name");
                        
                        if (!addedNames.contains(name)) {
                            String url = station.getString("url_resolved");
                            String stationGenre = station.getString("tags");
                            RadioStation newStation = new RadioStation(name, url, stationGenre);
                            uniqueStations.add(newStation);
                            addedNames.add(name);
                        }
                    }
                    
                    Platform.runLater(() -> {
                        searchResultsList.clear();
                        searchResultsList.addAll(uniqueStations);
                        searchProgress.setVisible(false);
                        searchButton.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        searchProgress.setVisible(false);
                        searchButton.setDisable(false);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Search Error");
                        alert.setHeaderText("Could not search for stations");
                        alert.setContentText("Please try again later: " + ex.getMessage());
                        alert.showAndWait();
                    });
                }
            });
        });

        // Search Results list
        Label searchResultsLabel = new Label("Search Results:");
        searchResultsLabel.setStyle("-fx-font-weight: bold");
        
        searchResultsList = FXCollections.observableArrayList();
        searchResultsListView = new ListView<>(searchResultsList);
        searchResultsListView.setPrefHeight(150);
        VBox.setVgrow(searchResultsListView, Priority.ALWAYS);

        searchResultsListView.setOnMouseClicked(e -> {
            RadioStation selected = searchResultsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                playStation(selected.url, selected.name);
            }
        });

        // Save to Favorites button
        Button saveToFavoritesButton = new Button("Save Selected to Favorites");
        saveToFavoritesButton.setOnAction(e -> {
            RadioStation selected = searchResultsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                favorites.put(selected.name, selected);
                saveFavorites();
                favoritesList.setAll(favorites.values());
            }
        });

        // Favorites list
        Label favoritesLabel = new Label("My Favorite Stations:");
        favoritesLabel.setStyle("-fx-font-weight: bold");
        
        favoritesList = FXCollections.observableArrayList(favorites.values());
        favoritesListView = new ListView<>(favoritesList);
        favoritesListView.setPrefHeight(150);
        VBox.setVgrow(favoritesListView, Priority.ALWAYS);

        favoritesListView.setOnMouseClicked(e -> {
            RadioStation selected = favoritesListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                playStation(selected.url, selected.name);
            }
        });

        searchSection.getChildren().addAll(
            searchLabel,
            genreField,
            searchBox,
            new Separator(),
            searchResultsLabel,
            searchResultsListView,
            saveToFavoritesButton,
            new Separator(),
            favoritesLabel,
            favoritesListView
        );

        // Add Quit button
        Button quitButton = new Button("Quit");
        quitButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            if (metadataReader != null) {
                metadataReader.stop();
            }
            Platform.exit();
        });
        
        root.getChildren().addAll(
            playerControls,
            new Separator(),
            searchSection,
            new Separator(),
            quitButton
        );

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.mediaPlayer().audio().setVolume((int) volumeSlider.getValue());
            }
        });

        Scene scene = new Scene(root, 400, 800);
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
                    Platform.runLater(() -> {
                        titleLabel.setText("Title: " + title);
                        artistLabel.setText("Artist: " + artist);
                    });
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
            Platform.runLater(() -> {
                nowPlayingLabel.setText("Error loading station: " + e.getMessage());
                playPauseButton.setText("Play");
                isPlaying = false;
            });
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
                        if (parts.length == 3) {
                            favorites.put(parts[0], new RadioStation(parts[0], parts[1], parts[2]));
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
                for (RadioStation station : favorites.values()) {
                    writer.println(station.name + "|" + station.url + "|" + station.genre);
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
        // Set VLC library path for Linux
        System.setProperty("jna.library.path", "/usr/lib:/usr/lib/x86_64-linux-gnu:/usr/lib/aarch64-linux-gnu:/usr/local/lib");
        boolean found = new NativeDiscovery().discover();
        if (!found) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("VLC Not Found");
                alert.setHeaderText("VLC Media Player is required");
                alert.setContentText("Please install VLC Media Player to use this application.\n" +
                                   "Visit: https://www.videolan.org/vlc/");
                alert.showAndWait();
                Platform.exit();
            });
            throw new RuntimeException("VLC not found");
        }
        System.out.println("VLC native discovery: successful");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
