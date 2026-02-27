# Real-Time System Resource Monitor

A full-stack, production-grade application that monitors system resources (CPU, RAM, GPU, Disk, Network) in real time and streams metrics to a modern React dashboard via WebSockets.

## Features

- **Real-time metrics**: CPU usage %, RAM usage %, GPU info/usage, disk read/write speed, network upload/download speed
- **Live dashboard**: Five responsive Chart.js graphs that update every second from WebSocket messages
- **Performance alerts**: Red banner when CPU > 90%, RAM > 80%, or GPU > 85%
- **CSV export**: Download the last 60 seconds of recorded data as a CSV file
- **Light / Dark mode**: Toggle with Tailwind's `dark` class; preference persisted in `localStorage`
- **Responsive layout**: Works on desktop, tablet, and mobile
- **Single source of truth**: Global React context for WebSocket stats and rolling 60-second history

## Architecture (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           FRONTEND (React + Vite)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ StatsContext│  │ useWebSocket│  │ ThemeContext│  │ Dashboard       │  │
│  │ (stats +    │  │ SockJS+STOMP│  │ (dark mode) │  │ CpuChart,       │  │
│  │  history)   │  │ /topic/stats│  │             │  │ RamChart, ...   │  │
│  └──────┬──────┘  └──────┬──────┘  └─────────────┘  └────────┬────────┘  │
│         │                │                                    │          │
│         └────────────────┼────────────────────────────────────┘          │
│                          │ WebSocket (SockJS + STOMP)                      │
└──────────────────────────┼────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           BACKEND (Spring Boot 3, Java 17)               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ StatsScheduler (@Scheduled fixedRate = 1000 ms)                   │  │
│  │   → CpuService.getCpuStats()                                      │  │
│  │   → MemoryService.getMemoryStats()                                │  │
│  │   → GpuService.getGpuStats()                                      │  │
│  │   → DiskService.getDiskStats()                                   │  │
│  │   → NetworkService.getNetworkStats()                              │  │
│  │   → SystemStats DTO → SimpMessagingTemplate.convertAndSend         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │ CpuService │  │MemorySvc   │  │ GpuService │  │ DiskService│ ...    │  │
│  │ (OSHI)     │  │ (OSHI)     │  │ (OSHI)     │  │ (OSHI)     │        │  │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘        │  │
│        │               │               │               │                │  │
│        └───────────────┴───────────────┴───────────────┘                │  │
│                          SystemInfo (OSHI / JNA)                         │  │
│  WebSocket: /ws (STOMP over SockJS) → /topic/stats                      │  │
└─────────────────────────────────────────────────────────────────────────┘
```

## Screenshots

<!-- Add screenshots here after running the app -->
- **Dashboard (Light)**: _Placeholder: run `npm run dev` in frontend and open http://localhost:3000_
- **Dashboard (Dark)**: _Placeholder: use the theme toggle in the header_
- **Alert banner**: _Placeholder: when CPU > 90% or RAM > 80% or GPU > 85%_

## Installation

### Prerequisites

- **Java 17** (OpenJDK or Oracle)
- **Node.js 18+** and npm
- **Maven 3.6+** (or use wrapper: `./mvnw` / `mvnw.cmd`)

### Run order

**Start the backend first**, then the frontend. If you open the dashboard before the backend is running, you may see a proxy error (`ECONNREFUSED`); refresh the page after the backend has started.

### Backend

```bash
cd system-monitor/backend
mvn spring-boot:run
```

The API and WebSocket server will be available at `http://localhost:8081`.

### Frontend

```bash
cd system-monitor/frontend
npm install
npm run dev
```

The dashboard will be at `http://localhost:3000`. The Vite dev server proxies `/ws` to `http://localhost:8081`, so the frontend connects to the backend WebSocket automatically.

### CPU and GPU temperature, and GPU usage (optional)

The dashboard shows CPU and GPU temperature and **GPU usage %** when available. On Windows, the backend reads these from **[LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor/releases)** when it is running with its **Remote Web Server** enabled:

1. Install and run [LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor/releases) (e.g. run as Administrator if needed for full sensor access).
2. In LibreHardwareMonitor: **Options → Remote web server → Run** (default port **8085**).
3. Keep LibreHardwareMonitor running; the backend will read `http://localhost:8085/data.json` about once per second and display CPU/GPU temps and **GPU load (usage %)** on the dashboard.

**Why GPU usage was 0% without LHM:** The Java library (OSHI) used for system info does not provide GPU load or used VRAM on Windows—only GPU name and total VRAM. So without LibreHardwareMonitor, the app shows **0%** for GPU usage. With LHM’s remote server running, the backend uses LHM’s “GPU Load” sensor for the usage percentage.

If LibreHardwareMonitor is not running or the remote server is off, temperature may still appear from other sources (e.g. Windows thermal zones) or show as **N/A**, and GPU usage will show **0%**.

