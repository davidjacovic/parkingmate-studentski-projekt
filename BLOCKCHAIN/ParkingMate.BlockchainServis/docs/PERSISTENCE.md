# Blockchain Perzistencija

## Pregled

Blockchain servis sada podržava perzistenciju blockchain lanca na disk. Lanac se čuva u JSON fajlu i automatski se učitava pri pokretanju servisa.

## Lokacija fajla

**Default putanja**: `blockchain_data.json` u root direktorijumu blockchain servisa (gde se nalazi `.exe` fajl).

**Primer putanje**:
```
BLOCKCHAIN/ParkingMate.BlockchainServis/bin/Debug/net8.0/blockchain_data.json
```

## Format fajla

JSON format sa strukturom:
```json
{
  "blocks": [
    {
      "index": 0,
      "data": "Genesis Block",
      "timestamp": 1737000000,
      "previousHash": "0",
      "difficulty": 1,
      "nonce": 0,
      "hash": "00abc..."
    },
    {
      "index": 1,
      "data": "{\"eventId\":\"...\",\"topic\":\"...\",...}",
      "timestamp": 1737000001,
      "previousHash": "00abc...",
      "difficulty": 1,
      "nonce": 12345,
      "hash": "000def..."
    }
  ],
  "savedAt": 1737000002
}
```

## Automatsko čuvanje

Blockchain lanac se automatski čuva:
- **Nakon svakog dodavanja bloka** (POST /api/blockchain/mine)
- **Pri pokretanju servisa** (sačuva genesis blok ako ne postoji fajl)

## Automatsko učitavanje

Blockchain lanac se automatski učitava:
- **Pri pokretanju servisa** - ako postoji `blockchain_data.json`, učitava se
- **Validacija** - učitani lanac se validira pre korišćenja
- **Fallback** - ako učitavanje ne uspe ili je fajl nevalidan, kreira se novi genesis blok

## Šta se dešava pri restartu

### Scenario 1: Fajl postoji i validan je

```
1. Servis se pokreće
2. BlockchainStorage.LoadChain() učitava blokove iz blockchain_data.json
3. Validira se lanac
4. BlockchainState kreira Blockchain instancu sa učitanim blokovima
5. ✓ Blockchain nastavlja od prethodnog stanja
```

**Output**:
```
[BlockchainStorage] ✓ Chain loaded from ... (N blocks)
[BlockchainState] Loaded blockchain with N blocks from storage
```

### Scenario 2: Fajl ne postoji

```
1. Servis se pokreće
2. BlockchainStorage.LoadChain() vraća null (fajl ne postoji)
3. BlockchainState kreira novi Blockchain sa genesis blokom
4. Sačuva genesis blok u blockchain_data.json
5. ✓ Kreira se novi blockchain
```

**Output**:
```
[BlockchainStorage] No storage file found at ..., starting with genesis block
[BlockchainState] Created new blockchain with genesis block
[BlockchainStorage] ✓ Chain saved to ... (1 blocks)
```

### Scenario 3: Fajl je nevalidan

```
1. Servis se pokreće
2. BlockchainStorage.LoadChain() učitava blokove
3. Validacija neuspešna
4. BlockchainState kreira novi Blockchain sa genesis blokom
5. ⚠ Stari fajl se prepisuje novim genesis blokom
```

**Output**:
```
[BlockchainStorage] ✓ Chain loaded from ... (N blocks)
[BlockchainState] ✗ Error: Invalid blockchain chain provided - chain validation failed
[BlockchainState] Created new blockchain with genesis block
```

## Rucno upravljanje

### Brisanje blockchain-a

Ako želiš da resetuješ blockchain:

```bash
# Windows
del blockchain_data.json

# Linux/Mac
rm blockchain_data.json
```

Pri sledećem pokretanju servisa, kreiraće se novi genesis blok.

### Backup blockchain-a

```bash
# Kopiraj fajl
copy blockchain_data.json blockchain_data_backup.json
```

### Provera integriteta

```bash
# Proveri da li je lanac validan
curl http://localhost:5024/api/blockchain/validate
```

## Napomene

1. **Thread-safety**: Čuvanje se dešava nakon mining-a (single-threaded mining), tako da nema race condition-a.

2. **Performanse**: Čuvanje se dešava synchronously nakon svakog bloka. Za velike blockchain-ove, može biti sporije.

3. **Backup**: Preporučuje se redovan backup `blockchain_data.json` fajla.

4. **Velikost fajla**: Fajl raste sa brojem blokova. Svaki blok zauzima ~200-500 bytes u JSON formatu (zavisi od velikosti `data`).

## Troubleshooting

### Problem: Blockchain se ne čuva

**Provera**:
- Proveri da li postoji `blockchain_data.json` u root direktorijumu servisa
- Proveri da li imaš write permisije za taj direktorijum

### Problem: Blockchain se ne učitava

**Provera**:
- Proveri da li je `blockchain_data.json` validan JSON
- Proveri logove pri pokretanju servisa
- Proveri validaciju lanca: `GET /api/blockchain/validate`

### Problem: Blockchain je nevalidan pri učitavanju

**Razlog**:
- Fajl je oštećen
- Fajl je ručno menjan
- Format fajla je promenjen

**Rešenje**:
- Backup postojećeg fajla
- Obriši `blockchain_data.json` i restartuj servis (kreiraće se novi genesis blok)
- Ili popravi fajl ručno (pažljivo!)

---

**Versija**: 1.0  
**Datum**: 2024-01-15

