import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
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

function CustomRefreshControl({ onRefresh }) {
  const map = useMap();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const control = L.control({ position: 'topright' });

    control.onAdd = function () {
      const div = L.DomUtil.create('div', 'leaflet-bar leaflet-control leaflet-control-custom');
      div.innerHTML = '🔄';
      div.title = 'Osveži lokacije';

      Object.assign(div.style, {
        backgroundColor: 'white',
        width: '34px',
        height: '34px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
        fontSize: '20px',
        boxShadow: '0 1px 4px rgba(0,0,0,0.4)',
        transition: 'transform 0.2s, background-color 0.3s',
      });

      div.onclick = async () => {
        div.style.transform = 'scale(0.9)';
        setLoading(true);
        try {
          await onRefresh();
        } finally {
          setTimeout(() => {
            div.style.transform = 'scale(1)';
            setLoading(false);
          }, 300);
        }
      };

      return div;
    };

    control.addTo(map);
    return () => map.removeControl(control);
  }, [map, onRefresh]);

  useEffect(() => {
    const btn = document.querySelector('.leaflet-control-custom');
    if (btn) {
      btn.innerHTML = loading ? '⏳' : '🔄';
      btn.title = loading ? 'Učitavanje...' : 'Osveži lokacije';
    }
  }, [loading]);

  return null;
}

function ProximitySearch() {
  const [userLocation, setUserLocation] = useState(null);
  const [filtered, setFiltered] = useState([]);
  const [error, setError] = useState('');

  const fetchNearby = async () => {
    if (!userLocation) {
      setError('Geolokacija nije dostupna.');
      return;
    }
    setError('');
    const [lat, lng] = userLocation;
    const radius = 2000;

    try {
      const res = await fetch(`http://localhost:3002/parkingLocations/nearby/search?lat=${lat}&lng=${lng}&radius=${radius}`);
      if (!res.ok) throw new Error('Neuspešno učitavanje podataka');
      const data = await res.json();
      setFiltered(data);
    } catch (err) {
      console.error('Greška u geoprostorskoj pretrazi:', err);
      setError('Greška pri učitavanju parking lokacija.');
    }
  };

  useEffect(() => {
    if (!navigator.geolocation) {
      setError('Geolokacija nije podržana u vašem pretraživaču.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      pos => setUserLocation([pos.coords.latitude, pos.coords.longitude]),
      err => {
        console.error('Geolokacija greška:', err);
        setError('Dozvolite pristup lokaciji.');
      }
    );
  }, []);

  useEffect(() => {
    if (userLocation) fetchNearby();
  }, [userLocation]);

  return (
    <div style={{ height: '100vh', width: '100%' }}>
      {error && <p style={{ color: 'red', position: 'absolute', top: 50, right: 10, zIndex: 1000, background: 'white', padding: '4px 8px', borderRadius: '4px' }}>{error}</p>}

      {userLocation ? (
        <MapContainer center={userLocation} zoom={15} style={{ height: '100%', width: '100%' }}>
          <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          <CustomRefreshControl onRefresh={fetchNearby} />
          <Marker position={userLocation} icon={userIcon}>
            <Popup>Vaša lokacija</Popup>
          </Marker>
          {filtered.map(loc => (
            <Marker key={loc._id} position={[loc.location.coordinates[1], loc.location.coordinates[0]]}>
              <Popup>
                <div>
                  <strong>{loc.name}</strong><br />
                  {loc.address}<br />
                  <a href={`/location/${loc._id}`}>Detalji</a>
                </div>
              </Popup>
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
