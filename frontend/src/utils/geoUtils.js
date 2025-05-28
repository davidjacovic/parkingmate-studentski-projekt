import * as turf from '@turf/turf';

export function findNearestParking(userLocation, parkingSpots) {
  if (!Array.isArray(userLocation) || userLocation.length !== 2 || userLocation.some(c => typeof c !== 'number' || isNaN(c))) {
    throw new Error('Invalid userLocation coordinates');
  }

  const userPoint = turf.point(userLocation);

  const points = parkingSpots.map(p => {
    if (!Array.isArray(p.coordinates) || p.coordinates.length !== 2) {
      console.error('Invalid coordinates array:', p.coordinates, 'for parking spot:', p);
      throw new Error('Coordinates must be an array of two numbers');
    }

    if (p.coordinates.some(coord => typeof coord !== 'number' || isNaN(coord))) {
      console.error('Coordinates contain invalid number:', p.coordinates, 'for parking spot:', p);
      throw new Error('Coordinates must contain valid numbers');
    }

    return turf.point(p.coordinates, { id: p.id });
  });

  const nearest = turf.nearestPoint(userPoint, turf.featureCollection(points));
  return nearest.properties.id;
}
