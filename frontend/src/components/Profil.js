import React, { useEffect, useState } from 'react';

function Profile() {
    const [userData, setUserData] = useState(null);
    const [error, setError] = useState('');
    const [editField, setEditField] = useState(null);
    const [fieldValue, setFieldValue] = useState('');
    const [editVehicleField, setEditVehicleField] = useState(null);
    const [vehicleFieldValue, setVehicleFieldValue] = useState('');

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
                    <span onClick={() => {
                        setEditField(fieldName);
                        setFieldValue(value || '');
                    }}>
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
                    <span onClick={() => {
                        setEditVehicleField(fieldName);
                        setVehicleFieldValue(value || '');
                    }}>
                        ✏️
                    </span>
                </>
            )}
        </p>
    );

    return (
        <div>
            <h2>User Profile</h2>
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
        </div>
    );
}

export default Profile;
