# CI/CD projektno poročilo

Naziv skupine: SVD
Členi skupine: Sara Vesković, Vojin Keser, David Jaćović

## Povzetek naloge

V okviru projektne naloge smo vzpostavili osnovni CI/CD workflow za naš projekt ParkingMate. To vključuje:
- Gradnjo in nalaganje Docker slik na Docker Hub
- Uporabo GitHub Actions za avtomatizacijo procesov
- Sprejemanje webhook sporočil na VM-ju (Azure)
- Samodejni zagon in obnavljanje PM2 procesov za webhook strežnik
- Priprava skripte za nadgradnjo Docker containerja na strežniku 

## Docker Hub

Najprej je vodja skupine ustvaril račun na Docker Hub.

![ustvarjanje računa](slike/Screenshot%202025-06-07%20200222.png)

![ustvarjen račun](slike/Screenshot%202025-06-07%20200356.png)

Tukaj je pritisnil gumb `Go to Hub` in ga je to preusmerilo sem:

![Go to Hub](slike/Screenshot%202025-06-07%20213632.png)

Nato je kliknil na `Create repository` in ga je to preusmerilo sem.

![Create repository](slike\Screenshot%202025-06-07%20214231.png)

Tu je vnesel ime repozitorija `"backend"` in ga označil kot `public`. Nato je kliknil `create`, po tem pa je isto naredil tudi za repozitorij `"frontend"`.

![Repozitoriji](slike\Screenshot%202025-06-08%20035232.png)

Repozitoriji uspešno ustvarjeni!

## CLI ukazi

Nato smo preučili in potem dokumentirali CLI ukaze potrebne za uporabo container registry.

- docker login -u <*uporabnik*> → Prijava v Docker Hub
- docker build -t <*repo:tag*> . → Izgradnja slike (npr. docker build -t myusername/myapp:latest .)
- docker push <*repo:tag*> → Pošiljanje slike v Docker Hub (docker push myusername/myapp:latest)
- docker pull <*repo:tag*> → Prenos slike iz Docker Hub na drug strežnik
- docker stop <*container*> → Brisanje containerja
- docker run -d --name <*ime*> -p <*zunanjip*>:<*notranji*> <*repo:tag*> → Zagon novega containerja iz slike

Potem smo z uporabo teh ukazov naredili in objavili slike.

```
docker login -u davidjacovic -p davidjacovic (-u je username, -p je password)
docker build -t davidjacovic/myapp-backend:latest ./backend 
docker push davidjacovic/myapp-backend:latest 
```

Ker imamo Dockerfile tako v backendu kot v frontendu, se isti proces ponavlja tudi za frontend in rezultat je naslednji:

![Slike na repozitoriju](slike\Screenshot%202025-06-08%20152940.png)



## GitHub Actions workflows

Najprej smo GitHub secrets nastavili na naslednji način na GitHub: Settings → Secrets and variables → Actions → New repository secret 

Tukaj smo ustvarili secret:

![secret](slike\Screenshot%202025-06-08%20160616.png)

In nato na enak način dodali še enega:

![secrets](slike\Screenshot%202025-06-08%20160633.png)

## Opis CI/CD workflowa

V našem GitHub Actions workflowu smo nastavili avtomatsko gradnjo Docker slik ob vsakem pushu na repozitorij. Workflow poteka v naslednjih korakih:

- Checkout repozitorija — pridobitev najnovejše verzije kode

- Build Docker slike za backend in frontend

- Prijava na Docker Hub z uporabo GitHub Secrets

- Push Docker slike na Docker Hub

- Pošiljanje webhook sporočila na naš VM na Azure, da je nova verzija pripravljena za deploy

- Webhook sproži posodobitveno skripto na strežniku

S tem zagotavljamo, da je vsak push avtomatsko zgrajen in dostavljen na produkcijsko okolje brez ročnega posega.

## Predlog dodatnih workflowov

Za izboljšanje procesa bi lahko dodali naslednje GitHub Actions workflowe:

- Testiranje kode: Zaženemo unit in integracijske teste, preden izgradimo sliko, da preprečimo nepopolno kodo v produkciji.

- Linting in statična analiza: Samodejna kontrola kakovosti kode (npr. ESLint, SonarQube), ki opozori na napake in varnostne ranljivosti.

- Release workflow: Samodejno objavljanje verzij in generiranje dokumentacije ali changelogov.

- Rollback workflow: V primeru neuspešnega deploya samodejno povrnitev na prejšnjo stabilno verzijo.

## Webhook in avtomatska posodobitev
Na našem strežniku (VM na Azure) smo nastavili preprost webhook strežnik v Node.js (Express), ki posluša na portu 3000.

Webhook prejme sporočilo iz GitHub Actions in zažene skripto, ki:

- ustavi in odstrani obstoječe Docker containere

- prenese najnovejše slike z Docker Huba

- zažene nove containere z najnovejšo verzijo aplikacije

## Skripta update-backend.sh
```

#!/bin/bash

echo "Stopping old backend container..."
docker stop myapp-backend || true
docker rm myapp-backend || true

echo "Stopping old frontend container..."
docker stop myapp-frontend || true
docker rm myapp-frontend || true

echo "Pulling latest backend image..."
docker pull davidjacovic/myapp-backend:latest

echo "Pulling latest frontend image..."
docker pull davidjacovic/myapp-frontend:latest

echo "Starting new backend container..."
docker run -d --name myapp-backend -p 3002:3002 davidjacovic/myapp-backend:latest

echo "Starting new frontend container..."
docker run -d --name myapp-frontend -p 3000:3000 davidjacovic/myapp-frontend:latest

echo "Update complete!"
```