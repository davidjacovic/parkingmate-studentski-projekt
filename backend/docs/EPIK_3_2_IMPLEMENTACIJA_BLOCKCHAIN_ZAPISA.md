# EPIK 3.2: Implementacija zapisa dogodkov v blockchain

## Pregled

Ovaj dokument opisuje implementacijo zapisa dogodkov v blockchain sistem ParkingMate aplikacije.

## Arhitektura

```
┌─────────────────┐
│ Mobilna aplikacija │
│ (Android/Kotlin)  │
└────────┬──────────┘
         │
         │ POST /api/events
         │ { topic, message, location, eventType, timestamp }
         ▼
┌─────────────────────────────────────┐
│ Node.js Backend (Express)           │
│ ┌─────────────────────────────┐     │
│ │ eventController.create()     │     │
│ │ - Validacija                │     │
│ │ - Čuvanje u MongoDB         │     │
│ │   status: "PENDING"         │     │
│ └────────┬────────────────────┘     │
│          │                          │
│          │ ASINHRONO (background)  │
│          ▼                          │
│ ┌─────────────────────────────┐     │
│ │ blockchainWorker            │     │
│ │ (periodično procesiranje)  │     │
│ │ - Uzima PENDING evente      │     │
│ │ - Poziva blockchain API     │     │
│ │ - Ažurira event status      │     │
│ └────────┬────────────────────┘     │
└──────────┼──────────────────────────┘
           │
           │ HTTP POST
           │ /api/blockchain/mine
           │ { data: JSON(event), timestamp }
           ▼
┌─────────────────────────────────────┐
│ Blockchain Servis (ASP.NET Core)    │
│ ┌─────────────────────────────┐     │
│ │ POST /api/blockchain/mine   │     │
│ │ - Kreira Block sa eventom   │     │
│ │ - Mining (Proof-of-Work)    │     │
│ │ - Dodaje u blockchain       │     │
│ │ - Vraća mined block         │     │
│ └────────┬────────────────────┘     │
└──────────┼──────────────────────────┘
           │
           │ Response: { hash, timestamp, index, ... }
           ▼
┌─────────────────────────────────────┐
│ Node.js Backend (nastavak)          │
│ ┌─────────────────────────────┐     │
│ │ Ažurira Event u MongoDB     │     │
│ │ - blockchainHash            │     │
│ │ - blockchainTimestamp      │     │
│ │ - status: "BLOCKCHAIN_RECORDED"│
│ └─────────────────────────────┘     │
└─────────────────────────────────────┘
```

## Komponente

### 1. blockchainService.js

Servis za komunikacijo sa blockchain API-jem.

**Funkcije:**
- `recordEventInBlockchain(event)` - Zabeleži Event v blockchain
- `checkBlockchainServiceHealth()` - Proveri dostupnost servisa
- `validateBlockchain()` - Validira blockchain lanac
- `getBlockchainState()` - Dohvata trenutno stanje lanca

**Konfiguracija:**
- `BLOCKCHAIN_SERVICE_URL`: URL blockchain servisa (default: `http://localhost:5024`)
- `HTTP_TIMEOUT`: Timeout za HTTP zahteve (default: 30 sekundi)

**Environment varijable:**
```bash
BLOCKCHAIN_SERVICE_URL=http://localhost:5024
```

### 2. blockchainWorker.js

Background worker koji procesira PENDING evente.

**Funkcije:**
- `startWorker()` - Pokreće background worker
- `stopWorker()` - Zaustavlja background worker
- `processPendingEvents(batchSize)` - Procesira batch PENDING eventov
- `processAllPendingEvents()` - Procesira sve PENDING evente (ručno)

**Konfiguracija:**
- `WORKER_CONFIG.enabled`: Da li je worker omogočen (default: `true`)
- `WORKER_CONFIG.intervalMs`: Interval procesiranja (default: 30 sekundi)
- `WORKER_CONFIG.batchSize`: Broj eventov po batch-u (default: 10)
- `WORKER_CONFIG.maxRetries`: Maksimalni broj pokušaja (default: 3)
- `WORKER_CONFIG.retryDelayMs`: Kašnjenje između pokušaja (default: 60 sekundi)

