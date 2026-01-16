# Kako testirati ML servis

## Brzi test (preporučeno) ⚡

**Najjednostavniji način:**

```bash
cd backend
node ml_service/test_ml_service.js
```

**Šta će se desiti:**
1. ✅ Proveri da li su Python zavisnosti instalirane
2. ✅ Proveri da li postoji `last.pt` model
3. ✅ Analizira sliku (`test_slika3.png`)
4. ✅ Prikaže rezultate (koliko parking mesta, koliko praznih, koliko zauzetih)
5. ✅ Loguje rezultate u `backend/logs/ml_analysis.log`

**Primer outputa:**
```
=== Test ML Servisa ===

1. Testiranje health check...
✓ Python zavisnosti su instalirane

2. Provera YOLO modela...
✓ Model pronađen: C:\...\backend\models\last.pt

3. Testiranje analize slike...
Analiziram sliku: C:\...\uploads\test_slika3.png
Koristim confidence threshold: 0.3
✓ Analiza uspešna!

Rezultati:
  - Ukupno parking mesta: 3
  - Slobodna mesta: 3
  - Zauzeta mesta: 0
  - Ukupno automobila: 0
```

---

## Test sa drugom slikom

Ako želiš da testiraš sa drugom slikom, promeni u `test_ml_service.js`:

```javascript
const testImagePath = path.join(__dirname, '..', 'uploads', 'TvojaSlika.png');
```

Ili direktno u terminalu:

```bash
cd backend
node -e "const ml = require('./ml_service/mlInferenceService'); const path = require('path'); ml.analyzeImage(path.join(__dirname, 'uploads', 'test_slika1.png'), {confidenceThreshold: 0.5}).then(r => console.log(JSON.stringify(ml.formatResult(r), null, 2))).catch(console.error)"
```

---

## Direktan test Python skripte

Ako želiš da testiraš direktno Python skriptu (bez Node.js):

```bash
cd backend
python ml_service/inference.py "uploads/test_slika1.png" "models/last.pt" 0.5
```

**Parametri:**
- `uploads/test_slika1.png` - putanja do slike
- `models/last.pt` - putanja do modela
- `0.5` - confidence threshold (0.0-1.0)

**Sa class-specific thresholds:**
```bash
python ml_service/inference.py "uploads/test_slika1.png" "models/last.pt" 0.5 0.4 0.6
```

**Parametri:**
- `0.5` - opšti confidence threshold
- `0.4` - threshold za prazna mesta (empty)
- `0.6` - threshold za zauzeta mesta (occupied)

---

## Test preko API-ja (sa serverom)

### 1. Pokreni backend server

```bash
cd backend
npm start
```

Server će se pokrenuti na `http://localhost:3002`

### 2. Test Health Check

**PowerShell:**
```powershell
Invoke-WebRequest -Uri "http://localhost:3002/api/ml/health" | Select-Object -ExpandProperty Content
```

**Browser:**
```
http://localhost:3002/api/ml/health
```

### 3. Analiza slike (sa putanjom)

**PowerShell:**
```powershell
$body = @{
    imagePath = "test_slika1.png"
    confidenceThreshold = 0.5
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:3002/api/ml/analyze-path" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body | Select-Object -ExpandProperty Content
```

**Sa class-specific thresholds:**
```powershell
$body = @{
    imagePath = "test_slika1.png"
    confidenceThreshold = 0.5
    emptyThreshold = 0.4
    occupiedThreshold = 0.6
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:3002/api/ml/analyze-path" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body | Select-Object -ExpandProperty Content
```

### 4. Analiza slike (sa upload-om)

Koristi **Postman** ili **Insomnia**:
- POST `http://localhost:3002/api/ml/analyze`
- Body: `form-data`
- Key: `image` (type: File)
- Value: izaberi sliku
- Opciono: dodaj parametre (`confidenceThreshold`, `emptyThreshold`, `occupiedThreshold`)

---

## Troubleshooting

### Problem: "Python dependencies not installed"

**Rešenje:**
```bash
cd backend/ml_service
pip install -r requirements.txt
```

### Problem: "Model file not found"

**Rešenje:**
1. Proveri da li `last.pt` postoji u `backend/models/last.pt`
2. Ili kopiraj model:
```powershell
Copy-Item "C:\Users\keser\Downloads\vid\last.pt" -Destination "backend\models\last.pt"
```

### Problem: "Test slika nije pronađena"

**Rešenje:**
- Stavi bilo koju sliku u `backend/uploads/` folder
- Promeni ime slike u `test_ml_service.js` (linija 37)

### Problem: Model ne prepoznaje dobro objekte

**Rešenje:**
- Smanji `confidenceThreshold` (npr. 0.3 ili 0.2) za više detekcija
- Koristi class-specific thresholds (`emptyThreshold`, `occupiedThreshold`)

---

## Provera logova

Nakon testiranja, proveri log fajl:

```powershell
Get-Content backend\logs\ml_analysis.log -Tail 10
```

Ili otvori fajl:
```
backend/logs/ml_analysis.log
```

---

## Primeri threshold-a

### Konzervativno (manje detekcija, ali sigurnije):
```javascript
{
    confidenceThreshold: 0.7
}
```

### Balans (default):
```javascript
{
    confidenceThreshold: 0.5
}
```

### Agresivno (više detekcija):
```javascript
{
    confidenceThreshold: 0.3
}
```

### Sa class-specific thresholds:
```javascript
{
    confidenceThreshold: 0.5,
    emptyThreshold: 0.4,      // Niži threshold za prazna mesta
    occupiedThreshold: 0.6    // Viši threshold za zauzeta mesta
}
```

