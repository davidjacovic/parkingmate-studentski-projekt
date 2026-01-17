# EPIK 3.1: Zasnova zapisa dogodkov v blockchain

## Pregled

Ovaj dokument opisuje dizajn i zasnovu zapisa dogodkov (eventov) v blockchain sistemu ParkingMate aplikacije.

## 1. Struktura podatkov

### 1.1 Event model (MongoDB)

Event dokument v MongoDB ima sledeču strukturo:

```javascript
{
  _id: ObjectId,
  topic: String,
  message: String,
  timestamp: Date,
  location: String,        // Format: "latitude,longitude"
  eventType: String,       // "PARKING_FULL", "PARKING_AVAILABLE", "LOW_AVAILABILITY"
  status: String,          // "PENDING", "PROCESSED", "BLOCKCHAIN_RECORDED"
  blockchainHash: String,  // null ili hash bloka iz blockchain-a
  blockchainTimestamp: Date, // null ili timestamp bloka iz blockchain-a
  createdAt: Date,
  updatedAt: Date
}
```

### 1.2 Block model (Blockchain)

Block v blockchain sistemu ima sledeču strukturo:

```javascript
{
  index: Number,
  data: String,            // JSON string - serializovani event
  timestamp: Number,       // Unix timestamp (sekunde)
  previousHash: String,
  difficulty: Number,
  nonce: Number,
  hash: String
}
```

### 1.3 Event → Block.Data mapiranje

**Block.Data** je JSON string koji sadrži serializovani event podatke:

```json
{
  "eventId": "64a9b8c2f0a5c1234567890b",
  "topic": "Parking Full",
  "message": "Parking lot at location X is full",
  "timestamp": 1737000000000,
  "location": "46.0569,14.5058",
  "eventType": "PARKING_FULL"
}
```

**Format Block.Data:**
- `eventId`: MongoDB ObjectId kao string (obavezan)
- `topic`: Naslov dogodka (obavezan)
- `message`: Poruka dogodka (obavezan)
- `timestamp`: Unix timestamp u milisekundama (obavezan)
- `location`: Koordinate u formatu "lat,lon" (obavezan)
- `eventType`: Tip dogodka - enum vrednost (obavezan)

## 2. Serializacija in deserializacija

### 2.1 Event → Block.Data (serializacija)

**Proces:**
1. Uzmi Event dokument iz MongoDB
2. Ekstraktuj relevantna polja:
   - `_id` → `eventId` (konvertuj ObjectId u string)
   - `topic`, `message`, `location`, `eventType` (direktno)
   - `timestamp` → Unix timestamp u milisekundama
3. Serializuj u JSON string koristeći `JSON.stringify()`
4. Rezultat je `Block.Data` string

**Primer:**
```javascript
Event → {
  _id: ObjectId("64a9b8c2f0a5c1234567890b"),
  topic: "Parking Full",
  message: "Parking lot full",
  timestamp: Date("2024-01-15T10:30:00Z"),
  location: "46.0569,14.5058",
  eventType: "PARKING_FULL"
}

Block.Data → '{"eventId":"64a9b8c2f0a5c1234567890b","topic":"Parking Full","message":"Parking lot full","timestamp":1705315800000,"location":"46.0569,14.5058","eventType":"PARKING_FULL"}'
```

### 2.2 Block.Data → Event (deserializacija)

**Proces:**
1. Uzmi `Block.Data` string iz blockchain bloka
2. Deserializuj koristeći `JSON.parse()`
3. Validiraj strukturo podatkov
4. Konvertuj nazad u Event format (ako je potrebno)

**Primer:**
```javascript
Block.Data → '{"eventId":"64a9b8c2f0a5c1234567890b","topic":"Parking Full",...}'

Event podatci → {
  eventId: "64a9b8c2f0a5c1234567890b",
  topic: "Parking Full",
  message: "Parking lot full",
  timestamp: 1705315800000,
  location: "46.0569,14.5058",
  eventType: "PARKING_FULL"
}
```

## 3. Timestamp mapiranje

### 3.1 Event.timestamp → Block.timestamp

- **Event.timestamp**: JavaScript `Date` objekat (MongoDB Date)
- **Block.timestamp**: Unix timestamp u sekundama (long)

**Konverzija:**
```javascript
// Event.timestamp (Date) → Block.timestamp (Unix seconds)
const blockTimestamp = Math.floor(event.timestamp.getTime() / 1000);
```

