import React from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap  } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import userIconImg from '../assets/man-location.png';

// Ikonice
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

const userIcon = new L.Icon({
  iconUrl: userIconImg,
  iconSize: [40, 40],
  iconAnchor: [20, 40],
  popupAnchor: [0, -40],
});

// Ovdje ubaci CustomRefreshControl (iz prethodnog koraka)
function CustomRefreshControl({ onRefresh }) {
  const map = useMap();
  const [loading, setLoading] = React.useState(false);

  React.useEffect(() => {
    const control = L.control({ position: 'topright' });

    control.onAdd = function () {
      const div = L.DomUtil.create('div', 'leaflet-bar leaflet-control leaflet-control-custom');
      div.innerHTML = '🔄';
      div.title = 'Osveži lokacije';

      Object.assign(div.style, {
        backgroundColor: 'white',
        width: '34px',
        height: '34px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
        fontSize: '20px',
        boxShadow: '0 1px 4px rgba(0,0,0,0.4)',
        transition: 'transform 0.2s, background-color 0.3s',
      });

      div.onclick = async () => {
        div.style.transform = 'scale(0.9)';
        setLoading(true);
        try {
          await onRefresh();
        } finally {
          setTimeout(() => {
            div.style.transform = 'scale(1)';
            setLoading(false);
          }, 300);
        }
      };

      return div;
    };

    control.addTo(map);
    return () => map.removeControl(control);
  }, [map, onRefresh]);

  React.useEffect(() => {
    const btn = document.querySelector('.leaflet-control-custom');
    if (btn) {
      btn.innerHTML = loading ? '⏳' : '🔄';
      btn.title = loading ? 'Nalaganje...' : 'Osveži lokacije';
    }
  }, [loading]);

  return null;
}

const MapView = ({ userLocation, parkingLocations, nearestId, onRefresh }) => {
  const defaultCenter = userLocation
    ? [userLocation[1], userLocation[0]]
    : [46.0569, 14.5058]; // Ljubljana fallback

  return (
    <div style={{ height: '400px', width: '100%', margin: '2rem auto' }}>
      <MapContainer
        key={defaultCenter.join(',')}
        center={defaultCenter}
        zoom={15}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; OpenStreetMap'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {userLocation && (
          <Marker position={[userLocation[1], userLocation[0]]} icon={userIcon}>
            <Popup>Vaša lokacija</Popup>
          </Marker>
        )}

        {parkingLocations.map((loc) => (
          <Marker
            key={loc._id}
            position={[loc.location.coordinates[1], loc.location.coordinates[0]]}
            icon={
              loc._id === nearestId
                ? new L.Icon({
                    iconUrl: 'https://maps.google.com/mapfiles/ms/icons/green-dot.png',
                    iconSize: [25, 41],
                    iconAnchor: [12, 41],
                    popupAnchor: [0, -41],
                  })
                : new L.Icon.Default()
            }
          >
            <Popup>
              <div>
                <strong>{loc.name}</strong><br />
                {loc.address}<br />
                <a href={`/location/${loc._id}`}>Podrobnosti</a>
              </div>
            </Popup>
          </Marker>
        ))}

        {onRefresh && <CustomRefreshControl onRefresh={onRefresh} />}
      </MapContainer>
    </div>
  );
};

export default MapView;
