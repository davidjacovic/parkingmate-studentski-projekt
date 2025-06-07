import { useEffect, useState } from 'react';

export function useTokenExpirationNotification(onExtendSession, onLogout) {
    const [showNotification, setShowNotification] = useState(false);

    useEffect(() => {
        const expiresAt = localStorage.getItem('tokenExpiresAt');
        if (!expiresAt) return;

        const expiresAtTime = parseInt(expiresAt, 10);
        const now = Date.now();

        // Kad treba da se prikaze notifikacija (npr. 5 minuta pre isteka)
        const notifyTime = expiresAtTime - 5 * 60 * 1000; 

        if (now > expiresAtTime) {
            // Token je istekao, automatski logout
            onLogout();
            return;
        }

        const timeToNotify = notifyTime - now;

        if (timeToNotify <= 0) {
            setShowNotification(true);
        } else {
            const notifyTimeout = setTimeout(() => {
                setShowNotification(true);
            }, timeToNotify);

            return () => clearTimeout(notifyTimeout);
        }
    }, [onLogout]);

    function handleExtend() {
        setShowNotification(false);
        onExtendSession();
    }

    function handleLogout() {
        setShowNotification(false);
        onLogout();
    }

    return {
        showNotification,
        handleExtend,
        handleLogout,
    };
}
