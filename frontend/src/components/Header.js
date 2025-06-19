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
        <header className="header">
            <h2>
                <Link
                    to={user?.user_type === 'admin' ? '/userhome' : '/'}
                >
                    ParkingMate
                </Link>
            </h2>

            <nav>
                {user ? (
                    <>
                        {user.user_type === 'admin' && (
                            <>
                                <Link to="/">Administrator Domov</Link>
                            </>
                        )}
                        {user.user_type === 'user' && (
                            <>
                                <Link to="/profile">Profil uporabnika</Link>
                                <Link to="/payment">Plačaj parking</Link>
                            </>
                        )}
                        {/* Dugme dostupno i adminu i useru */}
                        <Link to="/reviews">Mnenja</Link>

                        <button onClick={handleLogout}>Odjava</button>
                    </>
                ) : (
                    <>
                        <Link to="/login">Prijava</Link>
                        <Link to="/register">Registracija</Link>
                    </>
                )}
            </nav>
        </header>
    );
}

export default Header;
