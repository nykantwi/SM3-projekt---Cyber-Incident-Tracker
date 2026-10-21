# CIT – API-dokumentation

API’et bruges til at oprette, hente, opdatere og slette hændelser.

Baseadresse: `http://localhost:7070/api/v1`

## Endpoints

| Metode | URL | Request Body (JSON) | Response (JSON) | Fejl |
| --- | --- | --- | --- | --- |
| GET | /incidents | Ingen | Liste af hændelser (1), status 200 | |
| GET | /incidents/{id} | Ingen | Hændelse (1), status 200 | 400, 404 |
| POST | /incidents | Oprettelsesformat (2) | Hændelse (1), status 201 | 400 |
| PUT | /incidents/{id} | Opdateringsformat (3) | Hændelse (1), status 200 | 400, 404 |
| DELETE | /incidents/{id} | Ingen | Ingen body, status 204 | 400, 404 |

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
  "reportedEmail": "bruger@example.com"
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

## Fejl

### 400 Bad Request – ugyldige oplysninger

Eksempel ved oprettelse eller opdatering med en tom titel:

```json
{
  "status": 400,
  "message": "Titel må ikke være tom"
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