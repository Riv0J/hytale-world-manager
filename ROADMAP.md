# TBD - Roadmap

## Grand Design

Minijuego de **acción y supervivencia** para Hytale con:
- Hordas de enemigos + ciclo día/noche
- Combate estilo souls-like accesible (parry, esquiva, stagger)
- Progresión estilo Warcraft 3 (héroe + niveles)
- 4 spawners → jefe final → victoria
- Partidas caóticas y memorables

## Core Loop

```
Cola → Lobby → Partida generada → Día (prep) → Noche (hordas) → Loop hasta destruir spawners → Jefe Final → Victoria/Derrota → Hub
```

## Arquitectura Modular

```
[QueueManager] → [LobbyManager] → [ArenaManager]
                                        ↓
                                 [MapGenerator]
                                        ↓
                                  [GameCore]
                                      ↓
            ┌───────────┬─────────────┼─────────────┬───────────┐
      [CombatSystem] [SpawnerSystem] [DayNightCycle] [ClassSystem] [ProgressionSystem]
```

## Módulos

### Fase 1: Infraestructura

#### 1.1 WorldManager ✅ DONE
- Crear/eliminar mundos
- Teleportar jugadores

#### 1.2 QueueManager 🎯 NEXT
- `/queue join|leave|status`
- Matchmaking por cantidad
- `MatchFoundEvent`

#### 1.3 LobbyManager
- Lobby temporal pre-partida
- Countdown + ready check
- Selección de clase

#### 1.4 ArenaManager
- Ciclo de vida de partidas
- Spawn points
- Cleanup post-partida

### Fase 2: Generación

#### 2.1 MapGenerator
- Mapa procedural reducido
- Placement de 4 spawners
- Altares de respawn
- Zonas de interés

### Fase 3: Gameplay Core

#### 3.1 DayNightCycle
- Ciclo día/noche marcado
- Día: preparación, visibilidad alta
- Noche: hordas, visibilidad reducida

#### 3.2 SpawnerSystem
- 4 spawners destruibles
- Escala de dificultad mientras vivos
- Al destruir todos → trigger jefe final

#### 3.3 CombatSystem
- Ataque, bloqueo, parry, esquiva
- Barra de stagger (balance)
- Sistema de amenaza (aggro)
- Tank busters (conos frontales)

#### 3.4 ClassSystem
- **Tank**: resistencia + amenaza + daño
- **Healer**: cura + daño
- **DPS**: daño alto
- Nadie pasivo

#### 3.5 ProgressionSystem
- XP por matar enemigos
- Level up durante partida
- Stats persistentes entre partidas

### Fase 4: Enemigos

#### 4.1 Zombies
- Básico, Armadura, Arma, Rápido
- Jefe: Zombi Gigante (AoE)
- Mini-jefe: Abomination (nube tóxica)

#### 4.2 Skeletons
- Base, Armadura, Commander, Mage
- Jefe: Lich (invoca esqueletos)

#### 4.3 Warp (Corrupción)
- Warplings (masa), Commander, Fast Zombie
- Stalker, Reaver
- Jefe: TBD

### Fase 5: Polish

#### 5.1 DeathSystem
- Revivir golpeando cuerpo aliado
- Respawn en altar lejano si no reviven
- 100% HP al revivir

#### 5.2 VisibilitySystem
- Nametags solo en proximidad
- Dirección general después
- Más difícil de noche

#### 5.3 BossSystem
- Jefe final post-spawners
- Se fortalece con el tiempo
- Estado de caos total

---

## Sistema de Datos

### Estrategia: JSON primero, SQLite después

**MVP (v0.1 - v1.0): JSON/BsonUtil**
- Usa el sistema nativo de Hytale (`BsonUtil`)
- Un archivo por jugador: `universe/playerdata/{uuid}.json`
- Suficiente para: stats, wins, kills, progresión básica
- Zero dependencias extra

```java
Path file = Universe.get().getPath().resolve("gamedata/" + uuid + ".json");
BsonUtil.writeDocument(file, doc);  // Async
BsonUtil.readDocument(file);        // Async
```

**Post-MVP (v1.5+): SQLite (si necesario)**
- Migrar cuando necesites: leaderboards, queries complejas, historial masivo
- Requiere Shadow Plugin para empaquetar en el .jar (~10MB extra)
- Alternativa: mod separado "SQLite-lib" como dependencia

### Qué guardar por jugador

| Dato | Fase | Storage |
|------|------|---------|
| UUID, nombre | v0.2 | JSON |
| Wins/losses/kills | v0.3 | JSON |
| Clase preferida | v0.3 | JSON |
| XP total, nivel | v0.8 | JSON |
| Stats persistentes | v0.8 | JSON |
| Historial partidas | v1.5+ | SQLite |
| Leaderboards | v1.5+ | SQLite |

---

## Milestones

| Version | Entregable |
|---------|------------|
| v0.1 | WorldManager ✅ |
| v0.2 | QueueManager |
| v0.3 | LobbyManager + ClassSystem básico |
| v0.4 | ArenaManager + MapGenerator básico |
| v0.5 | DayNightCycle + SpawnerSystem |
| v0.6 | CombatSystem (ataque/bloqueo/stagger) |
| v0.7 | Enemigos Fase 1 (Zombies) |
| v0.8 | DeathSystem + ProgressionSystem |
| v1.0 | MVP Jugable (1 facción completa) |
| v1.5 | Skeletons + Lich |
| v2.0 | Warp faction + Jefe final |

---

## Próximo Paso: QueueManager

**Scope v0.2:**
```
/queue join    → unirse a cola
/queue leave   → salir
/queue status  → ver estado

MatchFoundEvent → dispara cuando hay N jugadores
```

**Por qué primero:**
- Independiente (no necesita otros módulos)
- Define el flujo desde el inicio
- Reutilizable para otros minijuegos
