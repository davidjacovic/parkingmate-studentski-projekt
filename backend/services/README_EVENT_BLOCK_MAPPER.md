# Event Block Mapper - EPIK 3.1

## Pregled

Modul `eventBlockMapper.js` pruža funkcije za mapiranje Event dokumentov iz MongoDB u Block.Data format za blockchain sistem.

## Instalacija

Modul je već dostupan u `backend/services/eventBlockMapper.js`. Nema dodatnih zavisnosti osim `mongoose` koji je već instaliran.

## Uporaba

### Import modula

```javascript
const eventBlockMapper = require('../services/eventBlockMapper');
```

### Serializacija Event → Block.Data

```javascript
const Event = require('../models/eventModel');

// Uzmi Event iz MongoDB
const event = await Event.findById(eventId);

// Serializuj u Block.Data format
const result = eventBlockMapper.eventToBlockData(event);

if (result.success) {
    console.log('Block.Data:', result.data);
    // Rezultat: '{"eventId":"...","topic":"...","message":"...",...}'
} else {
    console.error('Greške:', result.errors);
}
```

### Deserializacija Block.Data → Event podatci

```javascript
const blockData = '{"eventId":"...","topic":"...",...}';

const result = eventBlockMapper.blockDataToEvent(blockData);

if (result.success) {
    console.log('Event podatci:', result.data);
    // Rezultat: { eventId: "...", topic: "...", ... }
} else {
    console.error('Greške:', result.errors);
}
```

### Validacija Event-a

```javascript
const event = await Event.findById(eventId);

const validation = eventBlockMapper.validateEventForBlockchain(event);

if (validation.valid) {
    console.log('Event je validan za blockchain');
} else {
    console.error('Greške validacije:', validation.errors);
}
```

### Validacija Block.Data

```javascript
const blockData = '{"eventId":"...",...}';

const validation = eventBlockMapper.validateBlockData(blockData);

if (validation.valid) {
    console.log('Block.Data je validan');
} else {
    console.error('Greške validacije:', validation.errors);
}
```

### Timestamp konverzije

```javascript
// Event.timestamp (Date) → Block.timestamp (Unix seconds)
const eventDate = new Date();
const blockTimestamp = eventBlockMapper.eventTimestampToBlockTimestamp(eventDate);

// Block.timestamp (Unix seconds) → Event.timestamp (Date)
const eventTimestamp = eventBlockMapper.blockTimestampToEventTimestamp(blockTimestamp);
```

## API Reference

### `eventToBlockData(event)`

Serializuje Event dokument u Block.Data JSON string.

**Parametri:**
- `event` (Object): Event dokument iz MongoDB

**Povratna vrednost:**
```javascript
{
    success: boolean,
    data: string | null,    // Block.Data JSON string
    errors: string[]
}
```

### `blockDataToEvent(blockData)`

Deserializuje Block.Data JSON string u Event podatke.

**Parametri:**
- `blockData` (string): JSON string koji predstavlja Block.Data

**Povratna vrednost:**
```javascript
{
    success: boolean,
    data: Object | null,    // Event podatci
    errors: string[]
}
```

### `validateEventForBlockchain(event)`

Validira Event dokument za zapisovanje v blockchain.

**Parametri:**
- `event` (Object): Event dokument iz MongoDB

**Povratna vrednost:**
```javascript
{
    valid: boolean,
    errors: string[]
}
```

### `validateBlockData(blockData)`

Validira Block.Data string.

**Parametri:**
- `blockData` (string): JSON string koji predstavlja Block.Data

**Povratna vrednost:**
```javascript
{
    valid: boolean,
    errors: string[]
}
```

### `eventTimestampToBlockTimestamp(eventTimestamp)`

Konvertuje Event.timestamp (Date) u Block.timestamp (Unix seconds).

**Parametri:**
- `eventTimestamp` (Date): JavaScript Date objekat

**Povratna vrednost:**
- `number`: Unix timestamp u sekundama

### `blockTimestampToEventTimestamp(blockTimestamp)`

Konvertuje Block.timestamp (Unix seconds) u Event.timestamp (Date).

**Parametri:**
- `blockTimestamp` (number): Unix timestamp u sekundama

**Povratna vrednost:**
- `Date`: JavaScript Date objekat

## Testiranje

Za pokretanje testova:

```bash
cd backend
node tests/eventBlockMapper.test.js
```

## Omejitve

- **Maksimalna velikost Block.Data**: 1024 karaktera
- **Dozvoljene eventType vrednosti**: `PARKING_FULL`, `PARKING_AVAILABLE`, `LOW_AVAILABILITY`
- **Location format**: `"latitude,longitude"` (npr. `"46.0569,14.5058"`)

## Napake

Sve funkcije vračajo objekte sa `valid` ili `success` i `errors` poljima za jasnu identifikacijo napak.

## Primeri

Detaljni primeri i testovi su dostopni v:
- `backend/tests/eventBlockMapper.test.js`
- `backend/docs/EPIK_3_1_ZASNOVA_BLOCKCHAIN_ZAPISA.md`

---

**Versija**: 1.0  
**EPIK**: 3.1 - Zasnova zapisa dogodkov v blockchain

