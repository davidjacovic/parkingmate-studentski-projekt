
import { BrowserRouter, Routes, Route, useNavigate } from 'react-router-dom';
import { UserContext } from "./userContext";
import { findNearestParking } from './utils/geoUtils';

import Login from "./components/Login";
import Register from "./components/Register";
import Logout from "./components/Logout";
import Homepage from "./components/Homepage";
import Header from "./components/Header";
import Profil from "./components/Profil";
import LocationDetails from "./components/LocationDetails";
import Payment from "./components/Payment";
import ProximitySearch from './components/ProximitySearch';
import CoverageAnalysis from './components/CoverageAnalysis';
import LocationsPage from './components/LocationsPage';
import { useTokenExpirationNotification } from './components/useTokenExpirationNotification';
import AdminHomepage from './components/AdminHomepage';
import { useEffect, useState } from 'react';
import { io } from 'socket.io-client';

import Reviews from './components/Reviews';

function App() {
    const [user, setUser] = useState(localStorage.user ? JSON.parse(localStorage.user) : null);
    const [notification, setNotification] = useState(null);
    // logout funkcija
    function logout() {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        localStorage.removeItem('tokenExpiresAt');
        setUser(null);
        window.location.href = '/login';  // redirect na login
    }
    useEffect(() => {
        if (!user) return;  // ako nije ulogovan, ne otvaraj socket

        const socket = io('http://localhost:3002');  // ili tvoj socket server URL

        // Slušaj event sa servera, npr. 'parking-expired'
        socket.on('parking-expired', ({ vehiclePlate }) => {
            setNotification(`Vreme za vozilo ${vehiclePlate} je isteklo!`);
        });

        return () => {
            socket.disconnect();
        };
    }, [user]);
    // extend sesije - refresovanje tokena (primer)
    async function extendSession() {
        const token = localStorage.getItem('token');
        try {
            const res = await fetch('http://localhost:3002/users/refresh-token', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                },
            });

            if (!res.ok) throw new Error('Failed to refresh token');

            const data = await res.json();

            localStorage.setItem('token', data.token);

            // Postavi novo vreme isteka tokena u localStorage
            const tokenPayload = JSON.parse(atob(data.token.split('.')[1]));
            const expiresAt = tokenPayload.exp * 1000;
            localStorage.setItem('tokenExpiresAt', expiresAt);

            alert('Session extended!');
        } catch (err) {
            alert('Could not extend session, please log in again.');
            logout();
        }
    }

    // koristi custom hook za praćenje isteka tokena
    const { showNotification, handleExtend, handleLogout } = useTokenExpirationNotification(extendSession, logout);

    // Parking primer podaci i ostalo
    const parkingSpots = [
        { id: 1, coordinates: [14.5058, 46.0569] },
        { id: 2, coordinates: [14.5065, 46.0575] },
    ];

    const [userLocation, setUserLocation] = useState([14.505, 46.056]);
    const nearestParkingId = findNearestParking(userLocation, parkingSpots);

    const updateUserData = (userInfo) => {
        localStorage.setItem("user", JSON.stringify(userInfo));
        setUser(userInfo);
    };

    return (
        <BrowserRouter>
            <UserContext.Provider value={{ user, setUserContext: updateUserData }}>
                <div className="App">
                    <Header />
                    <Routes>
                        {user?.user_type === 'admin' ? (
                            <>
                                <Route path="/" element={<AdminHomepage />} />
                                <Route path="/admin" element={<AdminHomepage />} />
                                <Route
                                    path="/userhome"
                                    element={
                                        <Homepage
                                            nearestParkingId={nearestParkingId}
                                            parkingSpots={parkingSpots}
                                            userLocation={userLocation}
                                            setUserLocation={setUserLocation}
                                        />
                                    }
                                />
                                <Route path="/location/:id" element={<LocationDetails />} />
                                <Route path="/login" element={<Login />} />
                                <Route path="/logout" element={<Logout />} />
                                {/* Ostale admin rute */}
                                <Route path="*" element={<AdminHomepage />} />
                            </>
                        ) : (
                            <>
                                <Route
                                    path="/"
                                    element={
                                        <Homepage
                                            nearestParkingId={nearestParkingId}
                                            parkingSpots={parkingSpots}
                                            userLocation={userLocation}
                                            setUserLocation={setUserLocation}
                                        />
                                    }
                                />
                                <Route path="/location/:id" element={<LocationDetails />} />
                                <Route path="/login" element={<Login />} />
                                <Route path="/register" element={<Register />} />
                                <Route path="/logout" element={<Logout />} />
                                <Route path="/profile" element={<Profil />} />
                                <Route path="/payment" element={<Payment />} />
                                <Route path="/locations/*" element={<LocationsPage />} />
                            </>
                        )}

                        {/* Rutu za Reviews staviš OVDE, izvan uslova — biće dostupna i adminu i useru */}
                        <Route path="/reviews" element={<Reviews />} />
                    </Routes>



                    {/* Notifikacija za isteka tokena */}
                    {showNotification && (
                        <div style={{
                            position: 'fixed',
                            bottom: 20,
                            right: 20,
                            backgroundColor: 'white',
                            border: '1px solid black',
                            padding: 20,
                            zIndex: 1000,
                            borderRadius: 5,
                            boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
                        }}>
                            <p>Your session will expire soon. Extend session?</p>
                            <button onClick={handleExtend} style={{ marginRight: 10 }}>Yes, extend</button>
                            <button onClick={handleLogout}>No, log me out</button>
                        </div>
                    )}

                    {notification && (
                        <div style={{
                            position: 'fixed',
                            bottom: 20,
                            right: 20,
                            backgroundColor: 'white',
                            border: '1px solid red',
                            padding: 15,
                            zIndex: 1000,
                            borderRadius: 5,
                            boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
                            color: 'red',
                            fontWeight: 'bold',
                        }}>
                            {notification}
                            <button onClick={() => setNotification(null)} style={{ marginLeft: 10 }}>X</button>
                        </div>
                    )}

                </div>
            </UserContext.Provider>
        </BrowserRouter>
    );
}

export default App;
