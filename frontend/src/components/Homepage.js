import React, { useContext, useEffect, useState } from 'react';
import LocationsTable from './LocationsTable';
import { UserContext } from '../userContext';
import { Link, Routes, Route } from 'react-router-dom';
import { findNearestParking } from '../utils/geoUtils';
import MapView from './MyMapView';

import ProximitySearch from './ProximitySearch';
import CoverageAnalysis from './CoverageAnalysis';
import ParkingOccupancyMap from './ParkingOccupancyMap';
import LocationItem from './LocationItem';  // ili putanja do fajla gde je komponenta
import AddCommentForm from './AddCommentForm'; // ako ti je potrebna direktno ovde


function Homepage() {
  const { user } = useContext(UserContext);
  const [locations, setLocations] = useState([]);
  const [error, setError] = useState('');
  const [userLocation, setUserLocation] = useState(null);
  const [nearestParking, setNearestParking] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);

  const [filters, setFilters] = useState({
    regular: false,
    invalid: false,
    electric: false,
    bus: false,
  });

  function toggleFilter(type) {
    setFilters(prev => ({ ...prev, [type]: !prev[type] }));
  }

  const fetchLocations = () => {
    setLoading(true);
    setError('');

    const params = new URLSearchParams();
    if (filters.regular) params.append('regular', 'true');
    if (filters.invalid) params.append('invalid', 'true');
    if (filters.electric) params.append('electric', 'true');
    if (filters.bus) params.append('bus', 'true');

    const url = `http://localhost:3002/parkingLocations/parking-filter?${params.toString()}`;

    fetch(url)
      .then(res => {
        if (!res.ok) throw new Error('Neuspešno nalaganje lokacij');
        return res.json();
      })
      .then(data => setLocations(data))
      .catch(err => {
        console.error(err);
        setError('Napaka pri nalaganju lokacij.');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchLocations();
  }, [filters]);

  useEffect(() => {
    if (!navigator.geolocation) {
      setError('Geolokacija ni podprta v vašem brskalniku.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      position => {
        setUserLocation([position.coords.longitude, position.coords.latitude]);
      },
      () => {
        setError('Dovolite dostop do lokacije, da vidite najbližje parkirišče.');
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
        setError('Ni veljavnih parkirnih lokacij s koordinatami.');
        return;
      }

      try {
        const nearestId = findNearestParking(userLocation, spots);
        const nearestLoc = locations.find(loc => loc._id === nearestId);
        setNearestParking(nearestLoc);
      } catch (err) {
        console.error('Napaka v findNearestParking:', err);
        setError('Napaka pri iskanju najbližjega parkirnega mesta.');
      }
    }
  }, [userLocation, locations]);

  return (
    <>
      <div className="d-flex flex-map-info">
        <div className="map-wrapper">
          <div style={{ width: '100vw', height: '450px' }}>
            <MapView
              userLocation={userLocation}
              parkingLocations={locations}
              nearestId={nearestParking?._id}
              onRefresh={fetchLocations}
              style={{ width: '100%', height: '100%' }}
            />
          </div>
        </div>

        {/* Desni sidebar */}
        <aside className="nearest-parking-info">
          {/* Filtri */}
          <section className="filters-section mb-4">
            <h2 className="h5 mb-3">Filtri</h2>
            <div className="filters d-flex flex-column">
              {[
                ['regular', 'Redna parkirna mesta'],
                ['invalid', 'Parkirna mesta za invalide'],
                ['electric', 'Električna parkirna mesta'],
                ['bus', 'Parkirna mesta za avtobuse'],
              ].map(([key, label]) => (
                <div className="form-check mb-2" key={key}>
                  <input
                    className="form-check-input"
                    type="checkbox"
                    checked={filters[key]}
                    id={key}
                    onChange={() => toggleFilter(key)}
                  />
                  <label className="form-check-label" htmlFor={key}>
                    {label}
                  </label>
                </div>
              ))}
            </div>
            <button
              className="btn btn-outline-primary btn-sm mt-2"
              onClick={() =>
                setFilters({ regular: false, invalid: false, electric: false, bus: false })
              }
            >
              Ponastavi filtre
            </button>
            <p className="mt-3 text-muted">
              Prikazano <strong>{locations.length}</strong> rezultatov
            </p>
          </section>

          {/* Najbližje parkirno mesto */}
          <section className="nearest-section mt-4">
            <h2 className="h5">Najbližje parkirno mesto</h2>
            {nearestParking ? (
              <div className="card mt-2">
                <div className="card-body">
                  <h5 className="card-title">{nearestParking.name}</h5>
                  <p className="card-text">Naslov: {nearestParking.address}</p>
                  <a
                    href={`https://www.google.com/maps?q=${nearestParking.location.coordinates[1]},${nearestParking.location.coordinates[0]}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn btn-sm btn-outline-secondary"
                  >
                    Odpri v Google Zemljevidih
                  </a>
                </div>
              </div>
            ) : (
              <p>Ni podatkov o najbližjem parkirnem mestu.</p>
            )}
          </section>
        </aside>
      </div>

      {/* Povezave pod zemljevidom */}
      <nav className="below-map-nav container-centered">
        <Link to="/locations/nearby">Parkirišča v bližini</Link>
        <Link to="/locations/coverage">Analiza pokritosti</Link>
        <Link to="/locations/occupancy">Status zasedenosti</Link>
      </nav>

      {/* Glavni del */}
      <div className="container-centered">
        {loading && <p>Nalaganje lokacij...</p>}
        {error && <p className="error-text">{error}</p>}

        {user?.user_type === 'admin' && (
          <>
            <button
              className="admin-add-btn"
              onClick={() => setShowAddForm(prev => !prev)}
            >
              {showAddForm ? 'Zapri obrazec za dodajanje' : 'Dodaj novo lokacijo'}
            </button>
            {showAddForm && (
              <AddParkingLocation
                onAdded={(newParking) => {
                  setLocations(prev => [newParking, ...prev]);
                  setShowAddForm(false);
                }}
              />
            )}
          </>
        )}
        <div>
          {locations.length === 0 ? (
            <p>Ni razpoložljivih parkirnih mest za izbrane filtre.</p>
          ) : (
            // Umesto da mapiraš na LocationItem, koristi tabelu
            <LocationsTable locations={locations} user={user} />
          )}
        </div>
        
      </div>
    </>
  );
}
function AddParkingLocation({ onAdded }) {
  const { user } = useContext(UserContext);

  const [form, setForm] = useState({
    name: '',
    address: '',
    latitude: '',
    longitude: '',
    total_regular_spots: '',
    total_invalid_spots: '',
    total_electric_spots: '',
    total_bus_spots: '',
    available_regular_spots: '',
    available_invalid_spots: '',
    available_electric_spots: '',
    available_bus_spots: '',
    description: '',
  });

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (!user || user.user_type !== 'admin') {
    return <p>Nemate dozvolu da dodajete parking lokacije.</p>;
  }

  function handleChange(e) {
    const { name, value, type } = e.target;
    if (type === 'number' && value !== '' && (!/^\d*$/.test(value) || Number(value) < 0)) return;
    setForm(prev => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    const lat = parseFloat(form.latitude);
    const lng = parseFloat(form.longitude);
    if (isNaN(lat) || lat < -90 || lat > 90) return setError('Latitude mora biti između -90 i 90.');
    if (isNaN(lng) || lng < -180 || lng > 180) return setError('Longitude mora biti između -180 i 180.');

    const toNum = (val) => Number(val || 0);
    const payload = {
      name: form.name.trim(),
      address: form.address.trim(),
      description: form.description.trim(),
      location: { type: 'Point', coordinates: [lng, lat] },
      total_regular_spots: toNum(form.total_regular_spots),
      total_invalid_spots: toNum(form.total_invalid_spots),
      total_electric_spots: toNum(form.total_electric_spots),
      total_bus_spots: toNum(form.total_bus_spots),
      available_regular_spots: toNum(form.available_regular_spots),
      available_invalid_spots: toNum(form.available_invalid_spots),
      available_electric_spots: toNum(form.available_electric_spots),
      available_bus_spots: toNum(form.available_bus_spots),
    };

    // Validacija dostupnih mesta
    const mismatch = [
      ['regular', 'Ukupno regular', 'Dostupno regular'],
      ['invalid', 'Ukupno invalidska', 'Dostupno invalidska'],
      ['electric', 'Ukupno električna', 'Dostupno električna'],
      ['bus', 'Ukupno autobuska', 'Dostupno autobuska'],
    ].find(([type]) => payload[`available_${type}_spots`] > payload[`total_${type}_spots`]);

    if (mismatch) return setError(`${mismatch[2]} mesta ne mogu biti veća od ${mismatch[1].toLowerCase()}.`);

    try {
      setLoading(true);
      const token = user?.token || localStorage.getItem('token');
      if (!token) throw new Error('Niste prijavljeni ili je sesija istekla.');

      const res = await fetch('http://localhost:3002/parkingLocations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify(payload),
      });

      if (!res.ok) throw new Error((await res.json()).message || 'Greška prilikom dodavanja.');

      const newParking = await res.json();
      onAdded(newParking);
      setForm({
        name: '', address: '', latitude: '', longitude: '',
        total_regular_spots: '', total_invalid_spots: '', total_electric_spots: '', total_bus_spots: '',
        available_regular_spots: '', available_invalid_spots: '', available_electric_spots: '', available_bus_spots: '',
        description: '',
      });
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
  <form onSubmit={handleSubmit} style={{
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '1rem',
    maxWidth: '800px',
    padding: '1rem',
    backgroundColor: '#f9f9f9',
    borderRadius: '10px',
    border: '1px solid #ddd',
  }}>
    <h3 style={{ gridColumn: '1 / -1' }}>Dodaj novo parkirno lokacijo</h3>
    {error && <p style={{ color: 'red', gridColumn: '1 / -1' }}>{error}</p>}

    <div>
      <label>Ime lokacije</label>
      <input name="name" value={form.name} onChange={handleChange} required />
    </div>
    <div>
      <label>Naslov</label>
      <input name="address" value={form.address} onChange={handleChange} required />
    </div>

    <div>
      <label>Širina</label>
      <input name="latitude" type="number" step="0.000001" value={form.latitude} onChange={handleChange} required />
    </div>
    <div>
      <label>Dolžina</label>
      <input name="longitude" type="number" step="0.000001" value={form.longitude} onChange={handleChange} required />
    </div>

    <div>
      <label>Skupno število običajnih parkirnih mest</label>
      <input name="total_regular_spots" type="number" value={form.total_regular_spots} onChange={handleChange} required />
    </div>
    <div>
      <label>Na voljo običajna parkirna mesta</label>
      <input name="available_regular_spots" type="number" value={form.available_regular_spots} onChange={handleChange} required />
    </div>

    <div>
      <label>Skupno število parkirnih mest za invalide</label>
      <input name="total_invalid_spots" type="number" value={form.total_invalid_spots} onChange={handleChange} required />
    </div>
    <div>
      <label>Na voljo parkirna mesta za invalide</label>
      <input name="available_invalid_spots" type="number" value={form.available_invalid_spots} onChange={handleChange} required />
    </div>

    <div>
      <label>Skupno število parkirnih mest za električna vozila</label>
      <input name="total_electric_spots" type="number" value={form.total_electric_spots} onChange={handleChange} required />
    </div>
    <div>
      <label>Na voljo parkirna mesta za električna vozila</label>
      <input name="available_electric_spots" type="number" value={form.available_electric_spots} onChange={handleChange} required />
    </div>

    <div>
      <label>Skupno število avtobusnih parkirnih mest</label>
      <input name="total_bus_spots" type="number" value={form.total_bus_spots} onChange={handleChange} required />
    </div>
    <div>
      <label>Na voljo avtobusna parkirna mesta</label>
      <input name="available_bus_spots" type="number" value={form.available_bus_spots} onChange={handleChange} required />
    </div>

    <div style={{ gridColumn: '1 / -1' }}>
      <label>Opis</label>
      <textarea name="description" value={form.description} onChange={handleChange} rows={3} />
    </div>

    <div style={{ gridColumn: '1 / -1', textAlign: 'right' }}>
      <button
        type="submit"
        disabled={loading}
        style={{
          backgroundColor: '#3c6e71',
          color: 'white',
          border: 'none',
          padding: '10px 20px',
          borderRadius: '8px',
          fontWeight: 'bold',
          fontSize: '1rem',
          cursor: 'pointer',
        }}
      >
        {loading ? 'Dodajanje...' : 'Dodaj lokacijo'}
      </button>
    </div>
  </form>
);

}



export default Homepage;
