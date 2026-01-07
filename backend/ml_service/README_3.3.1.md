# Task 3.3.1 - Kreiranje servisa za analizu slike

## Opis

Osnovni ML servis za analizu parking slika koristeći YOLO model. Servis omogućava:
- Učitavanje YOLO modela (`best.pt`)
- Pokretanje inferencije na parking slikama
- Detekciju automobila (class 0) i parking mesta (class 1)
- Vraćanje osnovnih rezultata detekcije

## Struktura fajlova

```
backend/
├── ml_service/
│   ├── inference.py              # Python YOLO inference servis
│   ├── mlInferenceService.js     # Node.js wrapper servis
│   └── requirements.txt           # Python zavisnosti
├── controllers/
│   └── mlAnalysisController.js   # Controller za ML analizu
├── routes/
│   └── mlRoutes.js                # API rute za ML servis
└── models/
    └── best.pt                    # YOLO model (treba kopirati)
```

## Instalacija

### 1. Instaliraj Python zavisnosti

```bash
cd backend/ml_service
pip install -r requirements.txt
```

### 2. Postavi YOLO model

Kopiraj `best.pt` fajl u `backend/models/best.pt`:

```bash
# Windows PowerShell
Copy-Item "C:\Users\keser\Downloads\best.pt" -Destination "backend\models\best.pt"
```

Ili ručno kopiraj fajl.

## API Endpoints

### 1. Health Check
```
GET /api/ml/health
```

Proverava da li je ML servis spreman.

### 2. Analiza slike (upload)
```
POST /api/ml/analyze
Content-Type: multipart/form-data

Body:
- image: (file) - Slika za analizu
- confidenceThreshold: (optional) - Prag za confidence (default: 0.5)
```

### 3. Analiza slike (putanja)
```
POST /api/ml/analyze-path
Content-Type: application/json

Body:
{
  "imagePath": "1766948891545_parking_1766948888138.jpg",
  "confidenceThreshold": 0.5
}
```

## Format rezultata

```json
{
  "success": true,
  "data": {
    "success": true,
    "total_spots": 10,
    "total_cars": 3,
    "all_detections": [
      {
        "class_id": 0,
        "class_name": "car",
        "confidence": 0.85,
        "bbox": {
          "x_center": 0.5,
          "y_center": 0.3,
          "width": 0.1,
          "height": 0.15
        }
      },
      ...
    ],
    "cars": [...],
    "parking_spots": [...],
    "analysis_metadata": {
      "confidence_threshold": 0.5,
      "image_path": "..."
    }
  }
}
```

## Testiranje

```bash
# Health check
curl http://localhost:3002/api/ml/health

# Analiza slike
curl -X POST http://localhost:3002/api/ml/analyze \
  -F "image=@path/to/your/image.jpg"
```

## Napomene

- Python servis mora biti dostupan u sistemu
- Model fajl (`best.pt`) mora biti na putanji `backend/models/best.pt`
- Za Windows, možda treba postaviti `PYTHON_EXECUTABLE=python` u `.env` fajlu

