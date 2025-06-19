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
                setError('Registracija ni uspela: ' + (result.message || 'Neznana napaka'));
            }
        } catch (err) {
            setError('Registracija ni uspela: ' + err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
      <div className="register-container">
        <h2>Registracija</h2>
        <form onSubmit={handleRegister}>
          <div className="mb-3">
            <label htmlFor="username" className="form-label">Uporabniško ime</label>
            <input
              type="text"
              id="username"
              className="form-control"
              placeholder="Vnesite uporabniško ime"
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="mb-3">
            <label htmlFor="email" className="form-label">Elektronska pošta</label>
            <input
              type="email"
              id="email"
              className="form-control"
              placeholder="Vnesite vaš email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="mb-3">
            <label htmlFor="password" className="form-label">Geslo</label>
            <input
              type="password"
              id="password"
              className="form-control"
              placeholder="Izberite geslo"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
            />
          </div>

          <div className="mb-3">
            <label htmlFor="creditCard" className="form-label">Številka kreditne kartice</label>
            <input
              type="text"
              id="creditCard"
              className="form-control"
              placeholder="Vnesite številko kreditne kartice (neobvezno)"
              value={creditCard}
              onChange={e => setCreditCard(e.target.value)}
            />
          </div>

          <div className="mb-3">
            <label htmlFor="registrationNumber" className="form-label">Registrska tablica</label>
            <input
              type="text"
              id="registrationNumber"
              className="form-control"
              placeholder="Vnesite registrsko številko vozila"
              value={registrationNumber}
              onChange={e => setRegistrationNumber(e.target.value)}
              required
            />
          </div>

          {error && <div className="alert alert-danger">{error}</div>}

          <button type="submit" className="btn btn-primary w-100" disabled={loading}>
            {loading ? 'Registracija poteka...' : 'Registriraj se'}
          </button>
        </form>

        <div className="text-center mt-3">
          <span>Že imate račun? </span>
          <a href="/login">Prijavite se tukaj</a>
        </div>
      </div>
    );
}

export default Register;
