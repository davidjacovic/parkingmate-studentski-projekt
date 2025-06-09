import React, { useState, useEffect, useContext } from 'react';
import { UserContext } from '../userContext'; // ako koristiš UserContext
import LocationItem from './LocationItem'; // ili prava putanja do komponente

function Reviews() {
  const { user } = useContext(UserContext);
  const [locations, setLocations] = useState([]);
  const [error, setError] = useState('');
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

    fetch(`http://localhost:3002/parkingLocations/parking-filter?${params.toString()}`)
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

  return (
    <div>


      {/* Loading i greške */}
      {loading && <p>Nalaganje lokacij...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {/* Lista lokacija */}
      {locations.length === 0 && !loading ? (
        <p>Ni razpoložljivih parkirnih mest za izbrane filtre.</p>
      ) : (
        locations.map(loc => (
          <LocationItem key={loc._id} loc={loc} user={user} />
        ))
      )}
    </div>
  );
}

export default Reviews;
