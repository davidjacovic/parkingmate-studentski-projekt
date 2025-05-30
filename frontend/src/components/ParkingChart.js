import React, { useEffect, useState } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import axios from 'axios';

const ParkingAvailabilityChart = ({ locationId }) => {
  const [logData, setLogData] = useState([]);
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  useEffect(() => {
    const fetchLogs = async () => {
      try {
        const params = new URLSearchParams();
        if (fromDate) params.append('from', fromDate);
        if (toDate) params.append('to', toDate);

        const res = await axios.get(`http://localhost:3002/parkingLocations/${locationId}/logs?${params.toString()}`);
        setLogData(res.data);
      } catch (err) {
        console.error(err);
      }
    };

    if (locationId) {
      fetchLogs();
    }
  }, [locationId, fromDate, toDate]);

  const formattedData = logData.map(entry => ({
    time: new Date(entry.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    regular: entry.available_regular_spots,
    invalid: entry.available_invalid_spots,
    bus: entry.available_bus_spots,
  }));

  return (
    <div>
      <div style={{ marginBottom: '1rem' }}>
        <label>
          From:{" "}
          <input
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
        </label>
        {" "}
        <label>
          To:{" "}
          <input
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
        </label>
      </div>

      <div style={{ width: '100%', height: 300 }}>
        <ResponsiveContainer>
          <LineChart data={formattedData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="regular" stroke="#8884d8" name="Regular" />
            <Line type="monotone" dataKey="invalid" stroke="#82ca9d" name="Invalid" />
            <Line type="monotone" dataKey="bus" stroke="#ffc658" name="Bus" />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default ParkingAvailabilityChart;
