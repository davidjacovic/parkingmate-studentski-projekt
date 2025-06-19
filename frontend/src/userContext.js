// userContext.js
import React, { createContext, useState, useEffect } from 'react';

export const UserContext = createContext({
  user: null,
  setUserContext: () => {}
});

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
  }, []);

  return (
    <UserContext.Provider value={{ user, setUserContext: setUser }}>
      {children}
    </UserContext.Provider>
  );
};
