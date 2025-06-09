// TopParkingLocationsChart.js
import React, { useEffect, useState } from 'react'; 
import axios from 'axios';
import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from 'recharts';

function TopParkingLocationsChart() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchTopLocations() {
      try {
        const token = localStorage.getItem('token');
        const response = await axios.get('http://localhost:3002/payments/top-parking-locations', {
          headers: { Authorization: `Bearer ${token}` },
        });
        setData(response.data);
      } catch (error) {
        console.error('Greška pri dohvatanju top lokacija:', error);
      } finally {
        setLoading(false);
      }
    }
    fetchTopLocations();
  }, []);

  if (loading) return <p>Učitavanje...</p>;

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} margin={{ top: 20, right: 30, bottom: 20, left: 20 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" interval={0} angle={-45} textAnchor="end" height={60} />
        <YAxis />
        <Tooltip />
        <Bar dataKey="usageCount" fill="#8884d8" />
      </BarChart>
    </ResponsiveContainer>
  );
}

export default TopParkingLocationsChart;
