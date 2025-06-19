import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Rectangle, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { Link } from 'react-router-dom';
import L from 'leaflet';

const GRID_SIZE = 0.01; // oko 1.1km x 1.1km

function getGridBounds(locations) {
  const grid = {};

  locations.forEach(loc => {
    if (!loc.location || !Array.isArray(loc.location.coordinates)) return;

    const [lngRaw, latRaw] = loc.location.coordinates;
    if (typeof latRaw !== 'number' || typeof lngRaw !== 'number' || isNaN(latRaw) || isNaN(lngRaw)) return;

    const lat = Math.floor(latRaw / GRID_SIZE) * GRID_SIZE;
    const lng = Math.floor(lngRaw / GRID_SIZE) * GRID_SIZE;
    const key = `${lat},${lng}`;

    if (!grid[key]) grid[key] = 0;
    grid[key] += 1;
  });

  return Object.entries(grid).map(([key, count]) => {
    const [lat, lng] = key.split(',').map(parseFloat);
    const bounds = [
      [lat, lng],
      [lat + GRID_SIZE, lng + GRID_SIZE]
    ];
    return { bounds, count };
  });
}

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
        transition: 'transform 0.2s, background-color 0.3s'
      });

      div.onclick = async () => {
        div.style.transform = 'scale(0.9)';
        setLoading(true);
        try {
          await onRefresh(); // čekamo da završi osvežavanje
        } finally {
          setTimeout(() => {
            div.style.transform = 'scale(1)';
            setLoading(false);
          }, 300); // efekat posle 0.3s
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

const CoverageAnalysis = () => {
  const [locations, setLocations] = useState([]);
  const [userLocation, setUserLocation] = useState(null);

  const fetchLocations = () => {
    fetch('http://localhost:3002/parkingLocations')
      .then(res => res.json())
      .then(data => setLocations(data))
      .catch(err => console.error(err));
  };

  useEffect(() => {
    fetchLocations();
  }, []);

  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        pos => {
          setUserLocation([pos.coords.latitude, pos.coords.longitude]);
        },
        err => {
          console.warn('Geolokacija greška:', err);
        }
      );
    }
  }, []);

  const grid = getGridBounds(locations);

  return (
    <div style={{ height: '100vh' }}>
      {userLocation ? (
        <MapContainer center={userLocation} zoom={15} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            attribution='&copy; OpenStreetMap'
            url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
          />
          <CustomRefreshControl onRefresh={fetchLocations} />

          {locations.map(loc => {
            if (!loc.location || !Array.isArray(loc.location.coordinates)) return null;

            const [lng, lat] = loc.location.coordinates;
            if (typeof lat !== 'number' || typeof lng !== 'number' || isNaN(lat) || isNaN(lng)) return null;

            return (
              <Marker key={loc._id} position={[lat, lng]}>
                <Popup>
                  <div>
                    <strong>{loc.name}</strong><br />
                    {loc.address}<br />
                    <Link to={`/location/${loc._id}`}>Detalji</Link>
                  </div>
                </Popup>
              </Marker>
            );
          })}

          {grid.map((cell, index) => (
            <Rectangle
              key={index}
              bounds={cell.bounds}
              pathOptions={{
                color: 'red',
                weight: 1,
                fillOpacity: Math.min(cell.count / 5, 0.4)
              }}
            />
          ))}
        </MapContainer>
      ) : (
        <p>Učitavanje vaše lokacije...</p>
      )}
    </div>
  );
};

export default CoverageAnalysis;
