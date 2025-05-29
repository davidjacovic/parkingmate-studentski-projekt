import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Lokalne ikonice
import userIconImg from '../assets/man-location.png';
import greenIcon from '../assets/marker-icon-2x-green.png';
import yellowIcon from '../assets/marker-icon-2x-yellow.png';
import redIcon from '../assets/marker-icon-2x-red.png';
import blackIcon from '../assets/marker-icon-2x-black.png';

// Popunjenost => boja
const getColor = (occupancy) => {
  if (occupancy === 100) return 'black';
  if (occupancy >= 75) return 'red';
  if (occupancy >= 40) return 'yellow';
  return 'green';
};

// Ikona korisnika
const userIcon = new L.Icon({
  iconUrl: userIconImg,
  iconSize: [40, 40],
  iconAnchor: [20, 40],
  popupAnchor: [0, -40],
});

// Mapa boja => ikonice
const iconMap = {
  green: greenIcon,
  yellow: yellowIcon,
  red: redIcon,
  black: blackIcon,
};

// Kreiranje ikonice parkinga
const createIcon = (color) =>
  new L.Icon({
    iconUrl: iconMap[color],
    iconSize: [30, 45],
    iconAnchor: [15, 45],
    popupAnchor: [0, -45],
  });

// ✅ Ažurirana kontrola za osvežavanje sa efektom i loadingom
function CustomRefreshControl({ onRefresh }) {
  const map = useMap();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const control = L.control({ position: 'topright' });

    control.onAdd = function () {
      const div = L.DomUtil.create('div', 'leaflet-bar leaflet-control leaflet-control-custom');
      div.innerHTML = '🔄';
      div.title = 'Osveži parking podatke';

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
        transition: 'transform 0.2s, background-color 0.3s'
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
      btn.title = loading ? 'Učitavanje...' : 'Osveži parking podatke';
    }
  }, [loading]);

  return null;
}

function ParkingOccupancyMap() {
  const [data, setData] = useState([]);
  const [userLocation, setUserLocation] = useState(null);

  // Učitavanje parking podataka
  const fetchData = async () => {
    try {
      const res = await fetch('http://localhost:3002/parkingLocations/occupancy/status');
      const data = await res.json();
      setData(data);
    } catch (error) {
      console.error(error);
    }
  };

  // Učitavanje prvi put
  useEffect(() => {
    fetchData();
  }, []);

  // Geolokacija korisnika
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
      <h2 style={{ margin: '10px' }}>Pregled popunjenosti parkinga</h2>

      {userLocation ? (
        <MapContainer center={userLocation} zoom={15} style={{ height: '90%', width: '100%' }}>
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          <CustomRefreshControl onRefresh={fetchData} />
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
