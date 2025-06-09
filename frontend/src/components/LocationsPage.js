import React from 'react';
import { Routes, Route, Link, Navigate } from 'react-router-dom';
import ProximitySearch from './ProximitySearch';
import CoverageAnalysis from './CoverageAnalysis';
import ParkingOccupancyMap from './ParkingOccupancyMap';


function LocationsPage() {
  return (
    <div style={{ maxWidth: 900, margin: '1rem auto', padding: '0 1rem' }}>
      <header style={{ marginBottom: '1rem' }}>
        <h1>Locations</h1>
        <nav style={{ display: 'flex', gap: '1rem' }}>
          <Link to="/locations/nearby" style={{ textDecoration: 'none', color: 'blue' }}>
            Parkirišča v bližini
          </Link>
          <Link to="/locations/coverage" style={{ textDecoration: 'none', color: 'blue' }}>
            Analiza pokritosti
          </Link>
          <Link to="/locations/occupancy" style={{ textDecoration: 'none', color: 'blue' }}>
            Status zasedenosti
          </Link>
        </nav>

      </header>

      <Routes>
        <Route index element={<Navigate to="nearby" />} />
        <Route path="nearby" element={<ProximitySearch />} />
        <Route path="coverage" element={<CoverageAnalysis />} />
        <Route path="occupancy" element={<ParkingOccupancyMap />} />
        <Route path="*" element={<p>Izaberite opciju iz menija.</p>} />
      </Routes>

    </div>
  );
}

export default LocationsPage;
