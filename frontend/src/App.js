import { useState } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { UserContext } from "./userContext";
import { findNearestParking } from './utils/geoUtils';  // <-- import funkcije

import Login from "./components/Login";
import Register from "./components/Register";
import Logout from "./components/Logout";
import Homepage from "./components/Homepage";
import Header from "./components/Header";
import Profile from "./components/Profile";
import LocationDetails from "./components/LocationDetails";
import Payment from "./components/Payment";
import ProximitySearch from './components/ProximitySearch';
import CoverageAnalysis from './components/CoverageAnalysis';

function App() {
    const [user, setUser] = useState(localStorage.user ? JSON.parse(localStorage.user) : null);

    // Primer statičkih podataka o parking mestima (ovo možeš kasnije povući iz API-ja)
    const parkingSpots = [
        { id: 1, coordinates: [14.5058, 46.0569] },
        { id: 2, coordinates: [14.5065, 46.0575] },
        // ...dodaj ostala parkirališta
    ];

    // Primer korisničke lokacije (ovde možeš koristiti geolokaciju ili user input)
    const [userLocation, setUserLocation] = useState([14.505, 46.056]);

    // Izračunaj najbliže parking mesto na osnovu lokacije korisnika
    const nearestParkingId = findNearestParking(userLocation, parkingSpots);

    const updateUserData = (userInfo) => {
        localStorage.setItem("user", JSON.stringify(userInfo));
        setUser(userInfo);
    };

    return (
        <BrowserRouter>
            <UserContext.Provider value={{
                user: user,
                setUserContext: updateUserData
            }}>
                <div className="App">
                    <Header /> 
                    <Routes>
                        {/* Prosledi nearestParkingId i ostale podatke Homepage komponenti */}
                        <Route path="/" element={
                            <Homepage 
                                nearestParkingId={nearestParkingId} 
                                parkingSpots={parkingSpots} 
                                userLocation={userLocation}
                                setUserLocation={setUserLocation} // ako želiš da menjaš lokaciju
                            />} 
                        />
                        <Route path="/location/:id" element={<LocationDetails />} />
                        <Route path="/login" element={<Login />} />
                        <Route path="/register" element={<Register />} />
                        <Route path="/logout" element={<Logout />} />
                        <Route path="/profile" element={<Profile />} />
                        <Route path="/payment" element={<Payment />} />
                        <Route path="/proximity" element={<ProximitySearch />} />
                        <Route path="/coverage" element={<CoverageAnalysis />} />
                    </Routes>
                </div>
            </UserContext.Provider>
        </BrowserRouter>
    );
}

export default App;
