<h1>3D-MMORPGr</h1>

**early development**
**HEAVILY INSPIRED BY RUNESCAPE**

<img width="1001" height="696" alt="image" src="https://github.com/user-attachments/assets/4d3679ee-c6fc-42bd-94d6-d1e39996c790" />
<img width="1001" height="697" alt="image" src="https://github.com/user-attachments/assets/220833db-ab4d-4f79-9ae8-7f5af3d7afb0" />



## About the project

**Available to play on henriwillman.fi**

This an attempt to create a 3D-MMORPG. **The project exists to explore MMO-scale simulation, authoritative networking, and engine-level system design rather than content production.**
Repository includes game-engine/server written with Java, and client+website written with typescript.
No external game-engines are used in this project. Only big dependency is Three.js on client side to handle 3D-rendering. Everything else is hand-written from scratch.
The MMO is server authorative, and has 600ms tick-based game loop. It manages all states, and send deltas to clients. This allows about 1000 players to be connected in at once, without major latency.

Backend is actually split into 4 different servers.
  - Register server. Constains one simple /create-account endpoint.
  - Update server. Players first connect to update server, which checks if current cache is stil valid,
    or if there is an update available. If update is available, the update server packs and sends new assets to the client.
  - Login server. Handles login requests.
  - Game server.

Server runs on about 150mb of memory when 100 players are connected. 

## Game features

  Moving, combat system and inventory management curently. I occasionally ad more features when I feel like it.
  
## Register server

Constains one simple /create-account endpoint.

## Getting Started

You need to have Docker, Java 23 and Gradle installed. 

1. Run docker-compose up -d on root to initialize database container
2. Run gradle run --args="migrate" to run migrations
3. Run gradle run to spin up the server

## Technologies used

1. Java 23
2. Gradle
3. Docker
4. MariaDB
