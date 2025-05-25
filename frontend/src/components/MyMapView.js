// src/components/MapView.jsx
import React from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Link } from 'react-router-dom';
import userIconImg from '../assets/man-location.png';


// Popravi ikonice da se pravilno prikazuju
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

const userIcon = new L.Icon({
  iconUrl: userIconImg,
  iconSize: [40, 40],
  iconAnchor: [20, 40],
  popupAnchor: [0, -40],
});

const MapView = ({ userLocation, parkingLocations, nearestId }) => {
  const defaultCenter = userLocation
    ? [userLocation[1], userLocation[0]]
    : [46.0569, 14.5058]; // Ljubljana fallback

  return (
    <div style={{ height: '400px', width: '100%', margin: '2rem auto' }}>
      <MapContainer center={defaultCenter} zoom={15} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; OpenStreetMap'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {userLocation && (
          <Marker position={[userLocation[1], userLocation[0]]} icon={userIcon}>
            <Popup>Vaša lokacija</Popup>
          </Marker>
        )}


        {parkingLocations.map((loc) => (
          <Marker
            key={loc._id}
            position={[loc.latitude, loc.longitude]}
            icon={
              loc._id === nearestId
                ? new L.Icon({
                  iconUrl: 'https://maps.google.com/mapfiles/ms/icons/green-dot.png',
                  iconSize: [25, 41],
                  iconAnchor: [12, 41],
                  popupAnchor: [0, -41],
                })
                : new L.Icon.Default()
            }
          >
            <Popup>
              <div>
                <strong>{loc.name}</strong><br />
                {loc.address}<br />
                <Link to={`/location/${loc._id}`}>Detalji</Link>
              </div>
            </Popup>

          </Marker>
        ))}
      </MapContainer>
    </div>
  );
};

export default MapView;