**Environment varijable:**
```bash
BLOCKCHAIN_WORKER_ENABLED=true
BLOCKCHAIN_WORKER_INTERVAL_MS=30000
BLOCKCHAIN_WORKER_BATCH_SIZE=10
BLOCKCHAIN_WORKER_MAX_RETRIES=3
BLOCKCHAIN_WORKER_RETRY_DELAY_MS=60000
```

### 3. blockchainController.js

Kontroler za blockchain worker operacije.

**Endpoints:**
- `POST /api/blockchain/process` - Ručno pokreće procesiranje PENDING eventov
- `GET /api/blockchain/worker/status` - Dohvata status worker-a
- `GET /api/blockchain/chain` - Dohvata trenutno stanje blockchain lanca
- `GET /api/blockchain/validate` - Validira blockchain lanac

### 4. Integracija v app.js

Worker se automatski pokreće pri pokretanju servera:
- Worker počinje da radi kada se server pokrene
- Worker se automatski zaustavlja pri graceful shutdown (SIGTERM, SIGINT)

## Tok podatkov

### 1. Kreiranje Event-a

```javascript
// Mobilna aplikacija šalje event
POST /api/events
{
  "topic": "Parking Full",
  "message": "Parking lot is full",
  "location": "46.0569,14.5058",
  "eventType": "PARKING_FULL",
  "timestamp": 1737000000000
}

// Backend čuva u MongoDB sa statusom "PENDING"
{
  _id: "...",
  topic: "Parking Full",
  message: "Parking lot is full",
  status: "PENDING",  // ← Čeka na procesiranje
  blockchainHash: null,
  blockchainTimestamp: null
}
```

### 2. Background procesiranje

```javascript
// Worker periodično proverava PENDING evente
setInterval(() => {
  // 1. Pronađi PENDING evente
  const pendingEvents = await Event.find({ status: 'PENDING' })
    .sort({ timestamp: 1 })
    .limit(10);

  // 2. Za svaki event:
  for (const event of pendingEvents) {
    // 2a. Serializuj Event → Block.Data
    const blockData = eventBlockMapper.eventToBlockData(event);

    // 2b. Pozovi blockchain servis
    const result = await blockchainService.recordEventInBlockchain(event);

    // 2c. Ažuriraj event
    if (result.success) {
      event.status = 'BLOCKCHAIN_RECORDED';
      event.blockchainHash = result.blockchainHash;
      event.blockchainTimestamp = result.blockchainTimestamp;
      await event.save();
    }
  }
}, 30000); // Svakih 30 sekundi
```

### 3. Blockchain zapis

```javascript
// blockchainService.recordEventInBlockchain(event)
// 1. Serializuj Event → Block.Data
const blockData = eventBlockMapper.eventToBlockData(event);
// Rezultat: '{"eventId":"...","topic":"...",...}'

// 2. Pozovi blockchain servis
POST http://localhost:5024/api/blockchain/mine
{
  "data": '{"eventId":"...","topic":"...",...}',
  "timestamp": 1737000000  // Unix seconds
}

// 3. Blockchain servis vraća mined block
{
  "index": 1,
  "hash": "000abc...",
  "timestamp": 1737000000,
  ...
}
```

### 4. Ažuriranje Event-a

```javascript
// Worker ažurira event sa blockchain podacima
event.status = 'BLOCKCHAIN_RECORDED';
event.blockchainHash = '000abc...';
event.blockchainTimestamp = new Date(1737000000 * 1000);
await event.save();
```

## API Endpoints

### POST /api/blockchain/process

Ručno pokreće procesiranje svih PENDING eventov.

**Request:**
```http
POST /api/blockchain/process
```

**Response:**
```json
{
  "success": true,
  "message": "Processing complete",
  "data": {
    "processed": 5,
    "successful": 4,
    "failed": 1
  }
}
```

### GET /api/blockchain/worker/status

Dohvata status worker-a i blockchain servisa.

**Request:**
```http
GET /api/blockchain/worker/status
```

**Response:**
```json
{
  "success": true,
  "data": {
    "worker": {
      "enabled": true,
      "intervalMs": 30000,
      "batchSize": 10,
      "blockchainServiceUrl": "http://localhost:5024"
    },
    "blockchainService": {
      "available": true,
      "message": "Blockchain service is available",
      "chainValid": true,
      "validationMessage": "Blockchain is valid"
    },
    "events": {
      "pending": 3,
      "blockchainRecorded": 12
    }
  }
}
```

