# GRIDMIND — Power, With a Brain

GRIDMIND is a real-time per-appliance electrical hazard guard and energy intelligence platform. It monitors household appliances at 1 Hz, detects loose-socket micro-arcing and plug-pin thermal drift (`dT/dt`) early, distinguishes upstream grid voltage surges from appliance faults, and compares live consumption against trained hourly `p50`/`p90` baseline profiles.

## Architecture

- **`data/gridmind_baseline_profiles.json`** — Trained hourly baseline model (`2,592` rows across `54` series with `median_kwh`, `p90_kwh`, and `avg_power_w`).
- **`backend/`** — Java 21 / Spring Boot 3.3.5 backend with MongoDB Atlas persistence, 1000 ms virtual appliance telemetry simulator, 6-state FSM (`LEARNING`, `ONLINE`, `WARNING_DRIFT`, `WARNING_ARCING`, `TRIPPED_LATCHED`, `FAULT_HARDWARE`), REST APIs, and WebSocket streaming (`/ws/telemetry`).
- **`frontend/`** — React 18 + TypeScript + Vite + Tailwind CSS live cards dashboard displaying per-appliance readings (`W`, `V`, `A`, `PF`, pin temperature & `dT/dt`, arc jitter, daily `kWh` & `₹` cost), fault simulation controls, 24-hour baseline curve inspection, and latched safety reset workflow.

---

## Running the Project

### 1. Backend (Spring Boot)
Use the short `mvn` wrapper script inside `backend/` (automatically configures `JAVA_HOME` and Maven and defaults to `spring-boot:run`):

```powershell
cd backend
.\mvn
```

*(You can also pass explicit Maven goals, e.g. `.\mvn spring-boot:run` or `.\mvn clean compile`.)*

- **REST API:** `http://localhost:8080/api/appliances`
- **Simulator Status:** `http://localhost:8080/api/simulator/status`
- **WebSocket Stream:** `ws://localhost:8080/ws/telemetry`

### 2. Frontend (React + Vite)
In a separate terminal:

```powershell
cd frontend
npm run dev
```

*(On a fresh clone without `node_modules`, run `npm install` once before `npm run dev`.)*

- **Dashboard URL:** `http://localhost:5173`
