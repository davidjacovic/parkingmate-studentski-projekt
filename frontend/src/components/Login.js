// pages/Login.js
import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserContext } from '../userContext';

function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const { setUserContext } = useContext(UserContext);

    async function handleLogin(e) {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const res = await fetch('http://localhost:3002/users/login', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password }),
            });

            if (!res.ok) {
                const errRes = await res.json();
                setError(errRes.message || 'Prijava ni uspela');
                setLoading(false);
                return;
            }

            const data = await res.json();

            if (data && data.user && data.user._id) {
                localStorage.setItem('user', JSON.stringify(data.user));
                localStorage.setItem('token', data.token);

                // Pridobi in shrani čas poteka tokena
                const tokenPayload = JSON.parse(atob(data.token.split('.')[1]));
                const expiresAt = tokenPayload.exp * 1000; // milisekunde
                localStorage.setItem('tokenExpiresAt', expiresAt);

                setUserContext(data.user);

                // 👉 Preusmeritev glede na tip uporabnika
                if (data.user.user_type === 'admin') {
                    navigate('/admin');  // ustvari pot za admina
                } else {
                    navigate('/');
                }
            }

        } catch (err) {
            setError('Prijava ni uspela: ' + err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="login-container">
            <h2>Prijava</h2>
            <form onSubmit={handleLogin}>
                <div className="mb-3">
                    <label htmlFor="loginUsername">Uporabniško ime:</label>
                    <input
                        type="text"
                        id="loginUsername"
                        value={username}
                        onChange={e => setUsername(e.target.value)}
                        required
                    />
                </div>

                <div className="mb-3">
                    <label htmlFor="loginPassword">Geslo:</label>
                    <input
                        type="password"
                        id="loginPassword"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        required
                    />
                </div>

                {error && <div className="alert alert-danger">{error}</div>}

                <button type="submit" className="btn-primary" disabled={loading}>
                    {loading ? 'Prijavljam se...' : 'Prijava'}
                </button>
            </form>

            <div className="register-link">
                <span>Še nimate računa? </span>
                <a href="/register">Registrirajte se tukaj</a>
            </div>
        </div>
    );
}

export default Login;
