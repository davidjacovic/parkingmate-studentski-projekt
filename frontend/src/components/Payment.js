import { useEffect, useState } from 'react';

function Payment() {
    const [searchTerm, setSearchTerm] = useState('');
    const [suggestions, setSuggestions] = useState([]);
    const [selectedLocation, setSelectedLocation] = useState(null);
    const [creditCard, setCreditCard] = useState('');
    const [vehiclePlate, setVehiclePlate] = useState('');
    const [duration, setDuration] = useState('');
    const [price, setPrice] = useState(null);

    useEffect(() => {
        const controller = new AbortController();
        const token = localStorage.getItem('token');

        const fetchSuggestions = async () => {
            if (!searchTerm.trim()) {
                setSuggestions([]);
                return;
            }

            try {
                const res = await fetch(`http://localhost:3002/payments/addresses/search?q=${encodeURIComponent(searchTerm)}`, {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    },
                    signal: controller.signal
                });

                if (res.ok) {
                    const data = await res.json();
                    setSuggestions(data);
                }
            } catch (err) {
                if (err.name !== 'AbortError') console.error(err);
            }
        };

        fetchSuggestions();
        return () => controller.abort();
    }, [searchTerm]);

    useEffect(() => {
        const fetchUserData = async () => {
            const token = localStorage.getItem('token');
            if (!token) return;

            try {
                const res = await fetch('http://localhost:3002/payments/user-info', {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (res.ok) {
                    const userData = await res.json();
                    setCreditCard(userData.credit_card_number || '');
                    setVehiclePlate(userData.registration_number || '');
                }
            } catch (err) {
                console.error('Error fetching user info:', err);
            }
        };

        fetchUserData();
    }, []);

    const handleSelect = (location) => {
        setSearchTerm(location.name);
        setSelectedLocation(location);
        setSuggestions([]);
    };

    const handlePay = async () => {
        if (!selectedLocation || !creditCard || !vehiclePlate || !duration) {
            alert('Please fill in all fields.');
            return;
        }

        const token = localStorage.getItem('token');
        if (!token) return alert('Not authorized');

        try {
            const res = await fetch('http://localhost:3002/payments/calculate-and-pay', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    locationId: selectedLocation._id,
                    duration: parseInt(duration),
                    method: 'card'
                })
            });

            const data = await res.json();

            if (!res.ok) {
                return alert(`Error: ${data.message}`);
            }

            setPrice(data.calculated_price);
            alert(`Payment successful. Amount: ${data.calculated_price} ${data.unit}`);
        } catch (err) {
            console.error('Error submitting payment:', err);
        }
    };

    return (
        <div>
            <h2>Search Parking Location</h2>
            <input
                type="text"
                placeholder="Start typing name..."
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
            />
            {suggestions.length > 0 && (
                <ul>
                    {suggestions.map((location, idx) => (
                        <li key={idx} onClick={() => handleSelect(location)}>
                            {location.name}
                        </li>
                    ))}
                </ul>
            )}

            <div>
                <label htmlFor="creditCard">Credit Card</label><br />
                <input
                    type="text"
                    id="creditCard"
                    value={creditCard}
                    onChange={(e) => setCreditCard(e.target.value)}
                    placeholder="Enter your credit card number"
                />
            </div>

            <div>
                <label htmlFor="vehiclePlate">Vehicle Plate</label><br />
                <input
                    type="text"
                    id="vehiclePlate"
                    value={vehiclePlate}
                    onChange={(e) => setVehiclePlate(e.target.value)}
                    placeholder="Enter your license plate"
                />
            </div>

            <div>
                <label htmlFor="duration">Duration (hours, min 1)</label><br />
                <input
                    type="number"
                    id="duration"
                    min="1"
                    value={duration}
                    onChange={(e) => setDuration(e.target.value)}
                    placeholder="Enter duration"
                />
            </div>

            <button onClick={handlePay}>
                Calculate Payment
            </button>

            {price && (
                <div>
                    <strong>Total Price:</strong> {price}
                </div>
            )}
        </div>
    );
}

export default Payment;
