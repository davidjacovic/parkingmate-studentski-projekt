# Dizajn paralelizacije rudarjenja blokova

## Subtask 4.1.1: Dizajn podele nonce prostora po nitima

### Pregled
Nonce je 64-bitni unsigned integer (ulong u C#), što znači da imamo prostor od 0 do 18,446,744,073,709,551,615 (2^64 - 1).

### Strategija podele

#### 1. Podela nonce prostora
Nonce prostor se deli na N jednakih opsega, gde je N broj niti:

- **Thread 0**: [0, MAX_NONCE/N)
- **Thread 1**: [MAX_NONCE/N, 2*MAX_NONCE/N)
- **Thread 2**: [2*MAX_NONCE/N, 3*MAX_NONCE/N)
- ...
- **Thread i**: [i*MAX_NONCE/N, (i+1)*MAX_NONCE/N)
- **Thread N-1**: [(N-1)*MAX_NONCE/N, MAX_NONCE]

Gde je MAX_NONCE = ulong.MaxValue (18,446,744,073,709,551,615)

#### 2. Računanje opsega za svaku nit
```
ulong rangeSize = ulong.MaxValue / (ulong)numThreads;
ulong threadStartNonce = threadId * rangeSize;
ulong threadEndNonce = (threadId == numThreads - 1) 
    ? ulong.MaxValue 
    : (threadId + 1) * rangeSize - 1;
```

#### 3. Sinhronizacija i zaustavljanje
- Svaka nit proverava da li je rešenje već pronađeno pre svake iteracije
- Kada jedna nit pronađe validan nonce, postavlja se shared flag
- Sve ostale niti prekidaju rad čim primete da je rešenje pronađeno
- Korišćenje `Interlocked` operacija ili `CancellationToken` za thread-safe komunikaciju

#### 4. Povrat pronađenog rešenja
- Kada se pronađe validan nonce, rezultujući blok se čuva u thread-safe strukturi
- Master nit (ili pozivajuća nit) čeka da se sve niti završe ili da se pronađe rešenje
- Prvi blok sa validnim hash-om se vraća

### Primer implementacije

```
Thread 0: nonce = 0, 1, 2, ..., rangeSize-1
Thread 1: nonce = rangeSize, rangeSize+1, ..., 2*rangeSize-1
Thread 2: nonce = 2*rangeSize, 2*rangeSize+1, ..., 3*rangeSize-1
...
```

### Prednosti ovog pristupa
1. **Ravnomerna distribucija**: Svaka nit dobija približno jednak opseg
2. **Bez preklapanja**: Nitovi ne rade na istim nonce vrednostima
3. **Efikasnost**: Minimalna sinhronizacija - samo provera shared flag-a
4. **Skalabilnost**: Lako se dodaju nove niti

### Napomene
- U praksi, verovatnoća da će biti potrebno pretražiti ceo ulong prostor je zanemarljiva
- Većina blokova će biti pronađena u ranijim opsezima
- Ako sve niti završe svoje opsege bez pronalaska, možemo pokrenuti novi ciklus ili povećati difficulty

---

## Subtask 4.1.3: Deljeni flag za prekid rada kada se rešenje nađe

### Pregled
Deljeni flag omogućava koordinaciju između niti - kada jedna nit pronađe validan blok, sve ostale niti moraju biti obaveštene da prekinu rad.

### Implementacija
- **Atomic flag**: Koristi `Interlocked.CompareExchange` za thread-safe postavljanje flag-a
- **Volatile čitanje**: `Thread.VolatileRead` za thread-safe proveru bez lock-a
- **Prvi pronalazač**: Samo jedna nit može postaviti flag (atomically)

### Prednosti
- Brza provera bez lock-a
- Garantovano thread-safe postavljanje
- Minimalna overhead

---

## Subtask 4.1.4: Sinhronizacija (mutex/atomic)

### Pregled
Potrebno je osigurati thread-safe pristup deljenim resursima: pronađenom bloku, statistici, i drugim podacima.

### Korišćeni mehanizmi sinhronizacije

#### 1. Atomic operacije (Interlocked)
Koristi se za jednostavne operacije na primitivnim tipovima:

**Primeri:**
- `Interlocked.CompareExchange` - atomic postavljanje flag-a
- `Interlocked.Exchange` - atomic zamena vrednosti
- `Interlocked.Increment` - atomic inkrementiranje brojača
- `Interlocked.Read` - atomic čitanje long vrednosti

**Prednosti:**
- Veoma brze (hardware podrška)
- Nema deadlock rizika
- Najmanji overhead

**Upotreba:**
```csharp
// Atomic postavljanje flag-a
int original = Interlocked.CompareExchange(ref _flag, 1, 0);

// Atomic inkrementiranje
Interlocked.Increment(ref _counter);
```

#### 2. Lock (mutex pattern)
Koristi se za zaštitu kompleksnih operacija i objekata:

**Primer:**
```csharp
private readonly object _resultLock = new object();
private Block? _foundBlock = null;

// Thread-safe postavljanje
lock (_resultLock)
{
    _foundBlock = block;
}
```

**Double-check locking pattern:**
```csharp
if (!IsSolutionFound())  // Brza provera bez lock-a
{
    if (TrySetSolutionFound())  // Atomic operacija
    {
        lock (_resultLock)  // Lock samo kada je potreban
        {
            if (_foundBlock == null)  // Ponovna provera
            {
                _foundBlock = block;
            }
        }
    }
}
```

**Prednosti:**
- Zaštita kompleksnih operacija
- Ekskluzivan pristup deljenim resursima
- Lako razumevanje

**Upotreba:**
- Čuvanje pronađenog bloka
- Pristup kompleksnim objektima
- Koordinacija više operacija

#### 3. Volatile čitanje
Osigurava da se uvek čita najnovija vrednost iz memorije:

**Primer:**
```csharp
return Thread.VolatileRead(ref _flag) == 1;
```

**Prednosti:**
- Brzo čitanje bez lock-a
- Garantovana konzistentnost
- Ne blokira druge niti

**Upotreba:**
- Česta provera flag-a (hot path)
- Provera statusa bez lock-a

### Thread-safe storage rezultata

#### TrySetFoundBlock metoda
Kombinuje atomic operacije i lock za optimalnu performansu:

1. **Fast path**: Brza provera flag-a bez lock-a
2. **Atomic postavljanje**: Interlocked.CompareExchange za flag
3. **Lock za storage**: Zaštita kompleksnog objekta (bloka)

#### GetFoundBlock metoda
Thread-safe čitanje pronađenog bloka koristeći lock.

### Statistika i brojači

#### Atomic brojači
Koriste se `Interlocked.Increment` i `Interlocked.Read` za:
- Broj pokušaja (total attempts)
- Druge statistike koje se često ažuriraju

### Sinhronizacione strategije po scenariju

| Scenario | Mehanizam | Razlog |
|----------|-----------|--------|
| Provera flag-a (česta) | VolatileRead | Brz, ne blokira |
| Postavljanje flag-a | Interlocked.CompareExchange | Atomic, bez lock-a |
| Čuvanje bloka | Lock | Kompleksan objekat |
| Brojanje pokušaja | Interlocked.Increment | Brz, atomic |
| Resetovanje state-a | Kombinacija | Atomic za flag, lock za objekte |

### Prednosti ovog pristupa
1. **Optimalna performansa**: Kombinacija brzih atomic operacija i lock-a samo gde je potrebno
2. **Thread-safety**: Garantovana sigurnost pri konkurentnom pristupu
3. **Skalabilnost**: Minimalna blokiranja između niti
4. **Čitljivost**: Jasna upotreba svakog mehanizma

### Napomene
- Atomic operacije su najbrže za jednostavne tipove
- Lock je potreban za kompleksne objekte
- Double-check locking pattern optimizuje česte provere
- Volatile čitanje osigurava konzistentnost bez overhead-a lock-a


