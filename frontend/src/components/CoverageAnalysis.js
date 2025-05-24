// src/components/CoverageAnalysis.jsx
import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Rectangle } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const GRID_SIZE = 0.01; // oko 1.1km x 1.1km

function getGridBounds(locations) {
  const grid = {};

  locations.forEach(loc => {
    const lat = Math.floor(loc.latitude / GRID_SIZE) * GRID_SIZE;
    const lng = Math.floor(loc.longitude / GRID_SIZE) * GRID_SIZE;
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

  useEffect(() => {
    fetch('http://localhost:3002/parkingLocations')
      .then(res => res.json())
      .then(data => setLocations(data))
      .catch(err => console.error(err));
  }, []);

  const grid = getGridBounds(locations);

  return (
    <div style={{ height: '100vh' }}>
      <MapContainer center={[45.25, 19.85]} zoom={13} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; OpenStreetMap'
          url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
        />

        {locations.map(loc => (
          <Marker key={loc._id} position={[loc.latitude, loc.longitude]}>
            <Popup>{loc.name}</Popup>
          </Marker>
        ))}

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
    </div>
  );
};

export default CoverageAnalysis;
