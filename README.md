# Cyber Incident Tracker (CIT)

Et studieprojekt i Java til at registrere og følge sikkerhedshændelser. Backend bruger Javalin, JPA/Hibernate og PostgreSQL. Login bruger bcrypt og JWT; kun ADMIN må slette hændelser.

## Start lokalt

1. Installer JDK 17 eller nyere, Maven og Docker. Åbn projektet som Maven-projekt i IntelliJ.
2. Start PostgreSQL på port `5432`, og opret databasen `cit`.
3. Opret `src/main/resources/config.properties` med dine lokale databaseoplysninger:

   ```properties
   DB_NAME=cit
   DB_USERNAME=din_databasebruger
   DB_PASSWORD=dit_databasepassword
   ```

   Filen er udeladt fra Git. Hibernate opretter/opdaterer tabellerne ved opstart.
4. Generér en lokal signeringsnøgle med `openssl rand -base64 32`.
5. Tilføj resultatet som miljøvariablen `JWT_SECRET` i kørselskonfigurationen for `app.Main`. Gem ikke nøglen i Git.
6. Kør `app.Main`. API’et starter på `http://localhost:7070/api/v1`.

Registrér en bruger og log ind som beskrevet i [API-dokumentationen](docs/api.md). Send tokenet med ved requests til `/incidents`. Rodadressen `/` har ikke et endpoint.

Den første ADMIN oprettes ved at registrere en bruger og ændre brugerens `role` til `ADMIN` i tabellen `users`, fx gennem pgAdmin. Gem ændringen, og log ind igen for at få et nyt token. Eksisterende brugere uden password-hash kan ikke logge ind.

## Tests

Start Docker. Testcontainers opretter separate PostgreSQL-testdatabaser; testene bruger ikke din lokale `cit`-database.

Tilføj en separat `JWT_SECRET` til JUnit-konfigurationen i IntelliJ. Main-konfigurationens miljøvariabler overføres ikke automatisk til JUnit. Nøglen skal være mindst 32 bytes.

Fra terminalen kan alle tests køres med en midlertidig, tilfældig testnøgle:

```sh
JWT_SECRET="$(openssl rand -base64 32)" mvn test
```

`ApiTest` opretter en REPORTER og en ADMIN med adresser under `@test.com` og testpasswordet `1234`. Testene logger ind gennem API’et og sender tokens med HTTP-requests.

## Projektets opbygning

- Routes forbinder URL’er med controllers.
- Controllers læser requests og returnerer DTO’er som JSON.
- `IncidentService` validerer oplysninger og bruger DAO-laget.
- DAO’er gemmer og henter entities gennem JPA.
- `SecurityController` håndterer register, login og adgangskontrol.

CVE-opslag og concurrency-statistik findes som selvstændige demoer. CVE-opslag er endnu ikke integreret i hændelsesforløbet.

## Afgrænsninger

Roller administreres manuelt. Tokens udløber efter én time og kan ikke tilbagekaldes ved logout eller rolleændring. Alle indloggede roller kan hente og opdatere alle hændelser; kontrol af ejerskab er ikke implementeret. Deployment er ikke verificeret som del af denne lokale opsætning.
