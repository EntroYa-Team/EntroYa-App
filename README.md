# EntroYa 🏢

Sistema de gestió de fitxatge laboral mitjançant tecnologia NFC. Els treballadors fitxen acostant una targeta NFC al dispositiu Android de l'empresa. Els administradors gestionen tot des d'un panell web accessible des de qualsevol dispositiu.

## Components del sistema

- **App Android (Kotlin)** — Terminal de fitxatge NFC instal·lat a l'empresa
- **Backend (Spring Boot / Java 21)** — API REST amb lògica de negoci i persistència
- **Frontend (React + Vite)** — Panell de control web per a administradors i treballadors

## Tecnologies

| Component | Tecnologies |
|-----------|-------------|
| Backend | Java 21, Spring Boot 3, Spring Security, Spring Data JPA, BCrypt |
| Frontend | React 18, Vite, Axios, Bootstrap 5, react-hot-toast |
| Android | Kotlin, MVVM, NFC API, Supabase Postgrest |
| Base de dades | PostgreSQL (Supabase) |
| Desplegament | Railway (monorepository) |

## Requisits previs

- Java 21+
- Node.js 18+
- Android Studio (per a l'app Android)
- Compte a [Supabase](https://supabase.com) amb una base de dades PostgreSQL creada
- (Opcional) Compte a [Railway](https://railway.app) per al desplegament

---

## Posada en marxa en local

### 1. Clonar el repositori

### 2. Configurar la base de dades

Al teu projecte de Supabase, crea les taules necessàries. Spring Boot amb `ddl-auto=update` les generarà automàticament en arrencar si la connexió és correcta.

### 3. Configurar el backend

Edita `application.properties` amb les teves credencials de Supabase:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

Pots definir les variables d'entorn al teu sistema o substituir-les directament per als valors reals (mai pujar credencials reals al repositori).

### 4. Arrencar el backend

El backend estarà disponible a `http://localhost:8080`.

### 5. Configurar i arrencar el frontend

```bash
cd frontend-vite
npm install
npm run dev
```

El frontend estarà disponible a `http://localhost:3000`. El proxy de Vite redirigeix automàticament les crides `/api` al backend al port 8080.

### 6. App Android

Obre la carpeta `android/` amb Android Studio. Configura les credencials de Supabase al fitxer de constants de l'app i executa-la en un dispositiu físic amb NFC habilitat (els emuladors no suporten NFC).


## Desplegament a Railway (monorepository)

### 1. Compilar el frontend

```bash
cd frontend-vite
npm run build
cp -r dist/* ../backend/src/main/resources/static/
```

### 2. Configurar variables d'entorn a Railway

Al tauler de Railway, afegeix:
DB_URL=jdbc:postgresql://...
DB_USER=postgres.xxxxxxxx
DB_PASSWORD=la-teva-contrasenya

### 3. Desplegar

Railway detecta el `pom.xml` i compila i desplega el backend, que serveix també el frontend compilat des de `static/`.

## Estructura del projecte
entroya/
├── backend/                    # Spring Boot
│   └── src/main/java/com/entroya/
│       ├── controller/         # API REST endpoints
│       ├── model/              # Entitats JPA
│       ├── repository/         # Repositoris Spring Data
│       ├── dto/                # Data Transfer Objects
│       └── config/             # SecurityConfig, WebConfig
├── frontend-vite/              # React + Vite
│   └── src/
│       ├── components/
│       │   ├── admin/          # Vistes d'administrador
│       │   ├── worker/         # Vistes de treballador
│       │   └── shared/         # Navbar, Modal, Layout...
│       ├── context/            # AuthContext
│       ├── services/           # api.js (Axios)
│       └── utils/              # toast.jsx
└── android/                    # App Kotlin (Android Studio)

---

## Funcionalitats principals

**Administrador**
- Gestió d'usuaris (crear, editar, eliminar)
- Assignació i desactivació d'horaris laborals
- Pujada de nòmines en PDF per treballador
- Revisió i aprovació/rebuig de justificants d'absència
- Consulta de l'historial de fitxatges de qualsevol treballador
- Gestió de targetes NFC

**Treballador**
- Fitxatge automàtic per NFC (entrada/sortida)
- Resum setmanal d'hores treballades
- Historial de fitxatges agrupat per dies
- Sol·licitud de justificants amb document adjunt
- Descàrrega de nòmines en PDF
- Consulta de l'horari actiu

---

## Autors

- **Manuel Navarro Cortés**
- **Carlos Patricio Paredes**

Projecte intermodular — DAM CFGS — Institut Puig Castellar — Curs 2025-2026

---

## Llicència

CC BY-NC-ND 3.0 ES — Projecte educatiu.
