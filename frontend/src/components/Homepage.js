import React, { useContext, useEffect, useState } from 'react';
import { UserContext } from '../userContext';
import { Link, Routes, Route } from 'react-router-dom';
import { findNearestParking } from '../utils/geoUtils';
import MapView from './MyMapView';

import ProximitySearch from './ProximitySearch';
import CoverageAnalysis from './CoverageAnalysis';
import ParkingOccupancyMap from './ParkingOccupancyMap';

// --- LocationItem komponenta ---
function LocationItem({ loc, user }) {
  const [commentsVisible, setCommentsVisible] = useState(true); // odmah prikaz komentara
  const [comments, setComments] = useState([]);
  const [loadingComments, setLoadingComments] = useState(true);
  const [error, setError] = useState('');

  // Učitavamo komentare na prvi render i kad se promeni loc._id
  useEffect(() => {
    console.log('📥 Učitavanje komentara za:', loc._id);
    setLoadingComments(true);
    fetch(`http://localhost:3002/reviews/parking/${loc._id}`)
      .then(res => {
        console.log('⬅ Učitani komentari status:', res.status);
        return res.json();
      })
      .then(data => {
        console.log('✅ Komentari učitani:', data);
        setComments(data);
        setLoadingComments(false);
        setError('');
      })
      .catch((err) => {
        console.error('❌ Greška pri fetch-u komentara:', err);
        setError('Greška pri učitavanju komentara.');
        setLoadingComments(false);
      });
  }, [loc._id]);


  const toggleComments = () => {
    setCommentsVisible(!commentsVisible);
  };
  const reloadComments = () => {
    setLoadingComments(true);
    fetch(`http://localhost:3002/reviews/parking/${loc._id}`)
      .then(res => res.json())
      .then(data => {
        setComments(data);
        setLoadingComments(false);
        setError('');
      })
      .catch((err) => {
        console.error('❌ Greška pri refresh-u komentara:', err);
        setError('Greška pri učitavanju komentara.');
        setLoadingComments(false);
      });
  };


  return (
    <div style={{ marginBottom: '1rem', border: '1px solid #ddd', padding: '0.5rem' }}>
      <h4>{loc.name}</h4>
      <p>Adresa: {loc.address}</p>
      <p>
        Regularna mesta: Slobodnih: {loc.available_regular_spots} / Ukupno: {loc.total_regular_spots} <br />
        Invalidska mesta: Slobodnih: {loc.available_invalid_spots} / Ukupno: {loc.total_invalid_spots} <br />
        Električna mesta: Slobodnih: {loc.available_electric_spots} / Ukupno: {loc.total_electric_spots} <br />
        Autobuska mesta: Slobodnih: {loc.available_bus_spots} / Ukupno: {loc.total_bus_spots}
      </p>

      <button onClick={toggleComments} style={{ cursor: 'pointer' }} title={commentsVisible ? "Sakrij komentare" : "Prikaži komentare"}>
        💬
      </button>

      {commentsVisible && (
        <div style={{ marginTop: '0.5rem', borderTop: '1px solid #ccc', paddingTop: '0.5rem' }}>
          {loadingComments && <p>Učitavanje komentara...</p>}
          {error && <p style={{ color: 'red' }}>{error}</p>}
          {!loadingComments && comments.length === 0 && <p>Nema komentara za ovo parking mesto.</p>}
          {!loadingComments && comments.length > 0 && comments.map(c => (
            <div key={c._id} style={{ marginBottom: '0.5rem' }}>
              <b>{c.user?.username || 'Anonimni korisnik'}</b> ocena: {c.rating}/10<br />
              <p>{c.review_text}</p>
              <small>{new Date(c.review_date).toLocaleDateString()}</small>
            </div>
          ))}
          {user ? (
            <AddCommentForm
              parkingId={loc._id}
              onNewComment={reloadComments}
            />

          ) : (
            <p style={{ fontStyle: 'italic' }}>Prijavite se da biste dodali komentar.</p>
          )}
        </div>
      )}
    </div>
  );
}

