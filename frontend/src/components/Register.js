import { useState } from 'react';

function Register() {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [creditCard, setCreditCard] = useState('');
    const [registrationNumber, setRegistrationNumber] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    async function handleRegister(e) {
        e.preventDefault();
        setError('');
        setLoading(true);
        const data = {
            username,
            email,
            password,
            registration_number: registrationNumber,
        };

        if (creditCard.trim() !== '') {
            data.credit_card_number = creditCard.trim();
        }


        try {
            const res = await fetch('http://localhost:3002/users', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data),
            });

            const result = await res.json();
            if (res.ok) {
                window.location.href = '/login';
            } else {
                setError('Registration failed: ' + (result.message || 'Unknown error'));
            }
        } catch (err) {
            setError('Registration failed: ' + err.message);
        } finally {
            setLoading(false);
        }
    }


    return (
        <div className="container mt-5">
            <h2 className="text-center mb-4">Register</h2>
            <form onSubmit={handleRegister}>
                <div className="mb-3">
                    <label htmlFor="username" className="form-label">Username</label>
                    <input
                        type="text"
                        id="username"
                        className="form-control"
                        placeholder="Enter your username"
                        value={username}
                        onChange={e => setUsername(e.target.value)}
                        required
                    />
                </div>

                <div className="mb-3">
                    <label htmlFor="email" className="form-label">Email</label>
                    <input
                        type="email"
                        id="email"
                        className="form-control"
                        placeholder="Enter your email"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        required
                    />
                </div>

                <div className="mb-3">
                    <label htmlFor="password" className="form-label">Password</label>
                    <input
                        type="password"
                        id="password"
                        className="form-control"
                        placeholder="Choose a password"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        required
                    />
                </div>

                <div className="mb-3">
                    <label htmlFor="creditCard" className="form-label">Credit Card Number</label>
                    <input
                        type="text"
                        id="creditCard"
                        className="form-control"
                        placeholder="Enter your credit card number (optional)"
                        value={creditCard}
                        onChange={e => setCreditCard(e.target.value)}
                    />
                </div>

                <div className="mb-3">
                    <label htmlFor="registrationNumber" className="form-label">Registration Plates</label>
                    <input
                        type="text"
                        id="registrationNumber"
                        className="form-control"
                        placeholder="Enter your vehicle registration number"
                        value={registrationNumber}
                        onChange={e => setRegistrationNumber(e.target.value)}
                        required
                    />
                </div>

                {error && <div className="alert alert-danger">{error}</div>}

                <button type="submit" className="btn btn-primary w-100" disabled={loading}>
                    {loading ? 'Registering...' : 'Register'}
                </button>
            </form>

            <div className="text-center mt-3">
                <span>Already have an account? </span>
                <a href="/login">Log in here</a>
            </div>

        </div>
    );
}

export default Register;
