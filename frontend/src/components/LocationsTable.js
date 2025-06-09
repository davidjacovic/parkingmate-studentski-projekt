import React from 'react';
import { Link } from 'react-router-dom';

function LocationsTable({ locations, user }) {
  return (
    <table className="locations-table">
      <thead>
        <tr>
          <th rowSpan={2} className="sticky-col">Ulica / Naziv</th>
          <th colSpan={2}>Regularna mesta</th>
          <th colSpan={2}>Invalidska mesta</th>
          <th colSpan={2}>Električna mesta</th>
          <th colSpan={2}>Autobuska mesta</th>
        </tr>
        <tr>
          {[...Array(4)].map((_, i) => (
            <React.Fragment key={i}>
              <th>Prosto</th>
              <th>Na voljo</th>
            </React.Fragment>
          ))}
        </tr>
      </thead>
      <tbody>
        {locations.map(loc => (
          <tr key={loc._id}>
            <td className="sticky-col">
              <Link to={`/location/${loc._id}`} className="location-link">
                {loc.name}
              </Link>
              <div className="address">{loc.address}</div>
            </td>

            <td>{loc.available_regular_spots}</td>
            <td>{loc.total_regular_spots}</td>

            <td>{loc.available_invalid_spots}</td>
            <td>{loc.total_invalid_spots}</td>

            <td>{loc.available_electric_spots}</td>
            <td>{loc.total_electric_spots}</td>

            <td>{loc.available_bus_spots}</td>
            <td>{loc.total_bus_spots}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default LocationsTable;
