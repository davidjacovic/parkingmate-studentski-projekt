# Task 3.3.2 - Vraćanje broja slobodnih i zauzetih mesta

## Implementirano

### Modifikovani fajlovi:

1. **backend/ml_service/inference.py**
   - ✅ Dodata `calculate_iou()` funkcija za izračunavanje Intersection over Union
   - ✅ Dodata logika za određivanje zauzetih/slobodnih mesta
   - ✅ Dodat `iou_threshold` parametar (default: 0.3)
   - ✅ Vraća `free_spaces` i `occupied_spaces` u rezultatu

2. **backend/ml_service/mlInferenceService.js**
   - ✅ Dodat `iouThreshold` u konstruktor
   - ✅ Dodata `formatResult()` funkcija koja vraća `freeSpaces` i `occupiedSpaces`
   - ✅ Podrška za `iouThreshold` u opcijama

3. **backend/controllers/mlAnalysisController.js**
   - ✅ Koristi `formatResult()` za formatiranje odgovora
   - ✅ Podrška za `iouThreshold` parametar u request body-ju

## Algoritam za određivanje zauzetosti

1. Za svako parking mesto:
   - Izračunava IoU sa svim automobilima
   - Ako je IoU >= `iou_threshold` (default 0.3), mesto je zauzeto
   - Čuva indekse slobodnih i zauzetih mesta

2. Rezultat:
   - `free_spaces`: Broj slobodnih parking mesta
   - `occupied_spaces`: Broj zauzetih parking mesta

## Format odgovora

```json
{
  "success": true,
  "data": {
    "totalSpots": 10,
    "freeSpaces": 7,
    "occupiedSpaces": 3,
    "totalCars": 3,
    "allDetections": [...],
    "metadata": {
      "confidence_threshold": 0.5,
      "iou_threshold": 0.3,
      "image_path": "..."
    }
  }
}
```

## API Usage

```bash
# Sa custom iouThreshold
curl -X POST http://localhost:3002/api/ml/analyze \
  -F "image=@path/to/image.jpg" \
  -F "confidenceThreshold=0.5" \
  -F "iouThreshold=0.3"
```

## Napomene

- IoU threshold određuje koliko se parking mesto i automobil moraju preklapati da bi mesto bilo smatrano zauzetim
- Default vrednost: 0.3 (30% preklapanja)
- Može se prilagoditi preko `iouThreshold` parametra ili `ML_IOU_THRESHOLD` environment varijable

