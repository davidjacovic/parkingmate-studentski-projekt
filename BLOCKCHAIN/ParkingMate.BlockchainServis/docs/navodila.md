# ParkingMate Blockchain Web Service

Ovaj dokument opisuje kako je razvijen i kako se pokreće Blockchain Web Service
za projekat ParkingMate, kao i kako se demonstrira njegov rad kroz REST API.

## 0) Preduslovi i instalacije

### .NET SDK
Potrebno je imati instaliran .NET SDK (verzija 7 ili 8).

Provera:
```bash
dotnet --version
```

### Swagger
Swagger UI se koristi za ručno testiranje REST API-ja.
U projektu je dodat NuGet paket:
**Swashbuckle.AspNetCore**
Nije potrebna dodatna instalacija — Swagger je deo servisa.

### PowerShell
PowerShell se koristi za testiranje endpoint-a pomoću:
**irm (Invoke-RestMethod)**
**iwr (Invoke-WebRequest)**

## 1) Struktura projekta
Projekat se sastoji iz dva glavna dela:
1.1 Blockchain logika: **ParkingMate.Blockchain**

Ovaj projekat sadrži kompletnu blockchain logiku:
- Block i Blockchain modele
- Proof-of-Work mining
- validaciju lanca
- difficulty algoritam
- timestamp validaciju
- multithreading i MPI (test/benchmark režim)

1.2 Blockchain Web Service: **ParkingMate.BlockchainServis**

ASP.NET Core Web API servis koji:
- izlaže REST API
- koristi postojeću blockchain logiku
- čuva blockchain u memoriji
- omogućava integraciju sa Android aplikacijom


## 2) Povezivanje servisa sa blockchain logikom

- Web API projekat ima Project Reference ka ParkingMate.Blockchain
- U Dependency Injection (DI) se registruju:
-- BlockchainState (singleton – čuva chain u memoriji)
-- MiningGate (sprečava paralelni mining)
- Controller koristi BlockchainState i MiningGate
Na ovaj način servis koristi tačno istu blockchain logiku koja je ranije implementirana i testirana.

## 3) Omogućavanje Swagger UI-ja
U fajlu Program.cs nalaze se sledeće linije:
```bash
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

app.UseSwagger();
app.UseSwaggerUI();
```
Swagger UI je dostupan na adresi:
```bash
http://localhost:5024/swagger/index.html
```
Dostupni endpoint-i:
- GET /health
- GET /api/blockchain
- GET /api/blockchain/validate
- POST /api/blockchain/mine

## 4) Pokretanje servisa
Korak 1: Otvaranje foldera servisa 
Korak 2: Build - **dotnet build**
Korak 3: Pokretanje servisa - **dotnet run** (Ovaj terminal mora ostati otvoren dok se servis koristi.)

## 5) Testiranje endpoint-a (drugi terminal)
Otvoriti novi PowerShell prozor.

5.1 Health check
```bash
iwr http://localhost:5024/health -UseBasicParsing
```
Očekivani odgovor:
```bash
{
  "status": "ok"
}
```

5.2 GET stanje blockchaina (samo Genesis blok)
```bash
irm http://localhost:5024/api/blockchain
```
Primer odgovora:
```bash
yaml
Copy code
length           : 1
latestIndex      : 0
latestHash       : <hash>
cumulativeWeight : 2
chain            : {@{index=0; data=Genesis Block; ...}}
```

5.3 Validacija blockchaina
```bash
irm http://localhost:5024/api/blockchain/validate
```
Očekivano:
```bash
valid
-----
True
```

5.4 Dodavanje događaja (mining)
```bash
try {
  irm -Method Post http://localhost:5024/api/blockchain/mine `
    -ContentType "application/json" `
    -Body '{"data":"event: accident at Koroska 11","timestamp":1737000000}'
} catch {
  $resp = $_.Exception.Response
  "HTTP: $($resp.StatusCode.value__) $($resp.StatusDescription)"
  $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
  $reader.ReadToEnd()
}
```

Primer uspešnog odgovora:
```bash
index        : 1
data         : event: accident at Koroska 11
timestamp    : <unix time>
previousHash : <hash genesis>
difficulty   : 1
nonce        : <number>
hash         : <hash>
```

5.5 Testiranje grešaka

5.5.1 Predugačak data string
```bash
try {
  irm -Method Post http://localhost:5024/api/blockchain/mine `
    -ContentType "application/json" `
    -Body (@{ data = "a" * 2000; timestamp = 1737000000 } | ConvertTo-Json)
} catch {
  $resp = $_.Exception.Response
  "HTTP: $($resp.StatusCode.value__) $($resp.StatusDescription)"
  $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
  $reader.ReadToEnd()
}
```
Očekivano:
```bash
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid request body",
  "details": [
    "data must be <= 1024 characters"
  ]
}
```

5.5.2 Prazan data
```bash 
try {
  irm -Method Post http://localhost:5024/api/blockchain/mine `
    -ContentType "application/json" `
    -Body '{"data":"","timestamp":1737000000}'
} catch {
  $resp = $_.Exception.Response
  "HTTP: $($resp.StatusCode.value__) $($resp.StatusDescription)"
  $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
  $reader.ReadToEnd()
}
```

## Summary
- Definisan REST API ugovor
- Implementiran ASP.NET Core Web API servis
- Integrisana postojeća blockchain logika 
- Dodate validacije, error handling i concurrency zaštita 
- Pripremljen demo scenario i komande za reprodukciju

## Povezivanje sa Android aplikacijom
Android aplikacija (OkHttp) šalje događaje na:
```bash
POST /api/blockchain/mine
```
Događaji se trajno zapisuju u blockchain i mogu se kasnije čitati preko:
```bash
GET /api/blockchain
```