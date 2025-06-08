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
