package com.systemmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads CPU and GPU temperature from LibreHardwareMonitor's Remote Web Server
 * when it is running (Options → Remote web server → Run, default port 8085).
 * See: https://github.com/LibreHardwareMonitor/LibreHardwareMonitor/releases
 */
@Service
@Slf4j
public class LibreHardwareMonitorService {

    private static final String DEFAULT_BASE_URL = "http://localhost:8085";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
    private final String baseUrl;

    public LibreHardwareMonitorService(
            @Value("${librehardwaremonitor.url:http://localhost:8085}") String url) {
        this.baseUrl = url != null && !url.isBlank() ? url.replaceAll("/+$", "") : DEFAULT_BASE_URL;
    }

    @PostConstruct
    void logConfig() {
        log.info("LibreHardwareMonitor URL: {} (temps will show when LHM remote server is running there)", baseUrl + "/data.json");
    }
    private volatile long lastFetchTime = 0;
    private volatile Double lastCpuTemp = null;
    private volatile Double lastGpuTemp = null;
    private volatile Double lastGpuLoad = null;
    private static final long CACHE_MS = 1000;
    /** Log "not reachable" at most once per minute to avoid console spam */
    private volatile long lastNotReachableLogTime = 0;
    private volatile long lastNoTempsWarnTime = 0;
    private static final long NOT_REACHABLE_LOG_INTERVAL_MS = 60_000;
    private volatile String lastError = null;
    private volatile boolean lastHttpOk = false;
    /** Per-GPU temperature and load by LHM device name (e.g. "NVIDIA GeForce GTX 1650", "AMD Radeon(TM) Graphics"). */
    private volatile Map<String, Double> gpuTempsByName = new ConcurrentHashMap<>();
    private volatile Map<String, Double> gpuLoadsByName = new ConcurrentHashMap<>();

    /**
     * Returns CPU temperature from LibreHardwareMonitor if available; null otherwise.
     */
    public Double getCpuTemperature() {
        fetchIfNeeded();
        return lastCpuTemp;
    }

    /**
     * Returns GPU temperature from LibreHardwareMonitor if available; null otherwise.
     */
    public Double getGpuTemperature() {
        fetchIfNeeded();
        return lastGpuTemp;
    }

    /**
     * Returns GPU load (usage %) from LibreHardwareMonitor if available; null otherwise.
     * Value is 0–100 when present.
     */
    public Double getGpuLoad() {
        fetchIfNeeded();
        return lastGpuLoad;
    }

    /**
     * Returns GPU temperature for the given OSHI card name by matching LHM device names.
     * E.g. cardName "AMD Radeon(TM) Graphics" matches LHM key "AMD Radeon(TM) Graphics".
     */
    public Double getGpuTemperatureByName(String cardName) {
        fetchIfNeeded();
        return matchGpuByName(gpuTempsByName, cardName);
    }

    /**
     * Returns GPU load (usage %) for the given OSHI card name by matching LHM device names.
     */
    public Double getGpuLoadByName(String cardName) {
        fetchIfNeeded();
        return matchGpuByName(gpuLoadsByName, cardName);
    }

    /** For debugging: returns whether LHM was reachable and last temps/error. */
    public LhmStatus getStatus() {
        fetchIfNeeded();
        boolean ok = lastCpuTemp != null || lastGpuTemp != null;
        return new LhmStatus(ok, lastCpuTemp, lastGpuTemp, lastGpuLoad, lastError, lastHttpOk);
    }

    public static record LhmStatus(boolean reachable, Double cpuTemp, Double gpuTemp, Double gpuLoad, String error, boolean httpOk) {}

