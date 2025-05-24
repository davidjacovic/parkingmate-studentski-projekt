import React, { useContext, useEffect, useState } from 'react';
import { UserContext } from '../userContext';
import { Link } from 'react-router-dom';

function Homepage() {
    const { user } = useContext(UserContext);
    const [locations, setLocations] = useState([]);
    const [error, setError] = useState('');

    useEffect(() => {
        fetch('http://localhost:3002/parkingLocations')
            .then(res => {
                if (!res.ok) throw new Error('Neuspešno učitavanje lokacija');
                return res.json();
            })
            .then(data => setLocations(data))
            .catch(err => {
                console.error(err);
                setError('Greška pri učitavanju lokacija.');
            });
    }, []);

    return (
        <div className="text-center mt-5">
            <h1>HOME PAGE</h1>

            {error && <p style={{ color: 'red' }}>{error}</p>}

            <div style={{ maxWidth: '900px', margin: '2rem auto', textAlign: 'left' }}>
                <h2>Spisak ulica i parking mesta</h2>
                {locations.map((loc) => (
                    <div
                        key={loc._id}
                        style={{
                            border: '1px solid #ccc',
                            borderRadius: '8px',
                            padding: '1rem',
                            marginBottom: '1.5rem',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center'
                        }}
                    >
                        <div style={{ flex: 1 }}>
                            <h4>
                                <Link
                                    to={`/location/${loc._id}`}
                                    style={{ textDecoration: 'none', color: '#007bff' }}
                                >
                                    {loc.name}
                                </Link>
                            </h4>
                            <p><strong>Adresa:</strong> {loc.address}</p>
                            <p>
                                <strong>Regularna mesta:</strong> Slobodnih: {loc.available_regular_spots} / Ukupno: {loc.total_regular_spots} <br />
                                <strong>Invalidska mesta:</strong> Slobodnih: {loc.available_invalid_spots} / Ukupno: {loc.total_invalid_spots} <br />
                                <strong>Električna mesta:</strong> Slobodnih: {loc.available_electric_spots} / Ukupno: {loc.total_electric_spots} <br />
                                <strong>Autobuska mesta:</strong> Slobodnih: {loc.available_bus_spots} / Ukupno: {loc.total_bus_spots}
                            </p>
                        </div>

                        <div style={{ marginLeft: '1rem' }}>
                            <a
                                href={`https://www.google.com/maps?q=${loc.latitude},${loc.longitude}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                style={{
                                    fontSize: '1.5rem',
                                    textDecoration: 'none',
                                    color: '#007bff',
                                    marginLeft: '10px'
                                }}
                                title="Otvori u Google mapi"
                            >
                                📍
                            </a>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default Homepage;
