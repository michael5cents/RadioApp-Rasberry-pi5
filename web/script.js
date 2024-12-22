// JavaScript for the web version of the radio app

// Array to hold radio stations
let radioStations = [];

// Function to load radio stations from JSON file
function loadStations() {
    fetch('../radio_favorites.json')
        .then(response => response.json())
        .then(data => {
            radioStations = data.stations;
            displayStations();
        });
}

// Function to display radio stations
function displayStations() {
    const stationList = document.getElementById('station-list');
    stationList.innerHTML = '';
    radioStations.forEach(station => {
        const li = document.createElement('li');
        li.textContent = station.name;
        li.addEventListener('click', () => playStation(station));
        stationList.appendChild(li);
    });
}

// Function to play a radio station
function playStation(station) {
    const audio = document.getElementById('audio-player');
    audio.src = station.url;
    audio.play();
    updateNowPlaying(station);
}

function updateNowPlaying(station) {
    const nowPlaying = document.getElementById('now-playing');
    nowPlaying.textContent = `Now Playing: ${station.name}`;
}

// Function to search for stations
function searchStations() {
    const searchInput = document.getElementById('search-input').value.toLowerCase();
    const filteredStations = radioStations.filter(station => 
        station.name.toLowerCase().includes(searchInput) || 
        station.genre.toLowerCase().includes(searchInput)
    );
    displayStations(filteredStations);
}

// Event listener for search input
document.getElementById('search-input').addEventListener('input', searchStations);

// Load stations when the page loads
window.onload = loadStations;
