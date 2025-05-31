import React, { useContext, useEffect, useState } from 'react';
import { UserContext } from '../userContext';
import { Link } from 'react-router-dom';
import { findNearestParking } from '../utils/geoUtils';
import MapView from './MyMapView';

function Homepage() {
  const { user } = useContext(UserContext);
  const [locations, setLocations] = useState([]);
  const [error, setError] = useState('');
  const [userLocation, setUserLocation] = useState(null);
  const [nearestParking, setNearestParking] = useState(null);
  const [loading, setLoading] = useState(false);

  // Filter state
  const [filters, setFilters] = useState({
    regular: false,
    invalid: false,
    electric: false,
    bus: false,
  });

  // Update filters on checkbox toggle
  function toggleFilter(type) {
    setFilters(prev => ({ ...prev, [type]: !prev[type] }));
  }

  // Fetch locations with filters applied
  const fetchLocations = () => {
    setLoading(true);
    setError('');

    // Build query params from filters
    const params = new URLSearchParams();
    if (filters.regular) params.append('regular', 'true');
    if (filters.invalid) params.append('invalid', 'true');
    if (filters.electric) params.append('electric', 'true');
    if (filters.bus) params.append('bus', 'true');

    const url = `http://localhost:3002/parkingLocations/parking-filter?${params.toString()}`;

    fetch(url)
      .then(res => {
        if (!res.ok) throw new Error('Neuspešno učitavanje lokacija');
        return res.json();
      })
      .then(data => setLocations(data))
      .catch(err => {
        console.error(err);
        setError('Greška pri učitavanju lokacija.');
      })
      .finally(() => setLoading(false));
  };

  // Initial fetch and whenever filters change
  useEffect(() => {
    fetchLocations();
  }, [filters]);

  useEffect(() => {
    if (!navigator.geolocation) {
      setError('Geolokacija nije podržana u vašem pretraživaču.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      position => {
        setUserLocation([position.coords.longitude, position.coords.latitude]);
      },
      () => {
        setError('Dozvolite pristup lokaciji da biste videli najbliže parking mesto.');
      }
    );
  }, []);

  useEffect(() => {
    if (userLocation && locations.length > 0) {
      const spots = locations
        .map(loc => ({
          id: loc._id,
          coordinates: loc?.location?.coordinates,
        }))
        .filter(p =>
          Array.isArray(p.coordinates) &&
          p.coordinates.length === 2 &&
          p.coordinates.every(c => typeof c === 'number' && !isNaN(c))
        );

      if (spots.length === 0) {
        setError('Nema validnih parking lokacija sa koordinatama.');
        return;
      }

      try {
        const nearestId = findNearestParking(userLocation, spots);
        const nearestLoc = locations.find(loc => loc._id === nearestId);
        setNearestParking(nearestLoc);
      } catch (err) {
        console.error('Greška u findNearestParking:', err);
        setError('Greška pri pronalaženju najbližeg parking mesta.');
      }
    }
  }, [userLocation, locations]);

  return (
    <div>
      <h1>HOME PAGE</h1>

      {/* Filter checkboxes */}
      <div style={{ marginBottom: '1rem' }}>
        <label>
          <input
            type="checkbox"
            checked={filters.regular}
            onChange={() => toggleFilter('regular')}
          />{' '}
          Regular Spots
        </label>{' '}
        <label>
          <input
            type="checkbox"
            checked={filters.invalid}
            onChange={() => toggleFilter('invalid')}
          />{' '}
          Invalid Spots
        </label>{' '}
        <label>
          <input
            type="checkbox"
            checked={filters.electric}
            onChange={() => toggleFilter('electric')}
          />{' '}
          Electric Spots
        </label>{' '}
        <label>
          <input
            type="checkbox"
            checked={filters.bus}
            onChange={() => toggleFilter('bus')}
          />{' '}
          Bus Spots
        </label>
      </div>

      {loading && <p>Loading locations...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {nearestParking && (
        <>
          <MapView
            userLocation={userLocation}
            parkingLocations={locations}
            nearestId={nearestParking._id}
            onRefresh={fetchLocations}
          />
          <div>
            <h2>Najbliže parking mesto</h2>
            <h4>{nearestParking.name}</h4>
            <p>Adresa: {nearestParking.address}</p>
            <a
              href={`https://www.google.com/maps?q=${nearestParking.location.coordinates[1]},${nearestParking.location.coordinates[0]}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              Otvori u Google mapama
            </a>
          </div>
        </>
      )}

      <div>
        <h2>Spisak ulica i parking mesta</h2>
        {locations.length === 0 ? (
          <p>Nema dostupnih parking mesta za izabrane filtere.</p>
        ) : (
          locations.map(loc => (
            <div key={loc._id} style={{ marginBottom: '1rem' }}>
              <div>
                <h4>
                  <Link to={`/location/${loc._id}`}>
                    {loc.name}
                  </Link>
                </h4>
                <p>Adresa: {loc.address}</p>
                <p>
                  Regularna mesta: Slobodnih: {loc.available_regular_spots} / Ukupno: {loc.total_regular_spots} <br />
                  Invalidska mesta: Slobodnih: {loc.available_invalid_spots} / Ukupno: {loc.total_invalid_spots} <br />
                  Električna mesta: Slobodnih: {loc.available_electric_spots} / Ukupno: {loc.total_electric_spots} <br />
                  Autobuska mesta: Slobodnih: {loc.available_bus_spots} / Ukupno: {loc.total_bus_spots}
                </p>
              </div>
              <div>
                <a
                  href={`https://www.google.com/maps?q=${loc.location.coordinates[1]},${loc.location.coordinates[0]}`}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  📍
                </a>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default Homepage;