    /** Returns top-level keys of LHM JSON for debugging parser (e.g. ["Children"], ["Nodes"]). */
    public java.util.List<String> getJsonTopLevelKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/data.json"))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return java.util.List.of("HTTP " + response.statusCode());
            JsonNode root = MAPPER.readTree(response.body());
            if (root == null) return java.util.List.of();
            java.util.List<String> keys = new java.util.ArrayList<>();
            root.fieldNames().forEachRemaining(keys::add);
            return keys;
        } catch (Exception e) {
            return java.util.List.of("error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /** Returns first N chars of LHM data.json to inspect actual structure (for parser fix). */
    public String getJsonSample(int maxChars) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/data.json"))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return "HTTP " + response.statusCode();
            String body = response.body();
            if (body == null) return "null";
            return body.length() <= maxChars ? body : body.substring(0, maxChars) + "\n... (truncated, total " + body.length() + " chars)";
        } catch (Exception e) {
            return "error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private void fetchIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime < CACHE_MS) {
            return;
        }
        lastFetchTime = now;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/data.json"))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            lastHttpOk = (response.statusCode() == 200);
            if (response.statusCode() != 200) {
                lastCpuTemp = null;
                lastGpuTemp = null;
                lastGpuLoad = null;
                lastHttpOk = false;
                lastError = "HTTP " + response.statusCode();
                log.debug("LibreHardwareMonitor returned status {}", response.statusCode());
                return;
            }
            String body = response.body();
            parseAndStoreTemps(body);
            lastError = null;
            if (lastCpuTemp != null || lastGpuTemp != null) {
                log.info("LibreHardwareMonitor temps: CPU={} °C, GPU={} °C", lastCpuTemp, lastGpuTemp);
            } else if (body != null && body.length() > 0 && (now - lastNoTempsWarnTime >= NOT_REACHABLE_LOG_INTERVAL_MS)) {
                lastNoTempsWarnTime = now;
                log.warn("LibreHardwareMonitor returned data but no temps parsed. Length: {} chars. Open http://localhost:8081/api/lhm-sample to see JSON structure.", body.length());
            }
        } catch (Exception e) {
            lastCpuTemp = null;
            lastGpuTemp = null;
            lastGpuLoad = null;
            lastHttpOk = false;
            lastError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            long nowErr = System.currentTimeMillis();
            if (nowErr - lastNotReachableLogTime >= NOT_REACHABLE_LOG_INTERVAL_MS) {
                lastNotReachableLogTime = nowErr;
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.info("LibreHardwareMonitor not reachable at {} (start LHM and enable Options -> Remote web server -> Run): {}", baseUrl, msg);
            }
        }
    }

    private void parseAndStoreTemps(String json) {
        lastCpuTemp = null;
        lastGpuTemp = null;
        lastGpuLoad = null;
        Map<String, Double> tempsByName = new HashMap<>();
        Map<String, Double> loadsByName = new HashMap<>();
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root == null) return;
            // Unwrap: root may be { "Children": [...] }, or { "Computer": { "Children": [...] } }, or array
            JsonNode children = root.isArray() ? root : root.get("Children");
            if (children == null && root.isObject()) {
                var it = root.fields();
                if (it.hasNext()) children = it.next().getValue().get("Children");
            }
            if (children != null && children.isArray()) {
                for (JsonNode child : children) {
                    collectTempsFromNode(child, "", "", null, tempsByName, loadsByName);
                }
            }
            // Also try flat "Sensors" array if present
            JsonNode sensors = root.get("Sensors");
            if (sensors != null && sensors.isArray()) {
                for (JsonNode s : sensors) {
                    String name = nameOf(s);
                    String type = typeOf(s);
                    Double val = valueOf(s);
                    if (val == null) continue;
                    String sensorId = sensorIdOf(s);
                    if ("Temperature".equalsIgnoreCase(type)) {
                        if (name != null) {
                            String n = name.toLowerCase(Locale.ROOT);
                            if (n.contains("cpu") && !n.contains("gpu") && (lastCpuTemp == null || val > lastCpuTemp)) lastCpuTemp = val;
                            if ((n.contains("gpu") || n.contains("graphics") || n.contains("radeon") || n.contains("nvidia") || n.contains("geforce")) && (lastGpuTemp == null || val > lastGpuTemp)) lastGpuTemp = val;
                        }
                        if (sensorId != null && val > 0 && val < 150) {
                            String idLower = sensorId.toLowerCase(Locale.ROOT);
                            if (idLower.contains("/gpu-nvidia/0/")) tempsByName.put("nvidia", val);
                            else if (idLower.contains("/gpu-amd/0/")) tempsByName.put("amd", val);
                        }
                    } else if ("Load".equalsIgnoreCase(type)) {
                        if (name != null) {
                            String n = name.toLowerCase(Locale.ROOT);
                            if ((n.contains("gpu") || n.contains("graphics") || n.contains("radeon") || n.contains("nvidia") || n.contains("geforce")) && val >= 0 && val <= 100) {
                                if (lastGpuLoad == null || val > lastGpuLoad) lastGpuLoad = val;
                            }
                        }
                        if (sensorId != null && val >= 0 && val <= 100) {
                            String idLower = sensorId.toLowerCase(Locale.ROOT);
                            if (idLower.contains("/gpu-nvidia/0/")) loadsByName.put("nvidia", val);
                            else if (idLower.contains("/gpu-amd/0/")) loadsByName.put("amd", val);
                        }
                    }
                }
            }
            gpuTempsByName = new ConcurrentHashMap<>(tempsByName);
            gpuLoadsByName = new ConcurrentHashMap<>(loadsByName);
            // Backward compat: set single lastGpuTemp/lastGpuLoad from first entry if any
            if (lastGpuTemp == null && !tempsByName.isEmpty()) {
                lastGpuTemp = tempsByName.values().stream().max(Double::compareTo).orElse(null);
            }
            if (lastGpuLoad == null && !loadsByName.isEmpty()) {
                lastGpuLoad = loadsByName.values().stream().max(Double::compareTo).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Failed to parse LibreHardwareMonitor JSON: {}", e.getMessage());
        }
    }

    /** Recursively walk tree; pathLower is combined name path for CPU/GPU detection. currentGpuName = LHM device name when under a GPU section. */
    private void collectTempsFromNode(JsonNode node, String pathText, String pathLower, String currentGpuName,
                                      Map<String, Double> gpuTempsByName, Map<String, Double> gpuLoadsByName) {
        String text = textOf(node);
        String id = identifierOf(node);
        String combinedLower = (pathLower + " " + text + " " + id).toLowerCase(Locale.ROOT);
        String type = typeOf(node);
        Double val = valueOf(node);
        JsonNode children = node.get("Children");

        // Detect GPU device section: node has Children and text looks like a GPU name (not a sensor like "GPU Core" only)
        String nextGpuName = currentGpuName;
        if (children != null && children.isArray() && text != null && !text.isEmpty()) {
            String tLower = text.toLowerCase(Locale.ROOT);
            boolean looksLikeGpuDevice = (tLower.contains("nvidia") || tLower.contains("geforce") || tLower.contains("radeon") || (tLower.contains("amd") && tLower.contains("graphics")));
            if (looksLikeGpuDevice) nextGpuName = text.trim();
        }

        // Sensor name clearly indicates CPU (core/package/tctl/ccd) or GPU (gpu/graphics in sensor name)
        boolean isCpuSensor = combinedLower.contains("core #") || combinedLower.contains("package") || combinedLower.contains("tctl")
                || combinedLower.contains("tdie") || combinedLower.contains("ccd1") || combinedLower.contains("ccd2") || combinedLower.contains("ccd ")
                || combinedLower.contains("core (smu)") || combinedLower.contains("cpu package");
        boolean isGpuSensor = (text.toLowerCase(Locale.ROOT).contains("gpu") || text.toLowerCase(Locale.ROOT).contains("graphics"));
        boolean looksLikeCpuSection = combinedLower.contains("cpu") || combinedLower.contains("ryzen") || combinedLower.contains("intel")
                || combinedLower.contains("core") || combinedLower.contains("package");
        boolean isCpu = looksLikeCpuSection && (!combinedLower.contains("gpu") && !combinedLower.contains("graphics") || isCpuSensor)
                || (type != null && "Temperature".equalsIgnoreCase(type) && (combinedLower.contains("ryzen") || combinedLower.contains("intel")) && !isGpuSensor);
        boolean isGpu = (combinedLower.contains("gpu") || combinedLower.contains("nvidia") || combinedLower.contains("radeon") || combinedLower.contains("graphics"))
                && !combinedLower.contains("core #") && (isGpuSensor || !combinedLower.contains("ryzen") && !combinedLower.contains("intel"));
        boolean isTemp = (type != null && ("Temperature".equalsIgnoreCase(type) || "Temp".equalsIgnoreCase(type)))
                || combinedLower.contains("temperature") || combinedLower.contains("temp");

        // Per-GPU: store temp/load by current GPU device name when under a GPU section
        if (currentGpuName != null && !currentGpuName.isEmpty()) {
            if (val != null && val > 0 && val < 150 && isTemp && (isGpuSensor || isGpu)) {
                gpuTempsByName.put(currentGpuName, val);
            }
            if (type != null && "Load".equalsIgnoreCase(type) && val != null && val >= 0 && val <= 100 && isGpu) {
                gpuLoadsByName.put(currentGpuName, val);
            }
        }

        if (val != null && val > 0 && val < 150) {
            boolean acceptAsTemp = isTemp || (type == null && (combinedLower.contains("package") || combinedLower.contains("core")));
            if (acceptAsTemp) {
                if (isCpuSensor) {
                    if (lastCpuTemp == null || val > lastCpuTemp) lastCpuTemp = val;
                } else if (isGpuSensor) {
                    if (lastGpuTemp == null || val > lastGpuTemp) lastGpuTemp = val;
                } else if (isCpu && isGpu) {
                    if (lastCpuTemp == null || val > lastCpuTemp) lastCpuTemp = val;
                } else if (isCpu) {
                    if (lastCpuTemp == null || val > lastCpuTemp) lastCpuTemp = val;
                } else if (isGpu) {
                    if (lastGpuTemp == null || val > lastGpuTemp) lastGpuTemp = val;
                }
            }
        }
        boolean isLoad = type != null && "Load".equalsIgnoreCase(type);
        if (isLoad && val != null && val >= 0 && val <= 100 && isGpu) {
            if (lastGpuLoad == null || val > lastGpuLoad) lastGpuLoad = val;
        }
        if (children != null && children.isArray()) {
            for (JsonNode c : children) {
                collectTempsFromNode(c, pathText + " " + text, combinedLower, nextGpuName, gpuTempsByName, gpuLoadsByName);
            }
        }
    }

    /** Find a value in the map by best match to OSHI card name (exact, contains, or normalized). */
    private static Double matchGpuByName(Map<String, Double> map, String cardName) {
        if (map == null || cardName == null || cardName.isEmpty()) return null;
        String key = cardName.trim();
        if (map.containsKey(key)) return map.get(key);
        String keyLower = key.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Double> e : map.entrySet()) {
            String k = e.getKey();
            if (k == null) continue;
            String kLower = k.toLowerCase(Locale.ROOT);
            if (kLower.equals(keyLower)) return e.getValue();
            if (kLower.contains(keyLower) || keyLower.contains(kLower)) return e.getValue();
        }
        return null;
    }

    private static String identifierOf(JsonNode n) {
        if (n == null) return "";
        JsonNode t = n.get("Identifier");
        if (t != null && t.isTextual()) return t.asText();
        t = n.get("Id");
        if (t != null && t.isTextual()) return t.asText();
        return "";
    }

    private static String textOf(JsonNode n) {
        if (n == null) return "";
        JsonNode t = n.get("Text");
        if (t != null && t.isTextual()) return t.asText();
        t = n.get("Name");
        if (t != null && t.isTextual()) return t.asText();
        return "";
    }

    private static String nameOf(JsonNode n) {
        if (n == null) return null;
        JsonNode t = n.get("Name");
        if (t != null && t.isTextual()) return t.asText();
        t = n.get("Text");
        if (t != null && t.isTextual()) return t.asText();
        return null;
    }

    private static String sensorIdOf(JsonNode n) {
        if (n == null) return null;
        JsonNode t = n.get("SensorId");
        if (t != null && t.isTextual()) return t.asText();
        t = n.get("Identifier");
        if (t != null && t.isTextual()) return t.asText();
        return null;
    }

    private static String typeOf(JsonNode n) {
        if (n == null) return null;
        JsonNode t = n.get("SensorType");
        if (t != null && t.isTextual()) return t.asText();
        t = n.get("Type");
        if (t != null && t.isTextual()) return t.asText();
        t = n.get("type");
        if (t != null && t.isTextual()) return t.asText();
        return null;
    }

    private static Double valueOf(JsonNode n) {
        if (n == null) return null;
        JsonNode v = n.get("Value");
        if (v == null) v = n.get("value");
        if (v == null) v = n.get("CurrentValue");
        if (v == null) return null;
        if (v.isNumber()) return v.doubleValue();
        if (v.isTextual()) {
            String s = v.asText().trim();
            if (s.isEmpty()) return null;
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) { }
            // LHM sends formatted strings like "45.0 °C" or "1234 RPM" - extract first number
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[+-]?\\d+(?:\\.\\d+)?").matcher(s);
            if (matcher.find()) {
                try {
                    return Double.parseDouble(matcher.group());
                } catch (NumberFormatException ignored) { }
            }
        }
        return null;
    }
}
