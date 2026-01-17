# EPIK 3.2: Implementacija zapisa dogodkov v blockchain

## Pregled

Implementacija zapisa dogodkov (eventov) v blockchain sistem ParkingMate aplikacije.

## Instalacija

### 1. Instaliraj axios dependency

```bash
cd backend
npm install axios
```

### 2. Konfiguracija environment varijabli

Dodaj u `backend/.env` fajl:

```env
# Blockchain servis URL (default: http://localhost:5024)
BLOCKCHAIN_SERVICE_URL=http://localhost:5024

# Worker konfiguracija (opciono)
BLOCKCHAIN_WORKER_ENABLED=true
BLOCKCHAIN_WORKER_INTERVAL_MS=30000
BLOCKCHAIN_WORKER_BATCH_SIZE=10
```

## Pokretanje

### 1. Pokreni blockchain servis

```bash
cd BLOCKCHAIN/ParkingMate.BlockchainServis
dotnet run
```

Blockchain servis će raditi na: `http://localhost:5024`

### 2. Pokreni backend server

```bash
cd backend
npm start
```

Worker će se automatski pokrenuti pri pokretanju servera.

## Testiranje

### 1. Kreiraj Event

```bash
curl -X POST http://localhost:3002/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Test Event",
    "message": "Test message",
    "location": "46.0569,14.5058",
    "eventType": "PARKING_FULL"
  }'
```

### 2. Proveri status worker-a

```bash
curl http://localhost:3002/api/blockchain/worker/status
```

### 3. Ručno pokreni procesiranje (ako je potrebno)

```bash
curl -X POST http://localhost:3002/api/blockchain/process
```

### 4. Proveri da li je Event zabeležen v blockchain

```bash
curl http://localhost:3002/api/events/<event_id>
```

Event bi trebao imati:
- `status: "BLOCKCHAIN_RECORDED"`
- `blockchainHash: "000abc..."`
- `blockchainTimestamp: Date`

### 5. Proveri blockchain lanac

```bash
curl http://localhost:5024/api/blockchain
```

## Novi fajlovi

### Servisi

- `backend/services/blockchainService.js` - Komunikacija sa blockchain API-jem
- `backend/services/blockchainWorker.js` - Background worker za procesiranje eventov

### Kontroleri i Routes

- `backend/controllers/blockchainController.js` - Kontroler za blockchain operacije
- `backend/routes/blockchainRoutes.js` - Routes za blockchain API

### Dokumentacija

- `backend/docs/EPIK_3_2_IMPLEMENTACIJA_BLOCKCHAIN_ZAPISA.md` - Detaljna dokumentacija

## API Endpoints

### POST /api/blockchain/process

Ručno pokreće procesiranje svih PENDING eventov.

### GET /api/blockchain/worker/status

Dohvata status worker-a i blockchain servisa.

### GET /api/blockchain/chain

Dohvata trenutno stanje blockchain lanca.

### GET /api/blockchain/validate

Validira blockchain lanac.

## Troubleshooting

### Worker ne radi

1. Proveri da li je blockchain servis pokrenut:
   ```bash
   curl http://localhost:5024/health
   ```

2. Proveri status worker-a:
   ```bash
   curl http://localhost:3002/api/blockchain/worker/status
   ```

3. Proveri logove u console-u backend servera.

### Eventi ostaju PENDING

1. Proveri da li je blockchain servis dostupan
2. Proveri da li je worker omogočen (`BLOCKCHAIN_WORKER_ENABLED=true`)
3. Ručno pokreni procesiranje: `POST /api/blockchain/process`

### Greške pri mining-u

1. Proveri da li blockchain servis radi
2. Proveri da li je mining već u toku (409 Conflict) - sačekaj malo
3. Proveri logove blockchain servisa

## Napomene

- Worker automatski procesira PENDING evente na intervalu (default: 30 sekundi)
- Eventi se procesiraju po redosledu stvaranja (FIFO)
- Worker nastavlja sa radom čak i ako neki event ne uspe
- Worker se automatski zaustavlja pri graceful shutdown servera

---

**Versija**: 1.0  
**EPIK**: 3.2 - Implementacija zapisa dogodkov v blockchain

