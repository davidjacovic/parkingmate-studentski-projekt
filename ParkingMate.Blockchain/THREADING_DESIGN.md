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


