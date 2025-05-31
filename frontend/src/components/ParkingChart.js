import React, { useEffect, useState } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import axios from 'axios';

const ParkingAvailabilityChart = ({ locationId }) => {
  const [logData, setLogData] = useState([]);
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  // New state for filters
  const [filters, setFilters] = useState({
    regular: true,
    invalid: true,
    bus: true,
  });

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

  // Toggle filter checkbox handler
  const toggleFilter = (type) => {
    setFilters(prev => ({ ...prev, [type]: !prev[type] }));
  };

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

      {/* Filter checkboxes */}
      <div style={{ marginBottom: '1rem' }}>
        <label>
          <input
            type="checkbox"
            checked={filters.regular}
            onChange={() => toggleFilter('regular')}
          /> Regular
        </label>{' '}
        <label>
          <input
            type="checkbox"
            checked={filters.invalid}
            onChange={() => toggleFilter('invalid')}
          /> Invalid
        </label>{' '}
        <label>
          <input
            type="checkbox"
            checked={filters.bus}
            onChange={() => toggleFilter('bus')}
          /> Bus
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
            {filters.regular && <Line type="monotone" dataKey="regular" stroke="#8884d8" name="Regular" />}
            {filters.invalid && <Line type="monotone" dataKey="invalid" stroke="#82ca9d" name="Invalid" />}
            {filters.bus && <Line type="monotone" dataKey="bus" stroke="#ffc658" name="Bus" />}
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default ParkingAvailabilityChart;
