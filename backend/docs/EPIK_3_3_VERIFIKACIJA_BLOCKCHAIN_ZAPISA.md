# EPIK 3.3: Preverjanje nespremenljivosti in zanesljivosti zapisov

## Pregled

Ovaj dokument opisuje implementaciju verifikacije nepromenljivosti i pouzdanosti zapisa u blockchain-u. Verifikacija omogućava proveru da li su eventi zapisani u blockchain-u, da li su blokovi nepromenjeni (hash validacija), i da li je celokupni lanac validan.

## Funkcionalnosti

### 1. Verifikacija bloka po hash-u

**C# Blockchain Service Endpoint:**
```
GET /api/blockchain/verify/{hash}
```

**Node.js Backend Endpoint:**
```
GET /api/blockchain/verify/event/:eventId
```

**Opis:**
- Proverava da li blok sa određenim hash-om postoji u blockchain-u
- Verifikuje integritet bloka (da li hash odgovara izračunatom hash-u)
- Verifikuje da li je blok validan u kontekstu lanca (validacija prethodnog hash-a, timestamp-a, itd.)

**Response:**
```json
{
  "verified": true,
  "found": true,
  "integrityValid": true,
  "chainValid": true,
  "message": "Block with hash '...' is verified and valid in chain",
  "block": {
    "index": 1,
    "data": "...",
    "timestamp": 1234567890,
    "previousHash": "...",
    "difficulty": 3,
    "nonce": 12345,
    "hash": "..."
  }
}
```

### 2. Pretraga blokova po sadržaju

**C# Blockchain Service Endpoint:**
```
GET /api/blockchain/search?data={data}
```

**Node.js Backend Endpoint:**
```
GET /api/blockchain/search?data={data}
```

**Opis:**
- Pronalazi sve blokove koji sadrže određene podatke (npr. event ID)
- Koristi se za pronalaženje eventa u blockchain-u po ID-ju ili drugim podacima

**Response:**
```json
[
  {
    "index": 1,
    "data": "...",
    "timestamp": 1234567890,
    "previousHash": "...",
    "difficulty": 3,
    "nonce": 12345,
    "hash": "..."
  },
  ...
]
```

### 3. Verifikacija eventa po ID-ju

**Node.js Backend Endpoint:**
```
GET /api/blockchain/verify/event/:eventId
```

**Opis:**
- Pronalazi event u MongoDB bazi po ID-ju
- Proverava da li event ima `blockchainHash`
- Verifikuje da li je blok sa tim hash-om validan u blockchain-u

**Response:**
```json
{
  "success": true,
  "data": {
    "eventId": "...",
    "eventStatus": "BLOCKCHAIN_RECORDED",
    "blockchainHash": "...",
    "verified": true,
    "found": true,
    "integrityValid": true,
    "chainValid": true,
    "message": "Block with hash '...' is verified and valid in chain",
    "block": {
      ...
    }
  }
}
```

## Metode za verifikaciju

### C# Blockchain klasa

#### `FindBlockByHash(string hash)`
Pronalazi blok po hash-u u lancu.

#### `FindBlocksByData(string data)`
Pronalazi sve blokove koji sadrže određene podatke.

#### `VerifyBlockIntegrity(Block block)`
Verifikuje integritet bloka:
- Proverava da li hash bloka odgovara izračunatom hash-u
- Proverava da li hash ispunjava difficulty zahtev (broj vodećih nula)

#### `VerifyBlockInChain(string hash)`
Verifikuje da li je blok validan u kontekstu lanca:
- Pronalazi blok po hash-u
- Verifikuje integritet bloka
- Za genesis blok (index 0), samo proverava hash
- Za ostale blokove, proverava validaciju u kontekstu prethodnog bloka (IsValidNewBlock)

### Node.js Backend servisi

#### `verifyBlock(hash)`
Komunicira sa C# blockchain servisom da verifikuje blok po hash-u.

#### `searchBlocks(data)`
Komunicira sa C# blockchain servisom da pretraži blokove po sadržaju.

#### `verifyEvent(eventId)` (u blockchainController.js)
- Pronalazi event u MongoDB
- Proverava da li ima `blockchainHash`
- Verifikuje blok u blockchain-u

## Kako funkcioniše verifikacija nepromenljivosti

### 1. Hash validacija

Svaki blok ima hash koji se računa na osnovu:
- Index
- Data
- Timestamp
- PreviousHash
- Difficulty
- Nonce

Hash se računa kao:
```
hash = SHA256(index + data + timestamp + previousHash + difficulty + nonce)
```

Ako se bilo koji podatak u bloku promeni, hash će biti drugačiji. Ovo garantuje nepromenljivost.

### 2. Chain validacija

Svaki blok (osim genesis bloka) mora:
- Imati `previousHash` koji se poklapa sa hash-om prethodnog bloka
- Imati `index` koji je za 1 veći od prethodnog bloka
- Ispuniti difficulty zahtev (broj vodećih nula u hash-u)
- Ispuniti timestamp validaciju (timestamp mora biti veći od prethodnog bloka)

### 3. Integritet bloka

Blok je validan ako:
- Hash bloka odgovara izračunatom hash-u
- Hash ispunjava difficulty zahtev

Ako se blok promeni, hash se neće poklapati sa izračunatim hash-om, što znači da je blok promenjen.

## Primeri korišćenja

### Verifikacija eventa po ID-ju

```bash
# Backend API
curl http://localhost:3000/api/blockchain/verify/event/69678e787ef71d768563a802
```

### Verifikacija bloka po hash-u

```bash
# C# Blockchain Service
curl http://localhost:5024/api/blockchain/verify/000a374f0d7b12a2...
```

### Pretraga blokova po sadržaju

```bash
# Backend API
curl "http://localhost:3000/api/blockchain/search?data=69678e787ef71d768563a802"

# C# Blockchain Service
curl "http://localhost:5024/api/blockchain/search?data=69678e787ef71d768563a802"
```

## Testiranje

### 1. Testiranje verifikacije eventa

1. Kreiraj event u aplikaciji
2. Sačekaj da se event zapisuje u blockchain (automatski ili ručno)
3. Verifikuj event:
   ```bash
   GET /api/blockchain/verify/event/:eventId
   ```
4. Proveri da je `verified: true`, `integrityValid: true`, `chainValid: true`

### 2. Testiranje verifikacije bloka

1. Zapiši event u blockchain
2. Dobij `blockchainHash` iz event-a
3. Verifikuj blok:
   ```bash
   GET /api/blockchain/verify/:hash
   ```
4. Proveri da je `verified: true`, `integrityValid: true`, `chainValid: true`

### 3. Testiranje nepromenljivosti

1. Verifikuj blok i zabeleži hash
2. Pokušaj da promeniš podatke u blockchain-u (nemoguće bez promene hash-a)
3. Verifikuj ponovo - integritet će biti invalidan ako su podaci promenjeni

## Bezbednost

- **Hash validacija**: Svaki blok ima hash koji garantuje nepromenljivost
- **Chain validacija**: Prethodni hash garantuje povezanost blokova
- **Difficulty**: Proof-of-Work difficulty garantuje da je blok legitiman
- **Timestamp validacija**: Timestamp validacija garantuje redosled blokova

## Zaključak

Implementacija verifikacije nepromenljivosti i pouzdanosti zapisa omogućava:
- Proveru da li su eventi zapisani u blockchain-u
- Proveru da li su blokovi nepromenjeni
- Proveru da li je celokupni lanac validan
- Pretragu blokova po sadržaju

Ovo garantuje integritet i nepromenljivost zapisa u blockchain-u.

