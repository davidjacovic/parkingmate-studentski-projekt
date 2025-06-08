import { useEffect, useState, useRef } from 'react';
import { io } from 'socket.io-client';

function Payment() {
    const [searchTerm, setSearchTerm] = useState('');
    const [suggestions, setSuggestions] = useState([]);
    const [selectedLocation, setSelectedLocation] = useState(null);
    const [tariffs, setTariffs] = useState([]);
    const [creditCard, setCreditCard] = useState('');
    const [vehiclePlate, setVehiclePlate] = useState('');
    const [duration, setDuration] = useState('');
    const [price, setPrice] = useState(null);
    const [priceUnit, setPriceUnit] = useState('');
    const [activePayments, setActivePayments] = useState([]);
    const [countdowns, setCountdowns] = useState({});
    const notifiedExpiredPlates = useRef(new Set());
    const countdownInterval = useRef(null);
    const socket = useRef(null);

    const formatTime = (ms) => {
        if (ms <= 0) return '00:00:00';
        let totalSeconds = Math.floor(ms / 1000);
        const hours = String(Math.floor(totalSeconds / 3600)).padStart(2, '0');
        totalSeconds %= 3600;
        const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
        const seconds = String(totalSeconds % 60).padStart(2, '0');
        return `${hours}:${minutes}:${seconds}`;
    };

    const initializeCountdowns = (payments) => {
        const now = Date.now();
        const initialCountdowns = {};
        payments.forEach(({ vehiclePlate, expiresAt }) => {
            const diff = expiresAt - now;
            initialCountdowns[vehiclePlate] = diff > 0 ? formatTime(diff) : 'Isteklo';
        });
        return initialCountdowns;
    };
    useEffect(() => {
        socket.current = io('http://localhost:3002');  // adresu servera promeni po potrebi

        // sluša notifikacije sa servera
        socket.current.on('parking-expired', ({ vehiclePlate }) => {
            alert(`Vreme za vozilo ${vehiclePlate} je isteklo. (preko Socket.IO)`);
        });

        return () => {
            socket.current.disconnect();
        };
    }, []);

    // 1) Load activePayments from localStorage on mount
    useEffect(() => {
        try {
            const savedPayments = JSON.parse(localStorage.getItem('activePayments'));
            if (Array.isArray(savedPayments)) {
                // Filtriraj samo validna (neistekla) plaćanja
                const validPayments = savedPayments.filter(p =>
                    p.expiresAt > Date.now() &&
                    p.vehiclePlate &&
                    p.vehiclePlate.trim() !== ''
                );
                setActivePayments(validPayments);
                setCountdowns(initializeCountdowns(validPayments));
            } else {
                setActivePayments([]);
                setCountdowns({});
            }
        } catch (error) {
            setActivePayments([]);
            setCountdowns({});
        }
    }, []);


    // 2) Fetch active payments from backend
    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            console.warn('[Payment] No token found in localStorage, skipping fetch active payments');
            return;
        }

        fetch('http://localhost:3002/payments/active-for-user', {
            headers: { Authorization: `Bearer ${token}` },
        })
            .then(res => {
                if (!res.ok) throw new Error(`Failed to fetch active payments, status: ${res.status}`);
                return res.json();
            })
            .then(data => {
                if (Array.isArray(data)) {
                    const validPayments = data.filter(p => p.expiresAt > Date.now());
                    setActivePayments(validPayments);
                    localStorage.setItem('activePayments', JSON.stringify(validPayments));
                    setCountdowns(initializeCountdowns(validPayments));
                } else {
                    setActivePayments([]);
                    setCountdowns({});
                }
            })
            .catch(err => {
                console.error('[Payment] Error fetching active payments:', err);
            });
    }, []);

    // Countdown for active payments
    useEffect(() => {
        if (countdownInterval.current) clearInterval(countdownInterval.current);

        countdownInterval.current = setInterval(() => {
            setCountdowns(prevCountdowns => {
                const now = Date.now();
                const newCountdowns = {};
                const stillActive = [];

                activePayments.forEach(({ vehiclePlate, expiresAt }) => {
                    const diff = expiresAt - now;
                    if (diff > 0) {
                        newCountdowns[vehiclePlate] = formatTime(diff);
                        stillActive.push({ vehiclePlate, expiresAt });
                    } else {
                        newCountdowns[vehiclePlate] = 'Isteklo';

                        if (!notifiedExpiredPlates.current.has(vehiclePlate)) {
                            notifiedExpiredPlates.current.add(vehiclePlate);

                            // POŠALJI SERVERU NOTIFIKACIJU
                            socket.current.emit('notify-expired', { vehiclePlate });
                        }
                    }
                });

                if (stillActive.length !== activePayments.length) {
                    setActivePayments(stillActive);
                    localStorage.setItem('activePayments', JSON.stringify(stillActive));
                }

                return newCountdowns;
            });
        }, 1000);

        return () => clearInterval(countdownInterval.current);
    }, [activePayments]);

    // Search locations
    useEffect(() => {
        if (!searchTerm.trim()) {
            console.log('[Payment] Search term empty, clearing suggestions');
            setSuggestions([]);
            return;
        }
        console.log('[Payment] Searching locations for:', searchTerm);

        const controller = new AbortController();
        const token = localStorage.getItem('token');

        fetch(`http://localhost:3002/payments/addresses/search?q=${encodeURIComponent(searchTerm)}`, {
            headers: { Authorization: `Bearer ${token}` },
            signal: controller.signal,
        })
            .then(res => {
                console.log('[Payment] Search locations response status:', res.status);
                if (!res.ok) throw new Error(`Failed to search locations, status: ${res.status}`);
                return res.json();
            })
            .then(data => {
                console.log('[Payment] Search locations results:', data);
                setSuggestions(data);
            })
            .catch(err => {
                if (err.name !== 'AbortError') console.error('[Payment] Search locations error:', err);
            });

        return () => {
            console.log('[Payment] Aborting location search for:', searchTerm);
            controller.abort();
        };
    }, [searchTerm]);

    // Load user info
    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            console.warn('[Payment] No token found, skipping user info fetch');
            return;
        }

        console.log('[Payment] Fetching user info');
        fetch('http://localhost:3002/payments/user-info', {
            headers: { Authorization: `Bearer ${token}` },
        })
            .then(res => {
                console.log('[Payment] Fetch user info response status:', res.status);
                if (!res.ok) throw new Error('Failed to fetch user info');
                return res.json();
            })
            .then(userData => {
                console.log('[Payment] User info fetched:', userData);
                setCreditCard(userData.credit_card_number || '');
                setVehiclePlate(userData.registration_number || '');
            })
            .catch(err => {
                console.error('[Payment] Error fetching user info:', err);
            });
    }, []);

    // Load tariffs for selected location
    useEffect(() => {
        if (!selectedLocation) {
            console.log('[Payment] No selected location, clearing tariffs');
            setTariffs([]);
            return;
        }

        console.log('[Payment] Fetching tariffs for location:', selectedLocation._id);
        fetch(`http://localhost:3002/tariffs/by-location/${selectedLocation._id}`)
            .then(res => {
                console.log('[Payment] Fetch tariffs response status:', res.status);
                if (!res.ok) throw new Error('Failed to load tariffs');
                return res.json();
            })
            .then(data => {
                console.log('[Payment] Tariffs fetched:', data);
                setTariffs(data);
            })
            .catch(err => {
                console.error('[Payment] Error fetching tariffs:', err);
                setTariffs([]);
            });
    }, [selectedLocation]);

    const handleSelect = (location) => {
        console.log('[Payment] Location selected:', location);
        setSelectedLocation(location);
        setSearchTerm(location.name);
        setSuggestions([]);
        setPrice(null);
        setPriceUnit('');
        setDuration('');
    };

    const handlePay = async () => {
        console.log('[Payment] Initiating payment');
        if (!selectedLocation || !creditCard.trim() || !vehiclePlate.trim() || !duration) {
            alert('Molimo popunite sva polja.');
            console.warn('[Payment] Payment failed: Missing fields');
            return;
        }

        const existingPayment = activePayments.find(p => p.vehiclePlate === vehiclePlate.trim() && p.expiresAt > Date.now());
        if (existingPayment) {
            alert('Već postoji aktivna uplata za ovu registarsku tablicu.');
            console.warn('[Payment] Payment failed: Active payment already exists for this vehicle');
            return;
        }

        const dur = parseInt(duration);
        if (isNaN(dur) || dur < 1) {
            alert('Trajanje mora biti broj veći ili jednak 1.');
            console.warn('[Payment] Payment failed: Invalid duration');
            return;
        }

        const token = localStorage.getItem('token');
        if (!token) {
            alert('Niste autorizovani.');
            console.warn('[Payment] Payment failed: No token');
            return;
        }

        try {
            const res = await fetch('http://localhost:3002/payments/calculate-and-pay', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({
                    locationId: selectedLocation._id,
                    duration: dur,
                    method: 'card',
                    credit_card: creditCard.trim(),
                    vehicle_plate: vehiclePlate.trim(),
                }),
            });

            console.log('[Payment] Payment response status:', res.status);
            const data = await res.json();

            if (!res.ok) {
                alert(`Greška: ${data.message || 'Nepoznata greška'}`);
                console.warn('[Payment] Payment error:', data.message || 'Unknown error');
                return;
            }

            console.log('[Payment] Payment successful:', data);
            setPrice(data.price);
            setPriceUnit(data.unit);

            const expiration = Date.now() + dur * 60 * 60 * 1000;
            const newPayment = { vehiclePlate: vehiclePlate.trim(), expiresAt: expiration };
            const updatedPayments = [...activePayments, newPayment];
            setActivePayments(updatedPayments);
            localStorage.setItem('activePayments', JSON.stringify(updatedPayments));

            alert(`Uspešna uplata. Iznos: ${data.price} ${data.unit}`);
        } catch (err) {
            console.error('[Payment] Greška pri plaćanju:', err);
            alert('Došlo je do greške prilikom plaćanja.');
        }
    };

    return (
        <div style={{ maxWidth: 600, margin: 'auto' }}>
            <h2>Pretraga Parking Lokacije</h2>
            <input
                type="text"
                placeholder="Unesite naziv..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                autoComplete="off"
                style={{ width: '100%', padding: '8px', fontSize: '1rem' }}
            />
            {suggestions.length > 0 && (
                <ul
                    style={{
                        border: '1px solid #ccc',
                        padding: 0,
                        marginTop: 0,
                        listStyle: 'none',
                        maxHeight: 150,
                        overflowY: 'auto',
                    }}
                >
                    {suggestions.map((loc) => (
                        <li
                            key={loc._id}
                            onClick={() => handleSelect(loc)}
                            style={{ padding: '8px', cursor: 'pointer', borderBottom: '1px solid #eee' }}
                        >
                            {loc.name}
                        </li>
                    ))}
                </ul>
            )}

            {selectedLocation && tariffs.length > 0 && (
                <>
                    <h3>Tarife za {selectedLocation.name}:</h3>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr>
                                <th>Tip</th>
                                <th>Trajanje</th>
                                <th>Tip vozila</th>
                                <th>Cena</th>
                                <th>Jedinica</th>
                            </tr>
                        </thead>
                        <tbody>
                            {tariffs.map((tariff) => (
                                <tr key={tariff._id}>
                                    <td>{tariff.tariff_type}</td>
                                    <td>{tariff.duration}</td>
                                    <td>{tariff.vehicle_type}</td>
                                    <td>{parseFloat(tariff.price.$numberDecimal).toFixed(2)}</td>
                                    <td>{tariff.price_unit}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </>
            )}

            <div style={{ marginTop: 20 }}>
                <label>Kreditna kartica</label>
                <br />
                <input
                    type="text"
                    value={creditCard}
                    onChange={(e) => setCreditCard(e.target.value)}
                    placeholder="Unesite broj kartice"
                    style={{ width: '100%', padding: '8px' }}
                />
            </div>

            <div style={{ marginTop: 10 }}>
                <label>Registarska tablica</label>
                <br />
                <input
                    type="text"
                    value={vehiclePlate}
                    onChange={(e) => setVehiclePlate(e.target.value)}
                    placeholder="Unesite registarsku tablicu"
                    style={{ width: '100%', padding: '8px' }}
                />
            </div>

            <div style={{ marginTop: 10 }}>
                <label>Trajanje (sati)</label>
                <br />
                <input
                    type="number"
                    value={duration}
                    onChange={(e) => setDuration(e.target.value)}
                    placeholder="Unesite trajanje u satima"
                    style={{ width: '100%', padding: '8px' }}
                    min="1"
                />
            </div>

            <button
                onClick={handlePay}
                style={{ marginTop: 15, padding: '10px 20px', fontSize: '1rem', cursor: 'pointer' }}
            >
                Plati
            </button>

            {price !== null && (
                <p>
                    Cena: {price} {priceUnit}
                </p>
            )}

            {activePayments.length > 0 && (
                <>
                    <h3 style={{ marginTop: 30 }}>Registarske tablice sa aktivnom uplatom:</h3>
                    <ul>
                        {activePayments.map(({ vehiclePlate }, index) => (
                            <li key={vehiclePlate ? vehiclePlate : `unknown-${index}`}>
                                Vozilo: <strong>{vehiclePlate || 'Nepoznato vozilo'}</strong> — {countdowns[vehiclePlate] || 'Učitavanje...'}
                            </li>
                        ))}
                    </ul>
                </>
            )}

        </div>
    );
}

export default Payment;
