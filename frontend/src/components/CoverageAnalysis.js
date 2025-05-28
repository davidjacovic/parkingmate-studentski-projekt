import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Rectangle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { Link } from 'react-router-dom';


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

const CoverageAnalysis = () => {
  const [locations, setLocations] = useState([]);
  const [userLocation, setUserLocation] = useState(null);

  useEffect(() => {
    fetch('http://localhost:3002/parkingLocations')
      .then(res => res.json())
      .then(data => setLocations(data))
      .catch(err => console.error(err));
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
                <a href={`/location/${loc._id}`}>Detalji</a>

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
              fillOpacity: Math.min(cell.count / 5, 0.4) // više parkinga = tamnije
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