### 3.2 Block.timestamp → Event.timestamp

**Konverzija:**
```javascript
// Block.timestamp (Unix seconds) → Event.timestamp (Date)
const eventTimestamp = new Date(blockTimestamp * 1000);
```

## 4. Validacija

### 4.1 Validacija Event dokumenta

Pre serializacije, Event mora biti validan:
- ✅ `_id` mora postojati (ObjectId)
- ✅ `topic` mora biti non-empty string
- ✅ `message` mora biti non-empty string
- ✅ `timestamp` mora biti validan Date objekat
- ✅ `location` mora biti u formatu "lat,lon"
- ✅ `eventType` mora biti jedna od dozvoljenih vrednosti

### 4.2 Validacija Block.Data stringa

Pre deserializacije, Block.Data mora biti validan:
- ✅ Mora biti validan JSON string
- ✅ Mora sadržati sva obavezna polja (`eventId`, `topic`, `message`, `timestamp`, `location`, `eventType`)
- ✅ `eventId` mora biti validan MongoDB ObjectId format
- ✅ `timestamp` mora biti validan Unix timestamp (number)
- ✅ `location` mora biti u formatu "lat,lon"
- ✅ `eventType` mora biti jedna od dozvoljenih vrednosti

## 5. Omejitve

### 5.1 Velikost Block.Data

- **Maksimalna velikost**: 1024 karaktera (definisano v blockchain servisu)
- **Provera**: Event podatci moraju biti dovoljno mali da se uklapaju u limit

### 5.2 Kompatibilnost

- **Versija formata**: v1.0
- **Backward compatibility**: Format mora biti stabilan za buduće verzije
- **Dodavanje novih polja**: Moguće samo kao opciona polja (backward compatible)

## 6. Primeri uporabe

### Primer 1: Serializacija Event-a

```javascript
const event = {
  _id: ObjectId("64a9b8c2f0a5c1234567890b"),
  topic: "Parking Full",
  message: "Parking lot at location X is full",
  timestamp: new Date("2024-01-15T10:30:00Z"),
  location: "46.0569,14.5058",
  eventType: "PARKING_FULL",
  status: "PENDING"
};

const blockData = eventBlockMapper.eventToBlockData(event);
// Rezultat: '{"eventId":"64a9b8c2f0a5c1234567890b","topic":"Parking Full",...}'
```

### Primer 2: Deserializacija Block.Data

```javascript
const blockData = '{"eventId":"64a9b8c2f0a5c1234567890b","topic":"Parking Full","message":"Parking lot full","timestamp":1705315800000,"location":"46.0569,14.5058","eventType":"PARKING_FULL"}';

const eventData = eventBlockMapper.blockDataToEvent(blockData);
// Rezultat: { eventId: "...", topic: "...", ... }
```

### Primer 3: Validacija

```javascript
const isValid = eventBlockMapper.validateEventForBlockchain(event);
if (!isValid.valid) {
  console.error('Validation errors:', isValid.errors);
}
```

## 7. Implementacija

Helper funkcije su implementirane v:
- **File**: `backend/services/eventBlockMapper.js`
- **Funkcije**:
  - `eventToBlockData(event)` - Serializacija Event → Block.Data
  - `blockDataToEvent(blockData)` - Deserializacija Block.Data → Event
  - `validateEventForBlockchain(event)` - Validacija Event-a
  - `validateBlockData(blockData)` - Validacija Block.Data stringa

## 8. Napake in obravnava napak

### Možne napake:

1. **Missing required fields**: Event nema sva obavezna polja
2. **Invalid timestamp format**: Timestamp nije validan Date ili number
3. **Invalid location format**: Location nije u formatu "lat,lon"
4. **Invalid eventType**: EventType nije jedna od dozvoljenih vrednosti
5. **Block.Data too large**: Serializovani string prelazi 1024 karaktera
6. **Invalid JSON**: Block.Data nije validan JSON string

### Obravnava napak:

Sve funkcije vračajo objekte sa `valid` i `errors` polji za jasnu identifikacijo napak.

## 9. Testiranje

Unit testovi za mapiranje su dostopni v:
- `backend/tests/eventBlockMapper.test.js`

---

**Versija**: 1.0  
**Datum**: 2024-01-15  
**Avtor**: ParkingMate Development Team

