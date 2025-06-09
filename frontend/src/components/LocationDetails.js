import React, { useEffect, useState, useRef, useContext } from 'react';
import { useParams } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet-routing-machine/dist/leaflet-routing-machine.css';
import 'leaflet-routing-machine';
import userIconImg from '../assets/man-location.png';
import ParkingAvailabilityChart from './ParkingChart';
import { UserContext } from '../userContext';

// Fix for default Leaflet icons
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
      waypoints: [L.latLng(from[0], from[1]), L.latLng(to[0], to[1])],
      lineOptions: { styles: [{ color: 'blue', opacity: 0.6, weight: 5 }] },
      addWaypoints: false,
      draggableWaypoints: false,
      fitSelectedRoutes: true,
      showAlternatives: false,
      createMarker: () => null,
    }).addTo(map);

    routingControlRef.current = routingControl;

    return () => {
      if (map && routingControlRef.current && map.hasLayer(routingControlRef.current)) {
        routingControlRef.current.remove();
      }
      routingControlRef.current = null;
    };
  }, [map, from, to]);

  return null;
}

function LocationDetails() {
  const { id } = useParams();
  const { user } = useContext(UserContext);

  const [location, setLocation] = useState(null);
  const [userLocation, setUserLocation] = useState(null);
  const [tariffs, setTariffs] = useState([]);
  const [error, setError] = useState('');
  const [tariffsError, setTariffsError] = useState('');

  const [formData, setFormData] = useState({
    name: '',
    address: '',
    description: '',
    total_regular_spots: 0,
    total_invalid_spots: 0,
    total_bus_spots: 0,
    available_regular_spots: 0,
    available_invalid_spots: 0,
    available_bus_spots: 0,
    location: { coordinates: [0, 0] },
  });

  // Forma za update tarife
  const [tariffFormData, setTariffFormData] = useState({
    _id: '',
    vehicle_type: '',
    duration: '',
    tariff_type: '',
    price: '',
    price_unit: '',
  });
  const [newTariffFormData, setNewTariffFormData] = useState({
    vehicle_type: '',
    duration: '',
    tariff_type: '',
    price: '',
    price_unit: '',
  });


  // Učitavanje lokacije
  useEffect(() => {
    fetch(`http://localhost:3002/parkingLocations/${id}`)
      .then((res) => {
        if (!res.ok) throw new Error('Neuspešno učitavanje detalja lokacije');
        return res.json();
      })
      .then((data) => {
        setLocation(data);
        setFormData({
          name: data.name || '',
          address: data.address || '',
          description: data.description || '',
          total_regular_spots: data.total_regular_spots || 0,
          total_invalid_spots: data.total_invalid_spots || 0,
          total_bus_spots: data.total_bus_spots || 0,
          available_regular_spots: data.available_regular_spots || 0,
          available_invalid_spots: data.available_invalid_spots || 0,
          available_bus_spots: data.available_bus_spots || 0,
          location: data.location || { coordinates: [0, 0] },
        });
      })
      .catch(() => setError('Greška pri učitavanju detalja lokacije.'));
  }, [id]);

  // Učitavanje tarifa
  useEffect(() => {
    fetch(`http://localhost:3002/tariffs/by-location/${id}`)
      .then((res) => {
        if (!res.ok) throw new Error();
        return res.json();
      })
      .then(setTariffs)
      .catch(() => setTariffsError('Greška pri učitavanju tarifa.'));
  }, [id]);

  // Geolokacija korisnika
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => setUserLocation([pos.coords.latitude, pos.coords.longitude]),
        (err) => console.error(err)
      );
    }
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]:
        ['total_regular_spots', 'total_invalid_spots', 'total_bus_spots', 'available_regular_spots', 'available_invalid_spots', 'available_bus_spots'].includes(name)
          ? Number(value)
          : value,
    }));
  };

  const handleCoordinateChange = (index, val) => {
    setFormData((prev) => {
      const newCoords = [...prev.location.coordinates];
      newCoords[index] = Number(val);
      return { ...prev, location: { coordinates: newCoords } };
    });
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`http://localhost:3002/parkingLocations/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token ? `Bearer ${token}` : undefined,
        },
        body: JSON.stringify(formData),
      });
      if (!res.ok) {
        const errData = await res.json();
        alert(`Greška pri ažuriranju: ${errData.message || 'Nepoznata greška'}`);
      } else {
        const updated = await res.json();
        setLocation(updated);
        alert('Uspešno ažurirano!');
      }
    } catch {
      alert('Greška pri slanju zahteva za ažuriranje.');
    }
  };

  const handleDeleteTariff = async (tariffId) => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`http://localhost:3002/tariffs/${tariffId}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (res.status === 204) {
        setTariffs((prev) => prev.filter((t) => t._id !== tariffId));
        // Ako je obrisana tarifa bila izabrana za update, resetuj formu
        if (tariffFormData._id === tariffId) {
          setTariffFormData({
            _id: '',
            vehicle_type: '',
            duration: '',
            tariff_type: '',
            price: '',
            price_unit: '',
          });
        }
      } else {
        const data = await res.json();
        alert(`Greška pri brisanju tarife: ${data.message || 'Nepoznata greška'}`);
      }
    } catch {
      alert('Greška pri slanju zahteva za brisanje.');
    }
  };

  // Handleri za update tarife
  const handleTariffSelect = (tariff) => {
    setTariffFormData({
      _id: tariff._id,
      vehicle_type: tariff.vehicle_type,
      duration: tariff.duration,
      tariff_type: tariff.tariff_type,
      price: tariff.price ? parseFloat(tariff.price.$numberDecimal || tariff.price).toFixed(2) : '',
      price_unit: tariff.price_unit,
    });
  };

  const handleTariffChange = (e) => {
    const { name, value } = e.target;
    setTariffFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleTariffUpdate = async (e) => {
    e.preventDefault();
    if (!tariffFormData._id) {
      alert('Izaberite tarifu za ažuriranje.');
      return;
    }
    try {
      const token = localStorage.getItem('token');
      const body = {
        vehicle_type: tariffFormData.vehicle_type,
        duration: tariffFormData.duration,
        tariff_type: tariffFormData.tariff_type,
        price: Number(tariffFormData.price),
        price_unit: tariffFormData.price_unit,
        location_id: id,
      };
      const res = await fetch(`http://localhost:3002/tariffs/${tariffFormData._id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token ? `Bearer ${token}` : undefined,
        },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const errData = await res.json();
        alert(`Greška pri ažuriranju tarife: ${errData.message || 'Nepoznata greška'}`);
      } else {
        const updatedTariff = await res.json();
        setTariffs((prev) => prev.map((t) => (t._id === updatedTariff._id ? updatedTariff : t)));
        alert('Tarifa uspešno ažurirana!');
      }
    } catch {
      alert('Greška pri slanju zahteva za ažuriranje tarife.');
    }
  };
  const handleNewTariffChange = (e) => {
    const { name, value } = e.target;
    setNewTariffFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };
  const handleNewTariffSubmit = async (e) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const body = {
        vehicle_type: newTariffFormData.vehicle_type,
        duration: newTariffFormData.duration,
        tariff_type: newTariffFormData.tariff_type,
        price: Number(newTariffFormData.price),
        price_unit: newTariffFormData.price_unit,
        location_id: id,
      };
      const res = await fetch(`http://localhost:3002/tariffs`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token ? `Bearer ${token}` : undefined,
        },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const errData = await res.json();
        alert(`Napaka pri dodajanju tarife: ${errData.message || 'Napaka'}`);
      } else {
        const newTariff = await res.json();
        setTariffs((prev) => [...prev, newTariff]);
        setNewTariffFormData({
          vehicle_type: '',
          duration: '',
          tariff_type: '',
          price: '',
          price_unit: '',
        });
        alert('Nova tarifa je uspešno dodata!');
      }
    } catch {
      alert('Napaka pri posiljanju zahteve za dodajanje nove tarife.');
    }
  };


  if (error) return <p style={{ color: 'red' }}>{error}</p>;
  if (!location) return <p>Učitavanje...</p>;
  return (
    <div style={{ maxWidth: '1200px', margin: '2rem auto', padding: '0 1rem', display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
      {/* Leva kolona: tekstualni detalji i tarife */}
      <div style={{ flex: '1 1 600px', minWidth: '320px' }}>
        <h2>{location.name}</h2>
        <p><strong>Opis:</strong> {location.description || 'Ni opisa za to lokacijo.'}</p>
        <p>
          <strong>Običajna mesta:</strong> {location.available_regular_spots}/{location.total_regular_spots}<br />
          <strong>Invalidska mesta:</strong> {location.available_invalid_spots}/{location.total_invalid_spots}<br />
          <strong>Električna mesta:</strong> {location.available_electric_spots}/{location.total_electric_spots}<br />
          <strong>Avtobusna mesta:</strong> {location.available_bus_spots}/{location.total_bus_spots}
        </p>


        <div style={{ marginTop: '2rem' }}>
          <h3>Tarife</h3>
          {tariffsError ? (
            <p style={{ color: 'red' }}>{tariffsError}</p>
          ) : tariffs.length === 0 ? (
            <p>Ni tarifa za to lokacijo.</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr>
                  <th style={{ borderBottom: '1px solid #ccc', padding: '8px', textAlign: 'left' }}>Vrsta vozila</th>
                  <th style={{ borderBottom: '1px solid #ccc', padding: '8px', textAlign: 'left' }}>Trajanje</th>
                  <th style={{ borderBottom: '1px solid #ccc', padding: '8px', textAlign: 'left' }}>Vrsta tarife</th>
                  <th style={{ borderBottom: '1px solid #ccc', padding: '8px', textAlign: 'left' }}>Cena</th>
                  {user?.user_type === 'admin' && <th style={{ borderBottom: '1px solid #ccc', padding: '8px' }}>Akcije</th>}
                </tr>
              </thead>
              <tbody>
                {tariffs.map((tariff) => (
                  <tr key={tariff._id} style={{ borderBottom: '1px solid #eee' }}>
                    <td style={{ padding: '8px' }}>{tariff.tariff_type}</td>
                    <td style={{ padding: '8px' }}>{tariff.duration}</td>
                    <td style={{ padding: '8px' }}>{tariff.vehicle_type}</td>
                    <td style={{ padding: '8px' }}>
                      {tariff.price ? parseFloat(tariff.price.$numberDecimal || tariff.price).toFixed(2) : '-'} {tariff.price_unit}
                    </td>
                    {user?.user_type === 'admin' && (
                      <td style={{ padding: '8px' }}>
                        <button
                          onClick={() => handleDeleteTariff(tariff._id)}
                          style={{ marginRight: '8px', backgroundColor: 'red', color: 'white', border: 'none', padding: '4px 8px', cursor: 'pointer' }}
                        >
                          Izbriši
                        </button>
                        <button
                          onClick={() => handleTariffSelect(tariff)}
                          style={{ backgroundColor: 'green', color: 'white', border: 'none', padding: '4px 8px', cursor: 'pointer' }}
                        >
                          Uredi
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {user?.user_type === 'admin' && (
          <>
            {/* Forma za update parking lokacije */}
            {/* Obrazec za posodobitev lokacije parkirišča */}
            <form onSubmit={handleUpdate} style={{ marginTop: '2rem', border: '1px solid #ccc', padding: '1rem', borderRadius: '8px' }}>
              <h3>Posodobi podrobnosti lokacije</h3>
              <label>
                Ime:
                <input type="text" name="name" value={formData.name} onChange={handleChange} required />
              </label>
              <br />
              <label>
                Naslov:
                <input type="text" name="address" value={formData.address} onChange={handleChange} required />
              </label>
              <br />
              <label>
                Opis:
                <textarea name="description" value={formData.description} onChange={handleChange} />
              </label>
              <br />
              <label>
                Skupno rednih mest:
                <input type="number" name="total_regular_spots" value={formData.total_regular_spots} onChange={handleChange} min={0} />
              </label>
              <br />
              <label>
                Skupno invalidskih mest:
                <input type="number" name="total_invalid_spots" value={formData.total_invalid_spots} onChange={handleChange} min={0} />
              </label>
              <br />
              <label>
                Skupno avtobusnih mest:
                <input type="number" name="total_bus_spots" value={formData.total_bus_spots} onChange={handleChange} min={0} />
              </label>
              <br />
              <label>
                Prosta redna mesta:
                <input type="number" name="available_regular_spots" value={formData.available_regular_spots} onChange={handleChange} min={0} />
              </label>
              <br />
              <label>
                Prosta invalidska mesta:
                <input type="number" name="available_invalid_spots" value={formData.available_invalid_spots} onChange={handleChange} min={0} />
              </label>
              <br />
              <label>
                Prosta avtobusna mesta:
                <input type="number" name="available_bus_spots" value={formData.available_bus_spots} onChange={handleChange} min={0} />
              </label>
              <br />
              <label>
                Koordinate (lat, lng):
                <input
                  type="number"
                  step="0.000001"
                  value={formData.location.coordinates[1]}
                  onChange={(e) => handleCoordinateChange(1, e.target.value)}
                  required
                />
                ,
                <input
                  type="number"
                  step="0.000001"
                  value={formData.location.coordinates[0]}
                  onChange={(e) => handleCoordinateChange(0, e.target.value)}
                  required
                />
              </label>
              <br />
              <button type="submit" style={{ marginTop: '1rem', padding: '0.5rem 1rem' }}>
                Posodobi lokacijo
              </button>
            </form>


            {/* Obrazec za posodobitev tarife */}
            <form onSubmit={handleTariffUpdate} style={{ marginTop: '2rem', border: '1px solid #ccc', padding: '1rem', borderRadius: '8px' }}>
              <h3>Uredi tarifo</h3>
              <label>
                Vrsta vozila:
                <input
                  type="text"
                  name="vehicle_type"
                  value={tariffFormData.vehicle_type}
                  onChange={handleTariffChange}
                  required
                />
              </label>
              <br />
              <label>
                Trajanje:
                <input
                  type="text"
                  name="duration"
                  value={tariffFormData.duration}
                  onChange={handleTariffChange}
                  required
                />
              </label>
              <br />
              <label>
                Vrsta tarife:
                <input
                  type="text"
                  name="tariff_type"
                  value={tariffFormData.tariff_type}
                  onChange={handleTariffChange}
                  required
                />
              </label>
              <br />
              <label>
                Cena:
                <input
                  type="number"
                  step="0.01"
                  name="price"
                  value={tariffFormData.price}
                  onChange={handleTariffChange}
                  required
                />
              </label>
              <br />
              <label>
                Enota cene:
                <input
                  type="text"
                  name="price_unit"
                  value={tariffFormData.price_unit}
                  onChange={handleTariffChange}
                  required
                />
              </label>
              <br />
              <button type="submit" style={{ marginTop: '1rem', padding: '0.5rem 1rem' }}>
                Posodobi tarifo
              </button>
            </form>

            {/* Obrazec za dodajanje nove tarife */}
            <form onSubmit={handleNewTariffSubmit} style={{ marginTop: '2rem', border: '1px solid #ccc', padding: '1rem', borderRadius: '8px' }}>
              <h3>Dodaj novo tarifo</h3>
              <label>
                Vrsta vozila:
                <input
                  type="text"
                  name="vehicle_type"
                  value={newTariffFormData.vehicle_type}
                  onChange={handleNewTariffChange}
                  required
                />
              </label>
              <br />
              <label>
                Trajanje:
                <input
                  type="text"
                  name="duration"
                  value={newTariffFormData.duration}
                  onChange={handleNewTariffChange}
                  required
                />
              </label>
              <br />
              <label>
                Vrsta tarife:
                <input
                  type="text"
                  name="tariff_type"
                  value={newTariffFormData.tariff_type}
                  onChange={handleNewTariffChange}
                  required
                />
              </label>
              <br />
              <label>
                Cena:
                <input
                  type="number"
                  step="0.01"
                  name="price"
                  value={newTariffFormData.price}
                  onChange={handleNewTariffChange}
                  required
                />
              </label>
              <br />
              <label>
                Enota cene:
                <input
                  type="text"
                  name="price_unit"
                  value={newTariffFormData.price_unit}
                  onChange={handleNewTariffChange}
                  required
                />
              </label>
              <br />
              <button type="submit" style={{ marginTop: '1rem', padding: '0.5rem 1rem' }}>
                Dodaj tarifo
              </button>
            </form>

          </>
        )}

      </div>

      {/* Desna kolona: mapa i graf */}
      <div style={{ flex: '1 1 400px', minWidth: '300px' }}>
        {userLocation && (
          <div style={{ height: '400px', width: '100%', marginBottom: '2rem' }}>
            <MapContainer
              center={[location.location.coordinates[1], location.location.coordinates[0]]}
              zoom={15}
              style={{ height: '100%', width: '100%', borderRadius: '8px', boxShadow: '0 0 10px rgba(0,0,0,0.1)' }}
            >
              <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
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

        <h3>Status zasedenosti</h3>
        <ParkingAvailabilityChart locationId={id} />
      </div>
    </div>
  );

}

export default LocationDetails;
