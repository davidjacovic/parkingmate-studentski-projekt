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

    useEffect(() => {
        if (!navigator.geolocation) {
            setError('Geolokacija nije podržana u vašem pretraživaču.');
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                setUserLocation([position.coords.longitude, position.coords.latitude]);
            },
            () => {
                setError('Dozvolite pristup lokaciji da biste videli najbliže parking mesto.');
            }
        );
    }, []);

    useEffect(() => {
        if (userLocation && locations.length > 0) {
            const spots = locations.map(loc => ({
                id: loc._id,
                coordinates: [loc.longitude, loc.latitude]
            }));

            const nearestId = findNearestParking(userLocation, spots);
            const nearestLoc = locations.find(loc => loc._id === nearestId);
            setNearestParking(nearestLoc);
        }
    }, [userLocation, locations]);

    return (
        <div>
            <h1>HOME PAGE</h1>

            {error && <p>{error}</p>}

            {nearestParking && (
                <>
                    <MapView
                        userLocation={userLocation}
                        parkingLocations={locations}
                        nearestId={nearestParking._id}
                    />
                    <div>
                        <h2>Najbliže parking mesto</h2>
                        <h4>{nearestParking.name}</h4>
                        <p>Adresa: {nearestParking.address}</p>
                        <a
                            href={`https://www.google.com/maps?q=${nearestParking.latitude},${nearestParking.longitude}`}
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
                {locations.map((loc) => (
                    <div key={loc._id}>
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
                                href={`https://www.google.com/maps?q=${loc.latitude},${loc.longitude}`}
                                target="_blank"
                                rel="noopener noreferrer"
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
