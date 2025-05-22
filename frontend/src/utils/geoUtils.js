import * as turf from '@turf/turf';

export function findNearestParking(userLocation, parkingSpots) {
  const userPoint = turf.point(userLocation);
  const points = parkingSpots.map(p => turf.point(p.coordinates, { id: p.id }));
  const nearest = turf.nearestPoint(userPoint, turf.featureCollection(points));
  return nearest.properties.id;
}
