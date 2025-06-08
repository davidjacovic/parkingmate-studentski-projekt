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

  useEffect(() => {
    setLoadingComments(true);
    fetch(`http://localhost:3002/reviews/parking/${loc._id}`)
      .then(res => res.json())
      .then(data => {
        setComments(data);
        setLoadingComments(false);
        setError('');
      })
      .catch(() => {
        setError('Greška pri učitavanju komentara.');
        setLoadingComments(false);
      });
  }, [loc._id]);

  const toggleComments = () => {
    setCommentsVisible(!commentsVisible);
  };
  const handleDeleteParking = () => {
    if (!window.confirm('Da li ste sigurni da želite da obrišete ovu parking lokaciju?')) return;

    const token = localStorage.getItem('token');
    fetch(`http://localhost:3002/parkingLocations/${loc._id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
      .then(res => {
        if (!res.ok) throw new Error('Greška pri brisanju lokacije.');
        alert('Parking lokacija uspešno obrisana.');
        // Opcionalno: refrešuj listu lokacija (ako je moguće proslediti `onDeleted` kao prop)
        window.location.reload(); // ili pozovi callback ako postoji
      })
      .catch(err => {
        console.error(err);
        alert('Greška pri brisanju lokacije.');
      });
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
      .catch(() => {
        setError('Greška pri učitavanju komentara.');
        setLoadingComments(false);
      });
  };

  const handleDeleteComment = (commentId) => {
    if (!window.confirm('Da li ste sigurni da želite da obrišete komentar?')) return;

    const token = localStorage.getItem('token');
    fetch(`http://localhost:3002/reviews/${commentId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => {
        if (!res.ok) throw new Error('Brisanje neuspešno');
        reloadComments();
      })
      .catch(() => {
        alert('Greška pri brisanju komentara.');
      });
  };

  return (
    <div style={{ marginBottom: '1rem', border: '1px solid #ddd', padding: '0.5rem' }}>
      <h4>
        <Link to={`/location/${loc._id}`} style={{ textDecoration: 'none', color: '#007bff' }}>
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

              {/* Dugme za brisanje samo za admina */}
              {user?.user_type === 'admin' && (
                <button
                  style={{ marginLeft: '1rem', color: 'red', cursor: 'pointer' }}
                  onClick={() => handleDeleteComment(c._id)}
                >
                  🗑 Obriši
                </button>
              )}
            </div>
          ))}

          {/* Forma za dodavanje komentara samo za user tip */}
          {user?.user_type === 'user' ? (
            <AddCommentForm
              parkingId={loc._id}
              onNewComment={reloadComments}
            />
          ) : user?.user_type === 'admin' ? null : (
            <p style={{ fontStyle: 'italic' }}>Prijavite se da biste dodali komentar.</p>
          )}
        </div>
      )}
      {user?.user_type === 'admin' && (
        <button
          onClick={handleDeleteParking}
          style={{ marginTop: '1rem', backgroundColor: 'red', color: 'white', padding: '0.5rem', border: 'none', cursor: 'pointer' }}
        >
          🗑 Obriši ovu lokaciju
        </button>
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
function AddParkingLocation({ onAdded }) {
  const { user } = useContext(UserContext);
  console.log('User u UserContext:', user);

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
    // hidden je uklonjen iz state-a
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (!user || user.user_type !== 'admin') {
    return <p>Nemate dozvolu da dodajete parking lokacije.</p>;
  }

  function handleChange(e) {
    const { name, value, type } = e.target;

    if (
      type === 'number' &&
      value !== '' &&
      (!/^\d*$/.test(value) || Number(value) < 0)
    ) {
      return;
    }

    setForm(prev => ({
      ...prev,
      [name]: value
    }));
  }

  async function handleSubmit(e) {
  e.preventDefault();
  setError('');

  // Validacije latitude i longitude
  const lat = parseFloat(form.latitude);
  const lng = parseFloat(form.longitude);
  if (isNaN(lat) || lat < -90 || lat > 90) {
    setError('Latitude mora biti broj između -90 i 90.');
    return;
  }
  if (isNaN(lng) || lng < -180 || lng > 180) {
    setError('Longitude mora biti broj između -180 i 180.');
    return;
  }

  // Validacije mesta
  const totalRegular = Number(form.total_regular_spots || 0);
  const availableRegular = Number(form.available_regular_spots || 0);
  const totalInvalid = Number(form.total_invalid_spots || 0);
  const availableInvalid = Number(form.available_invalid_spots || 0);
  const totalElectric = Number(form.total_electric_spots || 0);
  const availableElectric = Number(form.available_electric_spots || 0);
  const totalBus = Number(form.total_bus_spots || 0);
  const availableBus = Number(form.available_bus_spots || 0);

  if (availableRegular > totalRegular) {
    setError('Dostupna regular mesta ne mogu biti veća od ukupnih.');
    return;
  }
  if (availableInvalid > totalInvalid) {
    setError('Dostupna invalidska mesta ne mogu biti veća od ukupnih.');
    return;
  }
  if (availableElectric > totalElectric) {
    setError('Dostupna električna mesta ne mogu biti veća od ukupnih.');
    return;
  }
  if (availableBus > totalBus) {
    setError('Dostupna autobuska mesta ne mogu biti veća od ukupnih.');
    return;
  }

  setLoading(true);

  const payload = {
    name: form.name.trim(),
    address: form.address.trim(),
    description: form.description.trim(),
    location: {
      type: 'Point',
      coordinates: [lng, lat]
    },
    total_regular_spots: totalRegular,
    total_invalid_spots: totalInvalid,
    total_electric_spots: totalElectric,
    total_bus_spots: totalBus,
    available_regular_spots: availableRegular,
    available_invalid_spots: availableInvalid,
    available_electric_spots: availableElectric,
    available_bus_spots: availableBus,
  };

  try {
    // Uzmi token iz konteksta ili localStorage
    const token = user?.token || localStorage.getItem('token');
    if (!token) {
      setError('Niste prijavljeni ili je sesija istekla.');
      setLoading(false);
      return;
    }

    console.log('Token koji se salje:', token);

    const res = await fetch('http://localhost:3002/parkingLocations', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const errorData = await res.json();
      throw new Error(errorData.message || 'Greška prilikom dodavanja parkinga.');
    }

    const newParking = await res.json();
    onAdded(newParking);

    setForm({
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
  } catch (e) {
    setError(e.message);
  } finally {
    setLoading(false);
  }
}

  return (
    <form onSubmit={handleSubmit} style={{ maxWidth: 400 }}>
      <h3>Dodaj novu parking lokaciju</h3>
      {error && <p style={{ color: 'red' }}>{error}</p>}

      <label>
        Naziv lokacije:<br />
        <input
          name="name"
          placeholder="Unesite naziv parkinga"
          value={form.name}
          onChange={handleChange}
          required
        />
      </label>
      <br />

      <label>
        Adresa:<br />
        <input
          name="address"
          placeholder="Unesite adresu"
          value={form.address}
          onChange={handleChange}
          required
        />
      </label>
      <br />

      <label>
        Latitude:<br />
        <input
          type="number"
          step="0.000001"
          name="latitude"
          placeholder="npr. 44.7866"
          value={form.latitude}
          onChange={handleChange}
          required
        />
      </label>
      <br />

      <label>
        Longitude:<br />
        <input
          type="number"
          step="0.000001"
          name="longitude"
          placeholder="npr. 20.4489"
          value={form.longitude}
          onChange={handleChange}
          required
        />
      </label>
      <br />

      <label>
        Ukupno regular mesta:<br />
        <input
          type="number"
          name="total_regular_spots"
          placeholder="Ukupno regular mesta"
          value={form.total_regular_spots}
          onChange={handleChange}
          required
        />
      </label>
      <br />

      <label>
        Dostupno regular mesta:<br />
        <input
          type="number"
          name="available_regular_spots"
          placeholder="Dostupno regular mesta"
          value={form.available_regular_spots}
          onChange={handleChange}
          required
        />
      </label>
      <br />

      <label>
        Ukupno invalidska mesta:<br />
        <input
          type="number"
          name="total_invalid_spots"
          placeholder="Ukupno invalidska mesta"
          value={form.total_invalid_spots}
          onChange={handleChange}
          required
        />
      </label>
      <br />

      <label>
        Dostupno invalidska mesta:<br />
        <input
          type="number"
          name="available_invalid_spots"
          placeholder="Dostupno invalidska mesta"
          value={form.available_invalid_spots}
          onChange={handleChange}
          required
        />
      </label>
      <br />

      <label>
        Ukupno električna mesta:<br />
        <input
          type="number"
          name="total_electric_spots"
          placeholder="Ukupno električna mesta"
          value={form.total_electric_spots}
          onChange={handleChange}
        />
      </label>
      <br />

      <label>
        Dostupno električna mesta:<br />
        <input
          type="number"
          name="available_electric_spots"
          placeholder="Dostupno električna mesta"
          value={form.available_electric_spots}
          onChange={handleChange}
        />
      </label>
      <br />

      <label>
        Ukupno autobuska mesta:<br />
        <input
          type="number"
          name="total_bus_spots"
          placeholder="Ukupno autobuska mesta"
          value={form.total_bus_spots}
          onChange={handleChange}
        />
      </label>
      <br />

      <label>
        Dostupno autobuska mesta:<br />
        <input
          type="number"
          name="available_bus_spots"
          placeholder="Dostupno autobuska mesta"
          value={form.available_bus_spots}
          onChange={handleChange}
        />
      </label>
      <br />

      <label>
        Opis:<br />
        <textarea
          name="description"
          placeholder="Dodatni opis lokacije"
          value={form.description}
          onChange={handleChange}
          rows={3}
        />
      </label>
      <br />

      <button type="submit" disabled={loading}>
        {loading ? 'Dodavanje...' : 'Dodaj parking'}
      </button>
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
            {user?.user_type === 'admin' && (
              <>
                <button
                  onClick={() => setShowAddForm(prev => !prev)}
                  style={{ marginBottom: '1rem' }}
                >
                  {showAddForm ? 'Zatvori formu za dodavanje' : 'Dodaj novu lokaciju'}
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
