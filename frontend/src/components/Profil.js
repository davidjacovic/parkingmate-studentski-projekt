import React, { useEffect, useState } from 'react';
import {
    LineChart, Line, PieChart, Pie, Cell, Legend,
    CartesianGrid, XAxis, YAxis, Tooltip, ResponsiveContainer
} from 'recharts';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

function Profile() {
    const [userData, setUserData] = useState(null);
    const [error, setError] = useState('');
    const [editField, setEditField] = useState(null);
    const [fieldValue, setFieldValue] = useState('');
    const [editVehicleField, setEditVehicleField] = useState(null);
    const [vehicleFieldValue, setVehicleFieldValue] = useState('');
    const [avatarFile, setAvatarFile] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [payments, setPayments] = useState([]);
    const [loadingPayments, setLoadingPayments] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            setError('You must be logged in to view this page.');
            return;
        }

        fetch('http://localhost:3002/users/me', {
            headers: { Authorization: `Bearer ${token}` },
        })
            .then(async (res) => {
                if (!res.ok) {
                    const msg = await res.text();
                    throw new Error(msg || 'Failed to load user data');
                }
                return res.json();
            })
            .then((data) => setUserData(data))
            .catch(() => setError('Could not fetch profile.'));
    }, []);

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) return;

        fetch('http://localhost:3002/payments/history', {
            headers: { Authorization: `Bearer ${token}` },
        })
            .then((res) => {
                if (!res.ok) throw new Error('Greška pri učitavanju uplata');
                return res.json();
            })
            .then((data) => {
                setPayments(data);
                setLoadingPayments(false);
            })
            .catch(() => {
                setPayments([]);
                setLoadingPayments(false);
            });
    }, []);

    const getAmountHistoryData = () =>
        payments
            .map((p) => ({
                date: new Date(p.date).toLocaleDateString(),
                amount: parseFloat(p.amount?.$numberDecimal || 0),
            }))
            .reverse();

    const getMethodDistribution = () => {
        const methodCount = {};
        payments.forEach((p) => {
            methodCount[p.method] = (methodCount[p.method] || 0) + 1;
        });
        return Object.entries(methodCount).map(([name, value]) => ({ name, value }));
    };

    const handleAvatarUpload = async () => {
        if (!avatarFile) return alert('Izaberi fajl za upload');

        setUploading(true);
        const formData = new FormData();
        formData.append('avatar', avatarFile);

        try {
            const token = localStorage.getItem('token');
            const res = await fetch('http://localhost:3002/users/upload-avatar', {
                method: 'POST',
                headers: { Authorization: `Bearer ${token}` },
                body: formData,
            });

            if (!res.ok) throw new Error('Upload nije uspeo');

            const data = await res.json();
            setUserData((prev) => ({ ...prev, avatar: data.avatar }));
            setAvatarFile(null);
            alert('Avatar uspešno postavljen!');
        } catch {
            alert('Greška pri uploadu avatara');
        } finally {
            setUploading(false);
        }
    };

    const handleFieldSave = async () => {
        if (!fieldValue || fieldValue === userData[editField]) {
            setEditField(null);
            return;
        }

        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:3002/users/update`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({ [editField]: fieldValue }),
            });

            if (!response.ok) throw new Error('Failed to update field');

            const updatedUser = await response.json();
            setUserData((prev) => ({ ...prev, ...updatedUser }));
            setEditField(null);
        } catch {
            alert('Greška pri čuvanju izmene.');
        }
    };

    const handleVehicleFieldSave = async () => {
        if (!vehicleFieldValue || vehicleFieldValue === userData.vehicle?.[editVehicleField]) {
            setEditVehicleField(null);
            return;
        }

        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:3002/vehicles/${userData.vehicle._id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({ [editVehicleField]: vehicleFieldValue }),
            });

            if (!response.ok) throw new Error('Failed to update vehicle');

            const updatedVehicle = await response.json();
            setUserData((prev) => ({
                ...prev,
                vehicle: { ...prev.vehicle, ...updatedVehicle },
            }));
            setEditVehicleField(null);
        } catch {
            alert('Greška pri ažuriranju vozila.');
        }
    };

    if (error) return <p>{error}</p>;
    if (!userData) return <p>Loading...</p>;

    const renderEditableField = (label, fieldName, value) => (
        <p style={{ fontSize: '1.1rem', marginBottom: '0.8rem' }}>
            <strong>{label}:</strong>{' '}
            {editField === fieldName ? (
                <input
                    value={fieldValue}
                    onChange={(e) => setFieldValue(e.target.value)}
                    onBlur={handleFieldSave}
                    autoFocus
                    style={{ fontSize: '1rem', padding: '4px', width: '100%' }}
                />
            ) : (
                <>
                    {value || 'Ni vneseno'}{' '}
                    <span
                        style={{ cursor: 'pointer', color: '#007bff', marginLeft: 8 }}
                        onClick={() => {
                            setEditField(fieldName);
                            setFieldValue(value || '');
                        }}
                    >
                        ✏️
                    </span>
                </>
            )}
        </p>
    );

    const maskCard = (cardNumber) => {
        if (!cardNumber || cardNumber.length !== 16) return cardNumber;
        return cardNumber.slice(0, 4) + '**********' + cardNumber.slice(-2);
    };

    const renderEditableVehicleField = (label, fieldName, value) => (
        <p style={{ fontSize: '1.1rem', marginBottom: '0.8rem' }}>
            <strong>{label}:</strong>{' '}
            {editVehicleField === fieldName ? (
                <input
                    value={vehicleFieldValue}
                    onChange={(e) => setVehicleFieldValue(e.target.value)}
                    onBlur={handleVehicleFieldSave}
                    autoFocus
                    style={{ fontSize: '1rem', padding: '4px', width: '100%' }}
                />
            ) : (
                <>
                    {value || 'Ni vneseno'}{' '}
                    <span
                        style={{ cursor: 'pointer', color: '#007bff', marginLeft: 8 }}
                        onClick={() => {
                            setEditVehicleField(fieldName);
                            setVehicleFieldValue(value || '');
                        }}
                    >
                        ✏️
                    </span>
                </>
            )}
        </p>
    );
    return (
    <div style={{ maxWidth: 1100, margin: '2rem auto', padding: '1rem' }}>
        <h2 style={{ fontSize: '2.2rem', marginBottom: '2rem', textAlign: 'center' }}>Uporabniški profil</h2>
        <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>

            {/* Leva kolona - uporabniške informacije + linijski graf spodaj */}
            <div style={{
                flex: '1 1 400px',
                minWidth: 320,
                maxWidth: 320,           // fiksna širina, da grafi ne "bežijo"
                display: 'flex',
                flexDirection: 'column',
                padding: '0 1rem',       // malo notranjega prostora levo/desno, po želji
                boxSizing: 'border-box', // da padding ne premika širine preko 320px
            }}>
                <div>
                    {/* Avatar */}
                    {userData.avatar ? (
                        <img
                            src={`http://localhost:3002${userData.avatar}`}
                            alt={`${userData.username}'s avatar`}
                            style={{ width: 120, height: 120, objectFit: 'cover', borderRadius: '50%', marginBottom: '1rem' }}
                        />
                    ) : (
                        <p style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Avatar ni nastavljen</p>
                    )}

                    <label
                        htmlFor="avatar-upload"
                        style={{
                            cursor: 'pointer',
                            color: '#007bff',
                            textDecoration: 'underline',
                            fontSize: '1.1rem',
                            display: 'inline-block',
                            marginBottom: '0.5rem',
                        }}
                    >
                        Izberi avatar
                    </label>
                    {avatarFile && (
                        <button
                            onClick={handleAvatarUpload}
                            disabled={uploading}
                            style={{
                                marginLeft: '0.5rem',
                                padding: '0.3rem 0.8rem',
                                fontSize: '1rem',
                                cursor: 'pointer',
                            }}
                        >
                            {uploading ? 'Nalaganje...' : 'Nastavi avatar'}
                        </button>
                    )}
                    {avatarFile && (
                        <div style={{ marginTop: '1rem' }}>
                            <p style={{ fontSize: '1rem' }}>Predogled slike:</p>
                            <img
                                src={URL.createObjectURL(avatarFile)}
                                alt="Preview"
                                style={{ width: 120, height: 120, objectFit: 'cover', borderRadius: '50%' }}
                            />
                        </div>
                    )}
                    <input
                        id="avatar-upload"
                        type="file"
                        accept="image/*"
                        style={{ display: 'none' }}
                        onChange={(e) => setAvatarFile(e.target.files[0])}
                    />

                    {/* Spremenljiva polja */}
                    {renderEditableField('Uporabniško ime', 'username', userData.username)}
                    {renderEditableField('Email', 'email', userData.email)}
                    {renderEditableField('Telefon', 'phone_number', userData.phone_number)}
                    {renderEditableField('Kartica', 'credit_card_number', maskCard(userData.credit_card_number))}

                    <p style={{ fontSize: '1.1rem' }}>
                        <strong>Ustvarjen:</strong> {new Date(userData.created_at).toLocaleDateString()}
                    </p>
                    <p style={{ fontSize: '1.1rem', marginBottom: '2rem' }}>
                        <strong>Zadnja posodobitev:</strong> {new Date(userData.updated_at).toLocaleDateString()}
                    </p>

                    <h3 style={{ marginBottom: '1rem' }}>Informacije o vozilu</h3>
                    {renderEditableVehicleField('Model', 'model', userData.vehicle?.model)}
                    {renderEditableVehicleField('Registracija', 'registration_number', userData.vehicle?.registration_number)}
                    {renderEditableVehicleField('Vrsta', 'vehicle_type', userData.vehicle?.vehicle_type)}
                    <p style={{ fontSize: '1.1rem' }}>
                        <strong>Ustvarjeno:</strong>{' '}
                        {userData.vehicle?.created ? new Date(userData.vehicle.created).toLocaleDateString() : 'Ni vnešeno'}
                    </p>
                    <p style={{ fontSize: '1.1rem', marginBottom: '2rem' }}>
                        <strong>Spremenjeno:</strong>{' '}
                        {userData.vehicle?.modified ? new Date(userData.vehicle.modified).toLocaleDateString() : 'Ni vnešeno'}
                    </p>
                </div>

                {/* Graf spodaj uporabniških podatkov */}
                <div style={{ marginTop: '2rem', width: '100%' }}>
                    <h4 style={{ textAlign: 'center', marginBottom: '1rem' }}>Znesek po datumu</h4>
                    <ResponsiveContainer width="100%" height={250}>
                        <LineChart data={getAmountHistoryData()}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="date" />
                            <YAxis />
                            <Tooltip />
                            <Legend />
                            <Line type="monotone" dataKey="amount" stroke="#8884d8" />
                        </LineChart>
                    </ResponsiveContainer>
                </div>

                <h4 style={{ textAlign: 'center', marginTop: '2rem', marginBottom: '1rem' }}>
                    Razdelitev metod plačanja
                </h4>
                <div style={{ width: '100%' }}>
                    <ResponsiveContainer width="100%" height={250}>
                        <PieChart>
                            <Pie
                                data={getMethodDistribution()}
                                dataKey="value"
                                nameKey="name"
                                cx="50%"
                                cy="50%"
                                outerRadius={80}
                                fill="#8884d8"
                                label
                            >
                                {getMethodDistribution().map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                ))}
                            </Pie>
                            <Tooltip />
                            <Legend />
                        </PieChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Desna kolona - tabela plačil in pie chart */}
            <div style={{ flex: '1 1 600px', minWidth: 300 }}>
                <h3 style={{ textAlign: 'center', marginBottom: '1rem' }}>Zgodovina plačil</h3>
                {loadingPayments ? (
                    <p>Nalaganje plačil...</p>
                ) : payments.length === 0 ? (
                    <p>Ni podatkov o plačilih.</p>
                ) : (
                    <>
                        <table
                            style={{
                                width: '100%',
                                borderCollapse: 'collapse',
                                marginBottom: '2rem',
                                fontSize: '1rem',
                            }}
                        >
                            <thead>
                                <tr style={{ backgroundColor: '#f0f0f0' }}>
                                    <th style={{ border: '1px solid #ddd', padding: '8px' }}>Datum</th>
                                    <th style={{ border: '1px solid #ddd', padding: '8px' }}>Metoda</th>
                                    <th style={{ border: '1px solid #ddd', padding: '8px' }}>Znesek</th>
                                </tr>
                            </thead>
                            <tbody>
                                {payments.map(({ _id, date, method, amount }) => (
                                    <tr key={_id}>
                                        <td style={{ border: '1px solid #ddd', padding: '8px' }}>
                                            {new Date(date).toLocaleDateString()}
                                        </td>
                                        <td style={{ border: '1px solid #ddd', padding: '8px' }}>{method}</td>
                                        <td style={{ border: '1px solid #ddd', padding: '8px' }}>
                                            {parseFloat(amount?.$numberDecimal || 0).toFixed(2)} EUR
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </>
                )}
            </div>
        </div>
    </div>
);


}

export default Profile;
