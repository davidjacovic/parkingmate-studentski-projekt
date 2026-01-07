# Task 3.3.3 - Vraćanje koordinata parking mesta

## Implementirano

### Modifikovani fajlovi:

1. **backend/ml_service/inference.py**
   - ✅ Dodato `spots_with_coordinates` u rezultat
   - ✅ Svako parking mesto ima koordinate i status (slobodno/zauzeto)
   - ✅ Koordinate su u formatu: `[x_center, y_center, width, height]` (normalizovane 0-1)

2. **backend/ml_service/mlInferenceService.js**
   - ✅ Ažurirana `formatResult()` funkcija da vraća `spotsCoordinates`
   - ✅ `spotsCoordinates` je niz koordinata: `[[x_center, y_center, width, height], ...]`
   - ✅ Format je kompatibilan sa `parkingImageModel.js` šemom

3. **backend/controllers/mlAnalysisController.js**
   - ✅ Ažuriran komentar da uključuje 3.3.3

4. **backend/routes/mlRoutes.js**
   - ✅ Ažuriran komentar da uključuje 3.3.3

## Format koordinata

Koordinata su normalizovane (0-1):
- `x_center`: X koordinata centra parking mesta (0-1)
- `y_center`: Y koordinata centra parking mesta (0-1)
- `width`: Širina bounding box-a (0-1)
- `height`: Visina bounding box-a (0-1)

## Format odgovora

```json
{
  "success": true,
  "data": {
    "totalSpots": 10,
    "freeSpaces": 7,
    "occupiedSpaces": 3,
    "totalCars": 3,
    "spotsCoordinates": [
      [0.5, 0.3, 0.1, 0.15],
      [0.7, 0.3, 0.1, 0.15],
      [0.2, 0.5, 0.1, 0.15],
      ...
    ],
    "allDetections": [...],
    "metadata": {
      "confidence_threshold": 0.5,
      "iou_threshold": 0.3,
      "image_path": "..."
    }
  }
}
```

## Kompatibilnost sa bazom

Format `spotsCoordinates` je kompatibilan sa `parkingImageModel.js`:
```javascript
urvrvResult: {
  spotsCoordinates: [[Number]] // Dvodimenzionalni niz koordinata
}
```

Svaki element u `spotsCoordinates` je niz `[x_center, y_center, width, height]`.

## Napomene

- Koordinate su normalizovane (0-1) što omogućava skaliranje na bilo koju veličinu slike
- Redosled koordinata u `spotsCoordinates` odgovara redosledu parking mesta u `parking_spots`
- Koordinate se mogu koristiti za vizualizaciju parking mesta na slikama

