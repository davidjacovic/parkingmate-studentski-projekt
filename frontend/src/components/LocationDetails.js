import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

function LocationDetails() {
    const { id } = useParams();
    const [location, setLocation] = useState(null);
    const [error, setError] = useState('');

    useEffect(() => {
        fetch(`http://localhost:3002/parkingLocations/${id}`)
            .then(res => {
                if (!res.ok) throw new Error('Neuspešno učitavanje detalja lokacije');
                return res.json();
            })
            .then(data => setLocation(data))
            .catch(err => {
                console.error(err);
                setError('Greška pri učitavanju detalja lokacije.');
            });
    }, [id]);

    if (error) return <p style={{ color: 'red' }}>{error}</p>;
    if (!location) return <p>Učitavanje...</p>;

    return (
        <div style={{ maxWidth: '600px', margin: '2rem auto', textAlign: 'left' }}>
            <h2>{location.name}</h2>
            <p><strong>Adresa:</strong> {location.address}</p>
        </div>
    );
}

export default LocationDetails;
