# Fajlovi za ML Servis - Task 3.3

## Pregled

Lista svih fajlova koji su kreirani ili modifikovani za ML servis za inferenciju parking slika.

---

## Novi fajlovi (kreirani)

### Python servis
- **`backend/ml_service/inference.py`**
  - Glavni Python servis koji koristi YOLO model
  - Detektuje parking mesta (empty/occupied)
  - Vraća koordinate i status parking mesta
  - Format: JSON output

### Node.js servis
- **`backend/ml_service/mlInferenceService.js`**
  - Node.js wrapper za Python servis
  - Poziva Python skriptu preko command line
  - Parsira JSON rezultate
  - Formatira rezultate za API
  - Loguje rezultate analize

### API Controller
- **`backend/controllers/mlAnalysisController.js`**
  - HTTP controller za ML analizu
  - Endpoint-i: `/api/ml/analyze`, `/api/ml/health`
  - Upravlja upload-om slika
  - Vraća formatirane rezultate

### API Routes
- **`backend/routes/mlRoutes.js`**
  - Express routes za ML servis
  - Definiše endpoint-e
  - Konfiguriše multer za file upload

### Test fajlovi
- **`backend/ml_service/test_ml_service.js`**
  - Test skripta za brzo testiranje servisa
  - Proverava zavisnosti, model, i analizira sliku

- **`backend/public/ml_test.html`**
  - HTML stranica za testiranje
  - Upload slike i prikaz rezultata
  - Vizualizacija bounding box-ova na slici

### Dokumentacija
- **`backend/ml_service/TASK_3.3_IMPLEMENTATION.md`**
  - Dokumentacija implementacije Task 3.3
  - Opis svih subtaskova (3.3.1, 3.3.2, 3.3.3, 3.3.4)
  - Objašnjenje funkcionalnosti

- **`backend/ml_service/SETUP_I_TESTIRANJE.md`**
  - Uputstva za setup i testiranje
  - Korak-po-korak instrukcije
  - Različiti načini pokretanja

- **`backend/ml_service/KAKO_TESTIRATI.md`**
  - Kratak vodič za testiranje
  - Brzi testovi i troubleshooting

### Python zavisnosti
- **`backend/ml_service/requirements.txt`**
  - Lista Python paketa potrebnih za ML servis
  - ultralytics, numpy, opencv-python, pillow, torch, torchvision

### Log folder
- **`backend/logs/.gitkeep`**
  - Placeholder fajl za logs folder
  - Log fajl `ml_analysis.log` se kreira automatski

---

## Modifikovani fajlovi

### Glavni backend fajl
- **`backend/app.js`**
  - Dodato: `const mlRouter = require('./routes/mlRoutes');`
  - Dodato: `app.use('/api/ml', mlRouter);`
  - Integracija ML routes u glavnu aplikaciju

---

## Model fajl

### YOLO model
- **`backend/models/last.pt`**
  - YOLO model za detekciju parking mesta
  - Detektuje: `empty` (class 0) i `occupied` (class 1)
  - Kopiran iz `C:\Users\keser\Downloads\vid\last.pt`

---

## Struktura foldera

```
backend/
├── ml_service/
│   ├── inference.py              # Python YOLO servis
│   ├── mlInferenceService.js     # Node.js wrapper
│   ├── requirements.txt          # Python zavisnosti
│   ├── test_ml_service.js        # Test skripta
│   ├── TASK_3.3_IMPLEMENTATION.md
│   ├── SETUP_I_TESTIRANJE.md
│   └── KAKO_TESTIRATI.md
│
├── controllers/
│   └── mlAnalysisController.js  # API controller
│
├── routes/
│   └── mlRoutes.js               # API routes
│
├── models/
│   └── last.pt                   # YOLO model
│
├── public/
│   └── ml_test.html              # HTML test stranica
│
├── logs/
│   └── ml_analysis.log           # Log fajl (kreira se automatski)
│
└── app.js                         # Modifikovan (dodata integracija)
```

---

## API Endpoint-i

### Health Check
- **GET** `/api/ml/health`
  - Proverava da li je ML servis spreman
  - Vraća status i informacije o modelu

### Analiza slike (upload)
- **POST** `/api/ml/analyze`
  - Upload slike preko `multipart/form-data`
  - Parametri: `image` (file), `confidenceThreshold` (opciono)
  - Vraća rezultate analize

### Analiza slike (putanja)
- **POST** `/api/ml/analyze-path`
  - Slanje putanje do slike u body-ju
  - Parametri: `imagePath`, `confidenceThreshold` (opciono)
  - Vraća rezultate analize

---

## Zavisnosti

### Python paketi
- `ultralytics` - YOLO model
- `numpy` - Numeričke operacije
- `opencv-python` - Image processing
- `pillow` - Image handling
- `torch` - PyTorch (za YOLO)
- `torchvision` - PyTorch vision utilities

### Node.js paketi
- `multer` - File upload handling (već postoji u projektu)
- Standardni Node.js moduli (fs, path, child_process)

---

## Kako pokrenuti

1. **Instaliraj Python zavisnosti:**
   ```bash
   cd backend/ml_service
   pip install -r requirements.txt
   ```

2. **Kopiraj model:**
   ```bash
   # Kopiraj last.pt u backend/models/
   ```

3. **Pokreni backend server:**
   ```bash
   cd backend
   npm start
   ```

4. **Testiraj:**
   - Browser: `http://localhost:3002/ml_test.html`
   - API: `http://localhost:3002/api/ml/health`

---

## Napomene

- Model fajl `last.pt` mora biti u `backend/models/` folderu
- Python mora biti instaliran i dostupan u PATH-u
- Log fajl se automatski kreira u `backend/logs/ml_analysis.log`
- Default confidence threshold je 0.3 (može se promeniti preko environment varijable `ML_MODEL_PATH`)

