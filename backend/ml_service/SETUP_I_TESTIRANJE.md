# Setup i Testiranje ML Servisa

## Korak 1: Instalacija Python zavisnosti

```bash
cd backend/ml_service
pip install -r requirements.txt
```

**Napomena:** Ako imaš problema, probaj:
```bash
python -m pip install -r requirements.txt
```

**Ili ako koristiš virtual environment:**
```bash
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

---

## Korak 2: Postavljanje YOLO modela

Kopiraj `best.pt` fajl u `backend/models/` folder:

**PowerShell:**
```powershell
Copy-Item "C:\Users\keser\Downloads\best.pt" -Destination "backend\models\best.pt"
```

**Ili ručno:**
- Kopiraj `best.pt` iz `C:\Users\keser\Downloads\`
- Paste u `backend\models\best.pt`

---

## Korak 3: Provera da li sve radi

### Test 1: Health Check (bez servera)

```bash
cd backend
node -e "const ml = require('./ml_service/mlInferenceService'); ml.checkDependencies().then(() => console.log('✓ OK')).catch(e => console.error('✗', e.message))"
```

### Test 2: Direktan test Python skripte

```bash
cd backend
python ml_service/inference.py "uploads/test_slika1.png" "models/best.pt" 0.5 0.3
```

Ovo bi trebalo da ispiše JSON sa rezultatima.

---

## Načini pokretanja i testiranja

### Način 1: Brzi test (bez servera) ⚡

**Najbrži način da testiraš servis:**

```bash
cd backend
node ml_service/test_ml_service.js
```

**Šta radi:**
- Proverava Python zavisnosti
- Proverava YOLO model
- Analizira sliku (`test_slika2.png`)
- Prikazuje rezultate
- Loguje rezultate

**Rezultat:**
```
=== Test ML Servisa ===

1. Testiranje health check...
✓ Python zavisnosti su instalirane

2. Provera YOLO modela...
✓ Model pronađen: ...

3. Testiranje analize slike...
✓ Analiza uspešna!

Rezultati:
  - Ukupno parking mesta: 3
  - Slobodna mesta: 1
  - Zauzeta mesta: 2
  - Ukupno automobila: 2
```

---

### Način 2: Test sa prilagodljivim threshold-ima

```bash
cd backend
node ml_service/test_with_threshold.js [confidence] [iou] [image_name]
```

**Primeri:**
```bash
# Sa threshold 0.3
node ml_service/test_with_threshold.js 0.3 0.3 test_slika1.png

# Sa threshold 0.25 (više detekcija)
node ml_service/test_with_threshold.js 0.25 0.25 test_slika1.png

# Sa optimalnim postavkama za test_slika1.png
node ml_service/test_with_threshold.js 0.1 0.3 test_slika1.png
```

**Parametri:**
- `confidence` - Confidence threshold (0.0-1.0)
- `iou` - IoU threshold (0.0-1.0)
- `image_name` - Ime slike iz `uploads/` foldera

---

### Način 3: Test sa optimalnim postavkama

```bash
cd backend
node ml_service/final_test.js
```

Koristi optimalne postavke za `test_slika1.png` (3 mesta, 2 automobila).

---

### Način 4: Preko API-ja (sa serverom) 🌐

#### 4.1. Pokreni backend server

```bash
cd backend
npm start
```

Server će se pokrenuti na `http://localhost:3002`

#### 4.2. Test Health Check

**Browser:**
```
http://localhost:3002/api/ml/health
```

**PowerShell:**
```powershell
Invoke-WebRequest -Uri "http://localhost:3002/api/ml/health" | Select-Object -ExpandProperty Content
```

**Očekivani odgovor:**
```json
{
  "success": true,
  "message": "ML service is ready",
  "modelPath": "...",
  "pythonExecutable": "python"
}
```

#### 4.3. Analiza slike (sa putanjom)

