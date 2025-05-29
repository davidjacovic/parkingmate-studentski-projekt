import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Ikonice
import userIconImg from '../assets/man-location.png';
import greenIcon from '../assets/marker-icon-2x-green.png';
import yellowIcon from '../assets/marker-icon-2x-yellow.png';
import redIcon from '../assets/marker-icon-2x-red.png';
import blackIcon from '../assets/marker-icon-2x-black.png';

// Funkcija za boju na osnovu popunjenosti
const getColor = (occupancy) => {
  if (occupancy === 100) return 'black';
  if (occupancy >= 75) return 'red';
  if (occupancy >= 40) return 'yellow';
  return 'green';
};

// Ikonica korisnika
const userIcon = new L.Icon({
  iconUrl: userIconImg,
  iconSize: [40, 40],
  iconAnchor: [20, 40],
  popupAnchor: [0, -40],
});

// Mapa boja -> lokalne ikonice
const iconMap = {
  green: greenIcon,
  yellow: yellowIcon,
  red: redIcon,
  black: blackIcon,
};

// Funkcija za kreiranje ikonice
const createIcon = (color) =>
  new L.Icon({
    iconUrl: iconMap[color],
    iconSize: [30, 45],
    iconAnchor: [15, 45],
    popupAnchor: [0, -45],
  });

function ParkingOccupancyMap() {
  const [data, setData] = useState([]);
  const [userLocation, setUserLocation] = useState(null);

  useEffect(() => {
    fetch('http://localhost:3002/parkingLocations/occupancy/status')
      .then(res => res.json())
      .then(data => {
        console.log("Parking data:", data);
        setData(data);
      })
      .catch(console.error);
  }, []);

  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        pos => setUserLocation([pos.coords.latitude, pos.coords.longitude]),
        err => console.warn('Greška pri geolokaciji:', err)
      );
    }
  }, []);

  return (
    <div style={{ height: '100vh', width: '100%' }}>
      <h2>Pregled popunjenosti parkinga</h2>
      {userLocation ? (
        <MapContainer center={userLocation} zoom={15} style={{ height: '90%', width: '100%' }}>
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          <Marker position={userLocation} icon={userIcon}>
            <Popup>Vaša lokacija</Popup>
          </Marker>
          {data.map(loc => {
            if (!loc.location?.coordinates || loc.location.coordinates.length !== 2) return null;

            const [lng, lat] = loc.location.coordinates;
            const color = getColor(loc.occupancy);

            return (
              <Marker
                key={loc._id}
                position={[lat, lng]}
                icon={createIcon(color)}
              >
                <Popup>
                  <strong>{loc.name}</strong><br />
                  {loc.address}<br />
                  Popunjenost: {loc.occupancy}%
                </Popup>
              </Marker>
            );
          })}
        </MapContainer>
      ) : (
        <p>Učitavanje vaše lokacije...</p>
      )}
    </div>
  );
}

export default ParkingOccupancyMap;