### GET /api/blockchain/chain

Dohvata trenutno stanje blockchain lanca.

**Request:**
```http
GET /api/blockchain/chain
```

**Response:**
```json
{
  "success": true,
  "data": {
    "length": 13,
    "latestIndex": 12,
    "latestHash": "000abc...",
    "cumulativeWeight": "12345",
    "chain": [ /* array of blocks */ ]
  }
}
```

### GET /api/blockchain/validate

Validira blockchain lanac.

**Request:**
```http
GET /api/blockchain/validate
```

**Response:**
```json
{
  "success": true,
  "data": {
    "valid": true,
    "message": "Blockchain is valid"
  }
}
```

## Testiranje

### 1. Kreiranje Event-a

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

### 3. Ručno pokreni procesiranje

```bash
curl -X POST http://localhost:3002/api/blockchain/process
```

### 4. Proveri blockchain lanac

```bash
curl http://localhost:3002/api/blockchain/chain
```

## Napake in obravnava napak

### 1. Blockchain servis nedostupan

**Simptom:** Worker ne može da se poveže sa blockchain servisom.

**Obravnava:**
- Worker proveri dostupnost servisa pre svakog procesiranja
- Event ostaje sa statusom `PENDING` dok servis ne postane dostupan
- Worker pokušava ponovo u sledećem ciklusu

### 2. Mining već u toku (409 Conflict)

**Simptom:** Blockchain servis vraća 409 Conflict.

**Obravnava:**
- Event ostaje sa statusom `PENDING`
- Worker pokušava ponovo u sledećem ciklusu
- Blockchain servis sprečava paralelno mining (concurrency gate)

### 3. Validaciona greška

**Simptom:** Event nije validan za blockchain (prevelik, nedostaju polja, itd.).

**Obravnava:**
- Validacija se vrši pre slanja ka blockchain servisu
- Event se ne šalje ako nije validan
- Greška se loguje u console

## Monitoring

### Logovi

Worker loguje sve operacije:
- `[BlockchainWorker] Processing event ...` - Početak procesiranja
- `[BlockchainWorker] ✓ Event ... recorded in blockchain` - Uspešno procesiranje
- `[BlockchainWorker] ✗ Failed to record event ...` - Neuspešno procesiranje

### Status endpoint

Koristi `/api/blockchain/worker/status` za proveru:
- Da li je worker omogočen
- Da li je blockchain servis dostupan
- Koliko ima PENDING eventov
- Koliko ima BLOCKCHAIN_RECORDED eventov

## Konfiguracija

### Environment varijable

```bash
# Blockchain servis URL
BLOCKCHAIN_SERVICE_URL=http://localhost:5024

# Worker konfiguracija
BLOCKCHAIN_WORKER_ENABLED=true
BLOCKCHAIN_WORKER_INTERVAL_MS=30000
BLOCKCHAIN_WORKER_BATCH_SIZE=10
BLOCKCHAIN_WORKER_MAX_RETRIES=3
BLOCKCHAIN_WORKER_RETRY_DELAY_MS=60000
```

### .env fajl

Dodaj u `backend/.env`:
```env
BLOCKCHAIN_SERVICE_URL=http://localhost:5024
BLOCKCHAIN_WORKER_ENABLED=true
BLOCKCHAIN_WORKER_INTERVAL_MS=30000
BLOCKCHAIN_WORKER_BATCH_SIZE=10
```

## Napomene

1. **Asinhrono procesiranje**: Eventi se procesiraju u pozadini, ne blokiraju API odgovor korisniku.

2. **FIFO redosled**: Eventi se procesiraju po redosledu stvaranja (najstariji prvi).

3. **Batch processing**: Eventi se procesiraju u batch-ovima za bolju performansu.

4. **Graceful shutdown**: Worker se automatski zaustavlja pri zatvaranju servera.

5. **Error resilience**: Worker nastavlja sa radom čak i ako neki event ne uspe.

---

**Versija**: 1.0  
**Datum**: 2024-01-15  
**EPIK**: 3.2 - Implementacija zapisa dogodkov v blockchain

