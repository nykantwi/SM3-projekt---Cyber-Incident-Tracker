# CIT – API-dokumentation

API’et bruges til at oprette, hente, opdatere og slette hændelser.

Baseadresse: `http://localhost:7070/api/v1`

## Endpoints

Alle URL’er nedenfor er relative til baseadressen.

| Metode | URL | Request Body (JSON) | Response (JSON) | Fejl |
| --- | --- | --- | --- | --- |
| POST | /auth/register | Bruger (4) | Bekræftelse (4), status 201 | 400, 409 |
| POST | /auth/login | Login (5) | Token (5), status 200 | 401 |
| GET | /incidents | Ingen | Liste af hændelser (1), status 200 | 401 |
| GET | /incidents/{id} | Ingen | Hændelse (1), status 200 | 400, 401, 404 |
| POST | /incidents | Oprettelsesformat (2) | Hændelse (1), status 201 | 400, 401 |
| PUT | /incidents/{id} | Opdateringsformat (3) | Hændelse (1), status 200 | 400, 401, 404 |
| DELETE | /incidents/{id} | Ingen | Ingen body, status 204 | 400, 401, 403, 404 |

## Login og adgang

Register og login er offentlige. Alle incident-endpoints kræver et gyldigt JWT i denne header:

```http
Authorization: Bearer INDSÆT_DIT_TOKEN
```

Ved JSON-requests skal `Content-Type: application/json` også sendes.

- REPORTER, ANALYST og ADMIN kan hente, oprette og opdatere hændelser.
- Kun ADMIN kan slette hændelser.
- Nye brugere får rollen REPORTER. Hver bruger har én rolle.
- Roller ændres foreløbig manuelt i databasen; der er ikke et endpoint til det.

Tokenet gælder i én time. Efter en rolleændring skal brugeren logge ind igen for at få et token med den nye rolle. Tidligere udstedte tokens beholder deres rolle indtil udløb; løsningen har ikke logout med tilbagekaldelse af tokens.

Adgangen er rollebaseret, ikke begrænset til brugerens egne hændelser. `reportedEmail` vælges i requesten og knyttes endnu ikke automatisk til den indloggede bruger.

## Request Body og Response-formater

### (1) Hændelse – response

Eksempel på en hændelse, som API’et returnerer:

```json
{
  "id": 1,
  "title": "Mistænkelig e-mail",
  "description": "En medarbejder har modtaget et muligt phishingforsøg.",
  "status": "OPEN",
  "severity": "HIGH",
  "reportedById": 1
}
```

`id` er hændelsens ID, og `reportedById` er ID’et på den bruger,
der har rapporteret hændelsen.

Ved GET /incidents returneres en liste af sådanne objekter: `[{...}, {...}]`.
Hvis der ingen hændelser er, returneres en tom liste: `[]`.

### (2) Oprettelse – request

Bruges til POST /incidents.

```json
{
  "title": "Mistænkelig e-mail",
  "description": "En medarbejder har modtaget et muligt phishingforsøg.",
  "severity": "HIGH",
  "reportedEmail": "bruger@test.com"
}
```

- `title` skal være udfyldt og må ikke kun indeholde mellemrum.
- `description` er valgfri og må højst være 2.000 tegn.
- `severity` skal være LOW, MEDIUM, HIGH eller CRITICAL.
- `reportedEmail` skal tilhøre en eksisterende bruger.

ID genereres automatisk, og status sættes til OPEN.

Ved succes returneres status 201 Created og den oprettede hændelse
i format (1).

### (3) Opdatering – request

Bruges til PUT /incidents/{id}. Erstat {id} med hændelsens ID.

```json
{
  "title": "Opdateret phishinghændelse",
  "description": "Hændelsen er under undersøgelse.",
  "severity": "HIGH",
  "status": "INVESTIGATING"
}
```

- `title` skal være udfyldt og må ikke kun indeholde mellemrum.
- `description` må højst være 2.000 tegn. Udelades feltet, bliver
  den eksisterende beskrivelse fjernet.
- `severity` skal være LOW, MEDIUM, HIGH eller CRITICAL.
- `status` skal være OPEN, INVESTIGATING, CONTAINED eller RESOLVED.

Titel, severity og status skal sendes med ved hver opdatering.
Hændelsens ID og rapportør ændres ikke.

Ved succes returneres status 200 OK og den opdaterede hændelse
i format (1).

### (4) Registrering – request og response

POST /auth/register:

```json
{
  "name": "Testbruger",
  "email": "bruger@test.com",
  "password": "1234"
}
```

Navn, e-mail og password skal være udfyldt. E-mailen skal være unik.
Passwordet gemmes som en bcrypt-hash og returneres ikke. `1234` er kun et testeksempel.

Response med status 201:

```json
{
  "status": 201,
  "message": "Bruger oprettet"
}
```

Registrering giver ikke automatisk et token. Brug login bagefter.

### (5) Login – request og response

POST /auth/login:

```json
{
  "email": "bruger@test.com",
  "password": "1234"
}
```

Response med status 200 (tokenværdien er en pladsholder):

```json
{
  "token": "INDSÆT_DIT_TOKEN"
}
```

Kopiér tokenet til Authorization-headeren ved efterfølgende requests.

## Fejl

### 400 Bad Request – ugyldige oplysninger

Eksempel ved oprettelse eller opdatering med en tom titel:

```json
{
  "status": 400,
  "message": "Titel må ikke være tom"
}
```

### 401 Unauthorized – manglende eller ugyldigt login

Uden token på et beskyttet endpoint:

```json
{
  "status": 401,
  "message": "Du skal være logget ind"
}
```

Ved forkert e-mail eller password returneres `Forkert e-mail eller password`.
Ved et ugyldigt token returneres `Token er ugyldigt` eller `Token er ugyldigt eller udløbet`, afhængigt af hvilken kontrol der afviser det.

### 403 Forbidden – rollen giver ikke adgang

Hvis en REPORTER eller ANALYST forsøger DELETE:

```json
{
  "status": 403,
  "message": "Kun ADMIN må slette hændelser"
}
```

### 409 Conflict – e-mailen er allerede registreret

```json
{
  "status": 409,
  "message": "E-mail er allerede i brug"
}
```

### 404 Not Found – hændelsen findes ikke

Eksempel ved opslag på et ID, der ikke findes:

```json
{
  "status": 404,
  "message": "Incident med id 99 findes ikke"
}
```

### 500 Internal Server Error – uventet serverfejl

Kan forekomme på alle endpoints. Fejldetaljerne logges på serveren,
mens klienten modtager:

```json
{
  "status": 500,
  "message": "Der opstod en intern serverfejl"
}
```

### Validering af ID

ID’et i URL’en skal være et heltal større end 0.
Javalins valideringsfejl returneres med status 400 og et andet
JSON-format end fejlene ovenfor.

Eksempel med ID -1:

```json
{
  "id": [
    {
      "message": "ID skal være større end 0",
      "args": {},
      "value": -1
    }
  ]
}
```

Hvis ID’et indeholder bogstaver, bruges fejlbeskeden
`TYPE_CONVERSION_FAILED`.
