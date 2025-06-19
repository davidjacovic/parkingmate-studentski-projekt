// components/Logout.js
import { useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserContext } from '../userContext';

function Logout() {
    const { setUserContext } = useContext(UserContext);
    const navigate = useNavigate();

    useEffect(() => {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        setUserContext(null);
        navigate('/login');
    }, [navigate, setUserContext]);

    return null;
}

export default Logout;
