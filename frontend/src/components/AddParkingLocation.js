import React, { useContext, useEffect, useState } from 'react';
import { UserContext } from '../userContext';


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