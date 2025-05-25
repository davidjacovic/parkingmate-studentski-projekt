import React, { useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserContext } from '../userContext';

function Header() {
    const { user, setUserContext } = useContext(UserContext);
    const navigate = useNavigate();

    function handleLogout() {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        setUserContext(null);
        navigate('/login');
    }

    return (
        <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px' }}>
            <h2 style={{ margin: 0 }}>
                <Link to="/" style={{ textDecoration: 'none', color: '#333' }}>
                    ParkingMate
                </Link>
            </h2>
            <nav style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                {user ? (
                    <>
                        <Link to="/profile" style={{ textDecoration: 'none', color: '#007bff' }}>Profile</Link>
                        <Link to="/payment" style={{ textDecoration: 'none', color: '#007bff' }}>
                            Pay Parking
                        </Link>
                        <Link to="/proximity" style={{ textDecoration: 'none', color: '#007bff' }}>Nearby parkings</Link>
                        <Link to="/coverage" style={{ textDecoration: 'none', color: '#007bff' }}>
                            Analiza pokrivenosti
                        </Link>

                        <button onClick={handleLogout} style={{ background: 'none', border: 'none', padding: 0, margin: 0, textDecoration: 'none', color: '#007bff', cursor: 'pointer', font: 'inherit' }}>
                            Logout
                        </button>

                    </>
                ) : (
                    <>
                        <Link to="/login">Login</Link>
                        <Link to="/register">Register</Link>
                    </>
                )}
            </nav>
        </header>
    );
}

export default Header;
