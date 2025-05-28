import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import userIconImg from '../assets/man-location.png';

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

function ProximitySearch() {
  const [userLocation, setUserLocation] = useState(null);
  const [filtered, setFiltered] = useState([]);

  useEffect(() => {
    if (!navigator.geolocation) return;

    navigator.geolocation.getCurrentPosition(
      pos => {
        const userCoords = [pos.coords.latitude, pos.coords.longitude];
        setUserLocation(userCoords);

        const [lat, lng] = userCoords;
        const radius = 2000; // 2km

        fetch(`http://localhost:3002/parkingLocations/nearby/search?lat=${lat}&lng=${lng}&radius=${radius}`)
          .then(res => res.json())
          .then(data => setFiltered(data))
          .catch(err => console.error('Greška u geoprostorskoj pretrazi:', err));
      },
      err => console.error('Geolokacija greška:', err)
    );
  }, []);

  return (
    <div style={{ height: '100vh', width: '100%' }}>
      {userLocation ? (
        <MapContainer center={userLocation} zoom={15} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            attribution='&copy; OpenStreetMap contributors'
            url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
          />
          <Marker position={userLocation} icon={userIcon}>
            <Popup>Vaša lokacija</Popup>
          </Marker>
          {filtered.map(loc => (
            <Marker
              key={loc._id}
              position={[loc.location.coordinates[1], loc.location.coordinates[0]]}
            >
              <Popup>{loc.name}<br />{loc.address}</Popup>
            </Marker>
          ))}
        </MapContainer>
      ) : (
        <p>Učitavanje vaše lokacije...</p>
      )}
    </div>
  );
}

export default ProximitySearch;
