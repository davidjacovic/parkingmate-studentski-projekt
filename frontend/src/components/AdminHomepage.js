import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

function AdminHomepage() {
    const [users, setUsers] = useState([]);
    const [loadingUsers, setLoadingUsers] = useState(true);
    const [errorUsers, setErrorUsers] = useState(null);
    const [deletingUserId, setDeletingUserId] = useState(null);
    const [selectedUser, setSelectedUser] = useState(null);
    const [paymentHistory, setPaymentHistory] = useState([]);
    const [loadingPayments, setLoadingPayments] = useState(false);
    const [errorPayments, setErrorPayments] = useState(null);
    const [parkingLocations, setParkingLocations] = useState({});
    const [usersPerDay, setUsersPerDay] = useState([]);
    const [loadingUsersPerDay, setLoadingUsersPerDay] = useState(false);
    const [errorUsersPerDay, setErrorUsersPerDay] = useState(null);

    useEffect(() => {
        fetchUsers();
        fetchUsersPerDay();
    }, []);

    async function fetchUsers() {
        try {
            setLoadingUsers(true);
            setErrorUsers(null);
            const token = localStorage.getItem('token');
            const response = await axios.get('http://localhost:3002/users/all-users', {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Dohvaćeni korisnici:', response.data); // <-- DODAJ OVO
            setUsers(response.data);
            setUsers(response.data);
        } catch (err) {
            setErrorUsers(err.response?.data?.message || 'Greška pri dohvatanju korisnika.');
        } finally {
            setLoadingUsers(false);
        }
    }

    async function handleDeleteUser(userId) {
        if (!window.confirm('Da li ste sigurni da želite da obrišete ovog korisnika?')) return;
        try {
            setDeletingUserId(userId);
            const token = localStorage.getItem('token');
            await axios.delete(`http://localhost:3002/users/${userId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            await fetchUsers();
            if (selectedUser?._id === userId) setSelectedUser(null);
            setPaymentHistory([]);
        } catch (err) {
            alert(err.response?.data?.message || 'Greška pri brisanju korisnika.');
        } finally {
            setDeletingUserId(null);
        }
    }

    async function fetchPaymentHistory(userId) {
        try {
            setLoadingPayments(true);
            setErrorPayments(null);
            setPaymentHistory([]);
            const token = localStorage.getItem('token');
            const response = await axios.get(`http://localhost:3002/payments/history/${userId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setPaymentHistory(response.data);
        } catch (err) {
            setErrorPayments(err.response?.data?.message || 'Greška pri dohvatanju istorije plaćanja.');
        } finally {
            setLoadingPayments(false);
        }
    }

    async function fetchParkingLocationById(id) {
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get(`http://localhost:3002/parkingLocations/${id}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setParkingLocations(prev => ({ ...prev, [id]: response.data }));
        } catch (err) {
            console.error('Error fetching parking location:', err);
        }
    }
    useEffect(() => {
        // Kad se učita paymentHistory, za svaki payment tražimo parking lokaciju ako je nemamo
        paymentHistory.forEach(payment => {
            const id = typeof payment.parking_location === 'string' ? payment.parking_location : payment.parking_location?._id;
            if (id && !parkingLocations[id]) {
                fetchParkingLocationById(id);
            }
        });
    }, [paymentHistory]);

    async function fetchUsersPerDay() {
        try {
            setLoadingUsersPerDay(true);
            setErrorUsersPerDay(null);
            const token = localStorage.getItem('token');
            const response = await axios.get('http://localhost:3002/users/users-per-day', {
                headers: { Authorization: `Bearer ${token}` },
            });
            // Pretvori odgovor u format koji recharts može lako da koristi
            const formattedData = response.data.map(item => ({
                date: item._id,
                count: item.count,
            }));
            setUsersPerDay(formattedData);
        } catch (err) {
            setErrorUsersPerDay(err.response?.data?.message || 'Greška pri dohvatanju podataka o korisnicima po danu.');
        } finally {
            setLoadingUsersPerDay(false);
        }
    }
    return (
        <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>

            {/* Grafikon korisnika po danu */}
            <div style={{ marginBottom: '40px' }}>
                <h2>Registracije uporabnikov po dnevih</h2>
                {loadingUsersPerDay && <p>Nalagam podatke...</p>}
                {errorUsersPerDay && <p style={{ color: 'red' }}>{errorUsersPerDay}</p>}
                {!loadingUsersPerDay && usersPerDay.length > 0 && (
                    <ResponsiveContainer width="100%" height={300}>
                        <LineChart
                            data={usersPerDay}
                            margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                        >
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="date" />
                            <YAxis />
                            <Tooltip />
                            <Line type="monotone" dataKey="count" stroke="#82ca9d" activeDot={{ r: 8 }} />
                        </LineChart>
                    </ResponsiveContainer>
                )}
                {!loadingUsersPerDay && usersPerDay.length === 0 && <p>Nema podataka za prikaz.</p>}
            </div>

            {loadingUsers && <p>Učitavanje korisnika...</p>}
            {errorUsers && <p style={{ color: 'red' }}>{errorUsers}</p>}

            {!loadingUsers && !errorUsers && (
                <div style={{ display: 'flex', gap: '40px' }}>
                    {/* Lista korisnika */}
                    <div style={{ flex: 1 }}>
                        <h2>Lista uporabnika</h2>
                        {users.length === 0 ? (
                            <p>Ni registrovanih uporabnika.</p>
                        ) : (
                            <ul
                                style={{
                                    listStyle: 'none',
                                    padding: 0,
                                    maxHeight: '500px',
                                    overflowY: 'auto',
                                    border: '1px solid #ccc',
                                    borderRadius: '5px',
                                }}
                            >
                                {users.map(user => (
                                    <li
                                        key={user._id}
                                        onClick={() => {
                                            setSelectedUser(user);
                                            fetchPaymentHistory(user._id);
                                        }}
                                        style={{
                                            padding: '10px',
                                            marginBottom: '4px',
                                            backgroundColor: selectedUser?._id === user._id ? '#f0f0f0' : 'white',
                                            cursor: 'pointer',
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center',
                                            borderRadius: '3px',
                                            border: '1px solid #ddd',
                                        }}
                                    >
                                        <span>
                                            {user.username} ({user.email})
                                        </span>
                                        <button
                                            onClick={e => {
                                                e.stopPropagation();
                                                handleDeleteUser(user._id);
                                            }}
                                            disabled={deletingUserId === user._id}
                                            style={{
                                                cursor: 'pointer',
                                                backgroundColor: 'transparent',
                                                border: 'none',
                                                color: 'red',
                                                fontWeight: 'bold',
                                                fontSize: '16px',
                                            }}
                                            title="Obriši korisnika"
                                        >
                                            {deletingUserId === user._id ? '...' : '🗑️'}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>

                    {/* Istorija plaćanja */}
                    <div style={{ flex: 2 }}>
                        {selectedUser ? (
                            <>
                                <h2>
                                    Zgodovina plačil za <strong>{selectedUser.username}</strong>
                                </h2>

                                {loadingPayments && <p>Nalagam zgodovino plačil...</p>}
                                {errorPayments && <p style={{ color: 'red' }}>{errorPayments}</p>}

                                {!loadingPayments && !errorPayments && paymentHistory.length === 0 && (
                                    <p>Ni podatkov o plačilih za tega uporabnika.</p>
                                )}


                                {!loadingPayments && paymentHistory.length > 0 && (
                                    <>
                                        <table
                                            style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px' }}
                                        >
                                            <thead>
                                                <tr style={{ backgroundColor: '#eee' }}>
                                                    <th style={{ border: '1px solid #ccc', padding: '8px' }}>Datum</th>
                                                    <th style={{ border: '1px solid #ccc', padding: '8px' }}>Znesek</th>
                                                    <th style={{ border: '1px solid #ccc', padding: '8px' }}>
                                                        Parking lokacija
                                                    </th>
                                                    <th style={{ border: '1px solid #ccc', padding: '8px' }}>
                                                        Registarska tablica
                                                    </th>
                                                    <th style={{ border: '1px solid #ccc', padding: '8px' }}>Metod plačanja</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {paymentHistory.map(payment => (
                                                    <tr key={payment._id} style={{ borderBottom: '1px solid #ddd' }}>
                                                        <td style={{ border: '1px solid #ccc', padding: '8px' }}>
                                                            {new Date(payment.date).toLocaleDateString()}
                                                        </td>
                                                        <td style={{ border: '1px solid #ccc', padding: '8px' }}>
                                                            {payment.amount && payment.amount.$numberDecimal
                                                                ? payment.amount.$numberDecimal
                                                                : payment.amount}
                                                        </td>
                                                        <td>
                                                            {(() => {
                                                                const id =
                                                                    typeof payment.parking_location === 'string'
                                                                        ? payment.parking_location
                                                                        : payment.parking_location?._id;
                                                                const location = parkingLocations[id];
                                                                if (location) {
                                                                    return `${location.name} ${location.address ? `- ${location.address}` : ''
                                                                        }`;
                                                                }
                                                                return 'Nalaganje...';
                                                            })()}
                                                        </td>
                                                        <td style={{ border: '1px solid #ccc', padding: '8px' }}>
                                                            {payment.vehicle_plate || '-'}
                                                        </td>
                                                        <td style={{ border: '1px solid #ccc', padding: '8px' }}>
                                                            {payment.method || '-'}
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>

                                        <h3 style={{ marginTop: '40px' }}>Graf plačanja</h3>
                                        <ResponsiveContainer width="100%" height={300}>
                                            <LineChart
                                                data={paymentHistory.map(payment => ({
                                                    date: new Date(payment.date).toLocaleDateString(),
                                                    amount: payment.amount?.$numberDecimal
                                                        ? parseFloat(payment.amount.$numberDecimal)
                                                        : parseFloat(payment.amount),
                                                }))}
                                                margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                                            >
                                                <CartesianGrid strokeDasharray="3 3" />
                                                <XAxis dataKey="date" />
                                                <YAxis />
                                                <Tooltip />
                                                <Line
                                                    type="monotone"
                                                    dataKey="amount"
                                                    stroke="#8884d8"
                                                    activeDot={{ r: 8 }}
                                                />
                                            </LineChart>
                                        </ResponsiveContainer>
                                    </>
                                )}
                            </>
                        ) : (
                            <p>Izberite uporabnika, da si ogledate zgodovino plačil.</p>
                        )}
                    </div>
                </div>
            )}
        </div>
    );

}

export default AdminHomepage;
