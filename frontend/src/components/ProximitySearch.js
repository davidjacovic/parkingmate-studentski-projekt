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
  iconSize: [40, 40],       // možeš menjati veličinu
  iconAnchor: [20, 40],     // gde se marker "kači" na mapu
  popupAnchor: [0, -40],
});


// Haversine formula to calculate distance in km
function getDistance(coord1, coord2) {
  const [lat1, lon1] = coord1;
  const [lat2, lon2] = coord2;
  const R = 6371; // Earth radius in km
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * (Math.PI / 180)) *
    Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) ** 2;
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function ProximitySearch() {
  const [userLocation, setUserLocation] = useState(null);
  const [parkingLocations, setParkingLocations] = useState([]);
  const [filtered, setFiltered] = useState([]);

  useEffect(() => {
    fetch('http://localhost:3002/parkingLocations')
      .then(res => res.json())
      .then(data => setParkingLocations(data))
      .catch(err => console.error('Greška pri učitavanju parkinga:', err));
  }, []);

  useEffect(() => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      pos => {
        const userCoords = [pos.coords.latitude, pos.coords.longitude];
        setUserLocation(userCoords);

        const filteredData = parkingLocations.filter(loc => {
          const dist = getDistance(userCoords, [loc.latitude, loc.longitude]);
          return dist <= 2; // 2 km radijus
        });

        setFiltered(filteredData);
      },
      err => console.error('Geolokacija greška:', err)
    );
  }, [parkingLocations]);

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
              key={loc.id}
              position={[loc.latitude, loc.longitude]}
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
