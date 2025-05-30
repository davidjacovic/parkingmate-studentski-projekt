import React, { useEffect, useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet-routing-machine/dist/leaflet-routing-machine.css';
import 'leaflet-routing-machine';
import userIconImg from '../assets/man-location.png';
import ParkingAvailabilityChart from './ParkingChart';


delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
    iconUrl: require('leaflet/dist/images/marker-icon.png'),
    shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

const userIcon = new L.Icon({
    iconUrl: userIconImg,
    iconSize: [40, 40],
    iconAnchor: [20, 40],
    popupAnchor: [0, -40],
});

function Routing({ from, to }) {
    const map = useMap();
    const routingControlRef = useRef(null);

    useEffect(() => {
        if (!from || !to || !map) return;

        const routingControl = L.Routing.control({
            waypoints: [
                L.latLng(from[0], from[1]),
                L.latLng(to[0], to[1]),
            ],
            lineOptions: {
                styles: [{ color: 'blue', opacity: 0.6, weight: 5 }],
            },
            addWaypoints: false,
            draggableWaypoints: false,
            fitSelectedRoutes: true,
            showAlternatives: false,

            // Ovim onemogućavamo automatsko kreiranje markera
            createMarker: () => null,
        }).addTo(map);

        routingControlRef.current = routingControl;

        return () => {
            try {
                if (map && routingControlRef.current && map.hasLayer(routingControlRef.current)) {
                    routingControlRef.current.remove();
                }
                routingControlRef.current = null;
            } catch (err) {
                console.warn('Routing cleanup error:', err);
            }
        };

    }, [map, from, to]);

    return null;
}

function LocationDetails() {
    const { id } = useParams();
    const [location, setLocation] = useState(null);
    const [error, setError] = useState('');
    const [userLocation, setUserLocation] = useState(null);

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

    useEffect(() => {
        if (!navigator.geolocation) return;
        navigator.geolocation.getCurrentPosition(
            pos => setUserLocation([pos.coords.latitude, pos.coords.longitude]),
            err => console.error(err)
        );
    }, []);

    if (error) return <p style={{ color: 'red' }}>{error}</p>;
    if (!location) return <p>Učitavanje...</p>;

    return (
        <div style={{ maxWidth: '600px', margin: '2rem auto', textAlign: 'left' }}>
            <h2>{location.name}</h2>
            <p><strong>Adresa:</strong> {location.address}</p>
            <p>
                <strong>Regularna mesta:</strong> Slobodnih: {location.available_regular_spots} / Ukupno: {location.total_regular_spots} <br />
                <strong>Invalidska mesta:</strong> Slobodnih: {location.available_invalid_spots} / Ukupno: {location.total_invalid_spots} <br />
                <strong>Električna mesta:</strong> Slobodnih: {location.available_electric_spots} / Ukupno: {location.total_electric_spots} <br />
                <strong>Autobuska mesta:</strong> Slobodnih: {location.available_bus_spots} / Ukupno: {location.total_bus_spots}
            </p>

            {userLocation && (
                <div style={{ height: '400px', width: '100%', marginTop: '2rem' }}>
                    <MapContainer center={[location.location.coordinates[1], location.location.coordinates[0]]} zoom={15} style={{ height: '100%', width: '100%' }}>
                        <TileLayer
                            attribution='&copy; OpenStreetMap contributors'
                            url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
                        />
                        <Marker position={[location.location.coordinates[1], location.location.coordinates[0]]}>
                            <Popup>{location.name}</Popup>
                        </Marker>
                        <Marker position={userLocation} icon={userIcon}>
                            <Popup>Vaša lokacija</Popup>
                        </Marker>
                        <Routing from={userLocation} to={[location.location.coordinates[1], location.location.coordinates[0]]} />
                    </MapContainer>
                </div>
            )}
             <h3>Status zauzetosti</h3>
            <ParkingAvailabilityChart locationId={id} />
       </div>
    );
}

export default LocationDetails;