// --- AddCommentForm komponenta ---
function AddCommentForm({ parkingId, onNewComment }) {
  const [rating, setRating] = useState(5);
  const [reviewText, setReviewText] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = e => {
    e.preventDefault();
    setError('');
    console.log('▶ Slanje komentara...');
    console.log('Ocena:', rating);
    console.log('Tekst:', reviewText);
    console.log('Parking ID:', parkingId);

    const token = localStorage.getItem('token');
    console.log('JWT token:', token);

    if (rating < 1 || rating > 10) {
      setError('Ocena mora biti između 1 i 10.');
      return;
    }

    setSubmitting(true);

    fetch('http://localhost:3002/reviews', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({
        rating,
        review_text: reviewText,
        parking_location: parkingId,
        
      }),
    })
      .then(res => {
        console.log('⬅ Server odgovor status:', res.status);
        if (!res.ok) throw new Error('Greška pri slanju komentara');
        return res.json();
      })
      .then(newComment => {
        console.log('✅ Komentar uspešno dodat:', newComment);
        onNewComment();
        setRating(5);
        setReviewText('');
      })
      .catch(err => {
        console.error('❌ Fetch greška:', err);
        setError('Greška pri slanju komentara.');
      })
      .finally(() => setSubmitting(false));
  };


  return (
    <form onSubmit={handleSubmit} style={{ marginTop: '1rem' }}>
      <label>
        Ocena (1-10):{' '}
        <input
          type="number"
          min="1"
          max="10"
          value={rating}
          onChange={e => setRating(Number(e.target.value))}
          required
        />
      </label>
      <br />
      <label>
        Komentar:<br />
        <textarea
          value={reviewText}
          onChange={e => setReviewText(e.target.value)}
          rows="3"
          cols="30"
          required
        />
      </label>
      <br />
      <button type="submit" disabled={submitting}>{submitting ? 'Šaljem...' : 'Dodaj komentar'}</button>
      {error && <p style={{ color: 'red' }}>{error}</p>}
    </form>
  );
}


// --- Homepage komponenta ---
function Homepage() {
  const { user } = useContext(UserContext);
  const [locations, setLocations] = useState([]);
  const [error, setError] = useState('');
  const [userLocation, setUserLocation] = useState(null);
  const [nearestParking, setNearestParking] = useState(null);
  const [loading, setLoading] = useState(false);

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
    <div style={{ maxWidth: 900, margin: '1rem auto', padding: '0 1rem' }}>
      <h1>HOME PAGE</h1>

      <nav style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
        <Link to="/locations/nearby" style={{ textDecoration: 'none', color: 'blue' }}>
          Nearby Parkings
        </Link>
        <Link to="/locations/coverage" style={{ textDecoration: 'none', color: 'blue' }}>
          Coverage Analysis
        </Link>
        <Link to="/locations/occupancy" style={{ textDecoration: 'none', color: 'blue' }}>
          Occupancy Status
        </Link>
      </nav>

      <Routes>
        <Route path="/" element={
          <>
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

            <div style={{ marginBottom: '1rem', marginTop: '1rem' }}>
              <p>Prikazano {locations.length} rezultata</p>
              <button onClick={() => setFilters({ regular: false, invalid: false, electric: false, bus: false })}>
                Ponastavi filtre
              </button>
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
                  <LocationItem key={loc._id} loc={loc} user={user} />
                ))
              )}
            </div>
          </>
        } />

        <Route path="locations/nearby" element={<ProximitySearch />} />
        <Route path="locations/coverage" element={<CoverageAnalysis />} />
        <Route path="locations/occupancy" element={<ParkingOccupancyMap />} />
        <Route path="*" element={<p>Izaberite opciju iz menija.</p>} />
      </Routes>
    </div>
  );
}

export default Homepage;