**PowerShell:**
```powershell
$body = @{
    imagePath = "test_slika1.png"
    confidenceThreshold = 0.1
    carThreshold = 0.12
    parkingThreshold = 0.55
    iouThreshold = 0.3
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:3002/api/ml/analyze-path" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body | Select-Object -ExpandProperty Content
```

**Ili sa curl** (ako je instaliran):
```bash
curl -X POST http://localhost:3002/api/ml/analyze-path ^
  -H "Content-Type: application/json" ^
  -d "{\"imagePath\": \"test_slika1.png\", \"confidenceThreshold\": 0.1, \"carThreshold\": 0.12, \"parkingThreshold\": 0.55, \"iouThreshold\": 0.3}"
```

#### 4.4. Analiza slike (sa upload-om)

**Postman/Insomnia:**
- POST `http://localhost:3002/api/ml/analyze`
- Body: `form-data`
- Key: `image` (type: File)
- Value: izaberi sliku
- Opciono: dodaj parametre (`confidenceThreshold`, `carThreshold`, `parkingThreshold`, `iouThreshold`)

---

### Način 5: Debug opcije

#### 5.1. Proveri sve detekcije

```bash
cd backend
node ml_service/debug_detections.js
```

Prikazuje sve detekcije sa niskim threshold-om i daje preporuke.

#### 5.2. Proveri IoU između automobila i parking mesta

```bash
cd backend
node ml_service/check_occupancy.js
```

Prikazuje IoU vrednosti i preporuke za threshold-e.

---

## Provera logova

Nakon testiranja, proveri log fajl:

**PowerShell:**
```powershell
Get-Content backend\logs\ml_analysis.log -Tail 10
```

**Ili otvori fajl:**
```
backend/logs/ml_analysis.log
```

---

## Troubleshooting

### Problem: "Python dependencies not installed"

**Rešenje:**
```bash
cd backend/ml_service
pip install -r requirements.txt
```

**Ili:**
```bash
python -m pip install -r requirements.txt
```

### Problem: "Model file not found"

**Rešenje:**
1. Proveri da li `best.pt` postoji u `backend/models/best.pt`
2. Ili postavi environment varijablu u `.env`:
```env
ML_MODEL_PATH=C:\Users\keser\Downloads\best.pt
```

### Problem: "python3: command not found" (Windows)

**Rešenje:**
Postavi u `.env` fajlu:
```env
PYTHON_EXECUTABLE=python
```

### Problem: Backend ne radi

**Proveri:**
1. Da li postoji `.env` fajl sa MongoDB connection string-om
2. Da li je port 3002 slobodan
3. Da li su Node.js zavisnosti instalirane (`npm install`)

### Problem: Model ne prepoznaje dobro objekte

**Rešenje:**
- Smanji `confidenceThreshold` (0.2-0.3) za više detekcija
- Koristi class-specific thresholds (`carThreshold`, `parkingThreshold`)
- Testiraj sa različitim slikama

---

## Preporučene postavke

### Za opštu upotrebu:
```javascript
{
    confidenceThreshold: 0.3,
    iouThreshold: 0.3
}
```

### Za test_slika1.png (3 mesta, 2 automobila):
```javascript
{
    confidenceThreshold: 0.1,
    carThreshold: 0.12,
    parkingThreshold: 0.55,
    iouThreshold: 0.3
}
```

---

## Brzi start checklist

- [ ] Instalirane Python zavisnosti (`pip install -r requirements.txt`)
- [ ] YOLO model kopiran u `backend/models/best.pt`
- [ ] Testirao sa `node ml_service/test_ml_service.js`
- [ ] Proverio logove u `backend/logs/ml_analysis.log`
- [ ] (Opciono) Pokrenuo backend server i testirao API

---

## Sledeći koraci

Nakon što je sve testirano i radi, možeš nastaviti sa:
- **Task 4.1**: Integracija ML analize u postojeći backend
- **Task 4.2**: Povrat rezultata aplikaciji
- **Task 5**: Vizualizacija digitalnog dvojčka