**Troubleshooting "not reachable" or "connect timed out":**
- After **Set Port** (Options → Remote web server), you must click **Run** in that menu to start the server. The dashboard only gets temps while the server is running.
- To avoid firewall issues, set the **network interface** in LHM "Set Port" to **127.0.0.1** (localhost). Backend config is in `application.yml` (`librehardwaremonitor.url`) or set env `LHM_URL=http://localhost:8085`.
- If you keep LHM on a LAN IP (e.g. 192.168.31.107), allow **TCP port 8085** in Windows Firewall (inbound) for LibreHardwareMonitor or Java.

### Environment variables

- **Backend**: See `backend/.env.example`. Main options: `SERVER_PORT`, `CORS_ALLOWED_ORIGINS` (comma-separated; use `*` for dev only), `LHM_URL`, `SPRING_PROFILES_ACTIVE=dev|prod`.
- **Frontend**: See `frontend/.env.example`. For production build set `VITE_WS_URL` and `VITE_API_BASE` to your backend base URL if the app is served from a different origin.

### Docker

From the `system-monitor` directory:

```bash
docker compose up --build
```

- Backend: `http://localhost:8081`
- Frontend: `http://localhost:80`
- Set `CORS_ALLOWED_ORIGINS` to include `http://localhost:80` and `http://127.0.0.1:80` (already set in `docker-compose.yml`).

### Running tests

- **Backend**: `cd backend && mvn verify`
- **Frontend**: `cd frontend && npm ci && npm run test`

### API documentation and health

- **OpenAPI (Swagger)**: When the backend is running, open `http://localhost:8081/swagger-ui.html`.
- **Health**: `GET http://localhost:8081/actuator/health` returns `{"status":"UP"}` when the app is healthy.

### Production build (frontend)

```bash
cd system-monitor/frontend
npm run build
```

Serve the `dist` folder with any static host. Set `VITE_WS_URL` to your backend WebSocket URL (e.g. `https://api.example.com/ws`) when building if the backend is on a different origin.

## WebSocket API

- **Endpoint**: `http://localhost:8081/ws` (SockJS)
- **Protocol**: STOMP over SockJS
- **Subscribe**: Client subscribes to destination **`/topic/stats`**
- **Message rate**: Server pushes one message every **1000 ms**
- **Payload**: JSON object with the following shape:

```json
{
  "timestamp": 1708789123456,
  "cpu": {
    "usagePercent": 25.5,
    "logicalProcessorCount": 8
  },
  "memory": {
    "totalBytes": 17179869184,
    "usedBytes": 8589934592,
    "availableBytes": 8589934592,
    "usagePercent": 50.0
  },
  "gpu": {
    "usagePercent": 0.0,
    "name": "NVIDIA GeForce GTX 1080",
    "vramUsedBytes": 0,
    "vramTotalBytes": 8589934592
  },
  "disk": {
    "readBytesPerSecond": 1048576,
    "writeBytesPerSecond": 524288,
    "totalBytes": 500000000000,
    "usedBytes": 250000000000,
    "usagePercent": 50.0
  },
  "network": {
    "downloadBytesPerSecond": 1024,
    "uploadBytesPerSecond": 512,
    "totalBytesReceived": 1000000000,
    "totalBytesSent": 500000000
  }
}
```

All numeric values are as described in the DTOs; speeds are in bytes per second.

## Tech Stack

| Layer    | Technologies |
|----------|--------------|
| Backend  | Java 17, Spring Boot 3.2, Spring WebSocket (STOMP), Actuator, SpringDoc OpenAPI, Lombok, OSHI, Maven |
| Frontend | React 18, Vite 5, TailwindCSS 3, Chart.js 4, TypeScript (incremental), Vitest, ESLint, Prettier |
| DevOps   | Docker, GitHub Actions CI, Maven (backend), npm (frontend) |

## Project structure

```
system-monitor/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/systemmonitor/
│       │   ├── SystemMonitorApplication.java
│       │   ├── config/
│       │   │   ├── OshiConfig.java
│       │   │   ├── WebMvcConfig.java
│       │   │   └── WebSocketConfig.java
│       │   ├── dto/
│       │   │   └── SystemStats.java
│       │   ├── scheduler/
│       │   │   └── StatsScheduler.java
│       │   └── service/
│       │       ├── CpuService.java
│       │       ├── MemoryService.java
│       │       ├── GpuService.java
│       │       ├── DiskService.java
│       │       └── NetworkService.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── application-prod.yml
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── tailwind.config.js
│   ├── index.html
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── index.css
│       ├── context/
│       │   ├── StatsContext.jsx
│       │   └── ThemeContext.jsx
│       ├── hooks/
│       │   └── useWebSocket.js
│       ├── components/
│       │   ├── Dashboard.jsx
│       │   ├── CpuChart.jsx
│       │   ├── RamChart.jsx
│       │   ├── GpuChart.jsx
│       │   ├── DiskChart.jsx
│       │   ├── NetworkChart.jsx
│       │   ├── AlertBanner.jsx
│       │   ├── ExportButton.jsx
│       │   └── ThemeToggle.jsx
│       └── utils/
│           └── exportCSV.js
└── README.md
```

## Contributions

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for how to run the app, tests, and lint. Please open an issue or submit a pull request.

## License

MIT License. See LICENSE file if present.
