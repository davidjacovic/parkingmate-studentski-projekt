import React, { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import {
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  Legend
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
            headers: {
                Authorization: `Bearer ${token}`,
            },
        })
            .then(async (res) => {
                if (!res.ok) {
                    const msg = await res.text();
                    throw new Error(msg || 'Failed to load user data');
                }
                return res.json();
            })
            .then((data) => {
                setUserData(data);
            })
            .catch((err) => {
                console.error(err);
                setError('Could not fetch profile.');
            });
    }, []);
    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) return;

        fetch('http://localhost:3002/payments/history', {
            headers: { Authorization: `Bearer ${token}` },
        })
            .then(res => {
                if (!res.ok) throw new Error('Greška pri učitavanju uplata');
                return res.json();
            })
            .then(data => {
                setPayments(data);
                setLoadingPayments(false);
            })
            .catch(err => {
                console.error('[Profile] Greska pri dohvatanju uplata:', err);
                setPayments([]);
                setLoadingPayments(false);
            });
    }, []);
 const getAmountHistoryData = () => {
    return payments.map(p => ({
      date: new Date(p.date).toLocaleDateString(),
      amount: parseFloat(p.amount?.$numberDecimal || 0)
    })).reverse();
  };

  const getMethodDistribution = () => {
    const methodCount = {};
    payments.forEach(p => {
      methodCount[p.method] = (methodCount[p.method] || 0) + 1;
    });
    return Object.entries(methodCount).map(([method, count]) => ({ name: method, value: count }));
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
                headers: {
                    Authorization: `Bearer ${token}`,
                },
                body: formData,
            });

            if (!res.ok) throw new Error('Upload nije uspeo');

            const data = await res.json();
            setUserData(prev => ({ ...prev, avatar: data.avatar }));
            setAvatarFile(null);
            alert('Avatar uspešno postavljen!');

        } catch (err) {
            console.error(err);
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
                body: JSON.stringify({
                    [editField]: fieldValue,
                }),
            });

            if (!response.ok) throw new Error('Failed to update field');

            const updatedUser = await response.json();
            setUserData((prev) => ({ ...prev, ...updatedUser }));
            setEditField(null);
        } catch (err) {
            console.error(err);
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
                body: JSON.stringify({
                    [editVehicleField]: vehicleFieldValue,
                }),
            });
            if (!response.ok) throw new Error('Failed to update vehicle');
            const updatedVehicle = await response.json();
            setUserData((prev) => ({
                ...prev,
                vehicle: {
                    ...prev.vehicle,
                    ...updatedVehicle,
                },
            }));
            setEditVehicleField(null);
        } catch (err) {
            console.error(err);
            alert('Greška pri ažuriranju vozila.');
        }
    };

    if (error) {
        return <p>{error}</p>;
    }

    if (!userData) {
        return <p>Loading...</p>;
    }

    const renderEditableField = (label, fieldName, value) => (
        <p>
            <strong>{label}:</strong>{' '}
            {editField === fieldName ? (
                <input
                    value={fieldValue}
                    onChange={(e) => setFieldValue(e.target.value)}
                    onBlur={handleFieldSave}
                    autoFocus
                />
            ) : (
                <>
                    {value || 'Nije uneto'}{' '}
                    <span
                        style={{ cursor: 'pointer' }}
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
        return cardNumber.slice(0, 4) + '**********' + cardNumber.slice(-3);
    };

    const renderEditableVehicleField = (label, fieldName, value) => (
        <p>
            <strong>{label}:</strong>{' '}
            {editVehicleField === fieldName ? (
                <input
                    value={vehicleFieldValue}
                    onChange={(e) => setVehicleFieldValue(e.target.value)}
                    onBlur={handleVehicleFieldSave}
                    autoFocus
                />
            ) : (
                <>
                    {value || 'Nije uneto'}{' '}
                    <span
                        style={{ cursor: 'pointer' }}
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
    // Grupisanje sati po danu iz payments
    const getDailyUsageData = () => {
        const dailyData = {};

        payments.forEach(p => {
            const day = new Date(p.date).toLocaleDateString(); // format npr. "7.6.2025."
            const duration = Number(p.duration) || 0;

            if (dailyData[day]) {
                dailyData[day] += duration;
            } else {
                dailyData[day] = duration;
            }
        });

        // Konvertuj u niz objekata za grafikon
        return Object.entries(dailyData).map(([day, hours]) => ({
            day,
            hours,
        }));
    };


    return (
        <div>
            <h2>User Profile</h2>
            {/* Prikaz avatara */}
            {userData.avatar ? (
                <img
                    src={`http://localhost:3002${userData.avatar}`}
                    alt={`${userData.username}'s avatar`}
                    style={{ width: 100, height: 100, borderRadius: '50%', objectFit: 'cover' }}
                />

            ) : (
                <p>No avatar set</p>
            )}
            {/* Upload avatar */}
            <label htmlFor="avatar-upload" style={{ cursor: 'pointer', color: 'blue', textDecoration: 'underline' }}>
                Izaberi avatar
            </label>
            {avatarFile && (
                <button onClick={handleAvatarUpload} disabled={uploading}>
                    {uploading ? 'Učitavanje...' : 'Postavi Avatar'}
                </button>
            )}
            {avatarFile && (
                <div style={{ marginTop: '10px' }}>
                    <p>Pregled slike:</p>
                    <img
                        src={URL.createObjectURL(avatarFile)}
                        alt="Preview"
                        style={{ width: 100, height: 100, borderRadius: '50%', objectFit: 'cover' }}
                    />
                </div>
            )}


            <input
                id="avatar-upload"
                type="file"
                accept="image/*"
                style={{ display: 'none' }} // sakriva input da koristiš labelu kao dugme
                onChange={(e) => setAvatarFile(e.target.files[0])}
            />

            {renderEditableField('Username', 'username', userData.username)}
            {renderEditableField('Email', 'email', userData.email)}
            {renderEditableField('Phone', 'phone_number', userData.phone_number)}
            {renderEditableField('Card', 'credit_card_number', maskCard(userData.credit_card_number))}

            <p><strong>Role:</strong> {userData.user_type}</p>
            <p><strong>Created:</strong> {new Date(userData.created_at).toLocaleDateString()}</p>
            <p><strong>Last Updated:</strong> {new Date(userData.updated_at).toLocaleDateString()}</p>

            <h3>Vehicle Information</h3>
            {renderEditableVehicleField('Model', 'model', userData.vehicle?.model)}
            {renderEditableVehicleField('Registration', 'registration_number', userData.vehicle?.registration_number)}
            {renderEditableVehicleField('Type', 'vehicle_type', userData.vehicle?.vehicle_type)}
            <p><strong>Created:</strong> {userData.vehicle?.created ? new Date(userData.vehicle.created).toLocaleDateString() : 'Nije uneto'}</p>
            <p><strong>Modified:</strong> {userData.vehicle?.modified ? new Date(userData.vehicle.modified).toLocaleDateString() : 'Nije uneto'}</p>
            <h3>Istorija uplata</h3>
            {loadingPayments ? (
                <p>Učitavanje uplata...</p>
            ) : payments.length === 0 ? (
                <p>Nema zabeleženih uplata.</p>
            ) : (
                <table border="1" cellPadding="6" style={{ borderCollapse: 'collapse', marginTop: '10px' }}>
                    <thead>
                        <tr>
                            <th>Datum</th>
                            <th>Iznos</th>
                            <th>Metod</th>
                            <th>Trajanje</th>
                            <th>Registracija</th>
                        </tr>
                    </thead>
                    <tbody>
                        {payments.map((payment) => (
                            <tr key={payment._id}>
                                <td>{new Date(payment.date).toLocaleString()}</td>
                                <td>{parseFloat(payment.amount?.$numberDecimal || 0).toFixed(2)}</td>
                                <td>{payment.method}</td>
                                <td>{payment.duration}</td>
                                <td>{payment.vehicle_plate}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}

            <h3>Istorija uplata</h3>
      {loadingPayments ? <p>Učitavanje uplata...</p> : (
        <>
          

          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={getAmountHistoryData()}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="amount" stroke="#82ca9d" />
            </LineChart>
          </ResponsiveContainer>

          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={getMethodDistribution()}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                outerRadius={80}
                label
              >
                {getMethodDistribution().map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </>
      )}
    </div>
    );
}

export default Profile;
