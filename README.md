# Cyber Incident Tracker (CIT)

## Vision

CIT er et backend-API til at registrere sikkerhedshændelser og følge deres status og alvorlighedsgrad. Formålet er at samle hændelser ét sted og styre adgangen gennem login og brugerroller.

## Links

- **Deployet API:** [https://api.antwi.dk/api/v1/incidents](https://api.antwi.dk/api/v1/incidents)
- **Kildekode:** [CIT på GitHub](https://github.com/nykantwi/SM3-projekt---Cyber-Incident-Tracker)
- **Portfolio og logbog:** [SM3-CIT](https://nykantwi.github.io/Portfolio/projects/sm3-cit/)
- **API-dokumentation:** [Endpoints, JSON-formater og fejlsvar](docs/api.md)

API'et har ikke en grafisk brugerflade. Linket til hændelser returnerer `401`, hvis requesten ikke indeholder et gyldigt token.

## Arkitektur

### Systemoverblik

Projektet er bygget som en backend med flere lag:

- **Routes:** Forbinder URL'er og HTTP-metoder med controller-metoder.
- **Controllers:** Læser requests og sender DTO'er tilbage som JSON.
- **Service:** `IncidentService` håndterer validering og handlinger på hændelser.
- **DAO:** Henter og gemmer data gennem JPA/Hibernate.
- **Entities:** Repræsenterer data og relationer i PostgreSQL.

Registrering og login går fra `SecurityController` direkte til `UserDAO` gennem `ISecurityDAO`.

Teknologier: Java 17, Javalin, JPA/Hibernate, PostgreSQL, Maven, Jackson, bcrypt og JWT. Testene bruger JUnit, REST Assured, Hamcrest og Testcontainers. Deployment bruger GitHub Actions, Docker Hub, Docker Compose, DigitalOcean og Caddy.



### Vigtige designvalg

**Lagdeling og DTO'er:** Databasearbejde ligger i DAO-laget, mens validering af hændelser ligger i service-laget. DTO'er bestemmer, hvilke felter klienten sender og modtager. Password-hashen sendes ikke tilbage i API-svar.

**Authentication:** Brugeren logger ind med e-mail og password. Passwordet kontrolleres med bcrypt, og serveren returnerer et JWT, der gælder i én time. Beskyttede requests skal sende tokenet i `Authorization`-headeren. Serveren kontrollerer signatur og udløbstid.

**Authorization:** Hver bruger har én rolle. REPORTER, ANALYST og ADMIN må hente, oprette og opdatere hændelser. Kun ADMIN må slette. Nye brugere får automatisk REPORTER.

**Fejlhåndtering:** Valideringsfejl og manglende data giver relevante HTTP-statuskoder. `ApiException` håndteres centralt, mens uventede fejl logges og giver en generel `500`-besked. Javalins ID-validering har sit eget JSON-fejlformat, som er beskrevet i API-dokumentationen.


### Vigtige entities

[Se entity-diagrammet](docs/entity-diagram.md)

**User:** En bruger med ID, navn, unik e-mail, password-hash og én rolle: REPORTER, ANALYST eller ADMIN. En bruger kan rapportere flere hændelser og skrive flere kommentarer.

**Incident:** En sikkerhedshændelse med ID, titel, beskrivelse, status, severity og en rapportør. Status kan være OPEN, INVESTIGATING, CONTAINED eller RESOLVED. Severity kan være LOW, MEDIUM, HIGH. Nye incidents starter med OPEN og MEDIUM; API'et kræver dog, at severity sendes ved oprettelse.

**Comment:** En kommentar med ID, indhold, oprettelsestidspunkt, forfatter og tilknyttet hændelse. Comment findes i entity- og DAO-laget, men har endnu ikke REST-endpoints.

## API-dokumentation

Baseadresse på serveren: `https://api.antwi.dk/api/v1`.

| Metode | Endpoint | Funktion | Adgang | Succes |
| --- | --- | --- | --- | --- |
| POST | /auth/register | Opret bruger | Offentlig | 201 |
| POST | /auth/login | Log ind og få token | Offentlig | 200 |
| GET | /incidents | Hent alle hændelser | Gyldigt token | 200 |
| GET | /incidents/{id} | Hent én hændelse | Gyldigt token | 200 |
| POST | /incidents | Opret hændelse | Gyldigt token | 201 |
| PUT | /incidents/{id} | Opdater hændelse | Gyldigt token | 200 |
| DELETE | /incidents/{id} | Slet hændelse | ADMIN | 204 |

### Eksempel: Opret bruger

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "Testbruger",
  "email": "bruger@test.com",
  "password": "1234"
}
```

`1234` bruges kun som testeksempel. Svaret ved succes er `201 Created`:

```json
{
  "status": 201,
  "message": "Bruger oprettet"
}
```

### Eksempel: Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "bruger@test.com",
  "password": "1234"
}
```

Ved succes returneres `200 OK` og et token:

```json
{
  "token": "DIT_JWT"
}
```

### Eksempel: Hent hændelser

```http
GET /api/v1/incidents
Authorization: Bearer DIT_JWT
```

Ved succes returneres `200 OK` og en liste, eksempelvis:

```json
[
  {
    "id": 1,
    "title": "Mistænkelig e-mail",
    "description": "Muligt phishingforsøg.",
    "status": "OPEN",
    "severity": "HIGH",
    "reportedById": 1
  }
]
```

Listen er tom (`[]`), hvis der ikke er oprettet hændelser. Se [den fulde API-dokumentation](docs/api.md) for request-formater, validering og fejlsvar.

## User stories

- Som bruger vil jeg kunne registrere mig og logge ind, så jeg kan bruge API'et.
- Som bruger vil jeg kunne oprette en sikkerhedshændelse med titel, beskrivelse og severity.
- Som bruger vil jeg kunne hente hændelser og se deres status.
- Som bruger vil jeg kunne opdatere en hændelse, efterhånden som den bliver undersøgt.
- Som administrator vil jeg kunne slette hændelser, mens andre roller bliver afvist.




## Deployment Notes

GitHub Actions bygger projektet med Maven og kører testene. Når workflowet lykkes, bygges der et Docker-image, som bliver 
uploadet til Docker Hub som `nyaw1/cit:latest`.
På DigitalOcean kører API'et og PostgreSQL i Docker Compose. Caddy modtager HTTPS-trafik på `api.antwi.dk` og sender den
videre til API-containeren på port 7070. Database- og certifikatdata bevares i Docker-volumes.



Serverens `.env` indeholder databaseoplysninger og `JWT_SECRET`. Den gemmes ikke i Git. Registrering, 
login og et beskyttet GET er afprøvet på serveren; HTTPS-adgang gennem domænet er også afprøvet.

## Afgrænsninger og begrænsninger
Projektet har ikke en password-reset eller token-refresh. JWT-tokens udløber efter én time og kan ikke tilbagekaldes ved 
logout. CVE-opslag og hændelsesstatistik fungerer som selvstændige demoer og er endnu ikke integreret i REST API’et. 
Kommentarer findes i databasen, men har ikke egne endpoints. Opdatering af API’et på serveren sker manuelt.
