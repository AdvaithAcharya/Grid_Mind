package com.gridmind.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridmind.model.ApplianceType;
import com.gridmind.model.BaselineProfileDoc;
import com.gridmind.repository.BaselineProfileRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BaselineProfileService {

    private static final Logger log = LoggerFactory.getLogger(BaselineProfileService.class);

    private static final Map<ApplianceType, String> TRAINED_SERIES_MAP = Map.of(
            ApplianceType.FAN, "ARCHIVE_2::FANS",
            ApplianceType.AC, "ARCHIVE_2::AC",
            ApplianceType.FRIDGE, "ARCHIVE_4::MYFRIDGE",
            ApplianceType.GEYSER, "ARCHIVE_3::RADIATOR_309",
            ApplianceType.KETTLE, "ARCHIVE_3::COFFEE_97",
            ApplianceType.TV, "ARCHIVE_2::TV"
    );

    private static final Map<ApplianceType, Double> ANCHOR_WATTS = Map.of(
            ApplianceType.FAN, 72.0,
            ApplianceType.AC, 1250.0,
            ApplianceType.FRIDGE, 155.0,
            ApplianceType.GEYSER, 1950.0,
            ApplianceType.KETTLE, 1800.0,
            ApplianceType.TV, 118.0
    );

    private static final Map<ApplianceType, Double> NOMINAL_PF = Map.of(
            ApplianceType.FAN, 0.85,
            ApplianceType.AC, 0.95,
            ApplianceType.FRIDGE, 0.82,
            ApplianceType.GEYSER, 0.99,
            ApplianceType.KETTLE, 1.00,
            ApplianceType.TV, 0.92
    );

    private static final Map<ApplianceType, Double> DAILY_KWH_ANCHOR = Map.of(
            ApplianceType.FAN, 0.72,
            ApplianceType.AC, 8.40,
            ApplianceType.FRIDGE, 1.65,
            ApplianceType.GEYSER, 3.60,
            ApplianceType.KETTLE, 0.45,
            ApplianceType.TV, 0.68
    );

    private final BaselineProfileRepository baselineProfileRepository;
    private final ObjectMapper objectMapper;

    @Value("${gridmind.baseline.profiles-path:ml/gridmind_baseline_profiles.json}")
    private String classpathProfilesPath;

    @Value("${gridmind.baseline.trained-file-path:../data/gridmind_baseline_profiles.json}")
    private String trainedFilePath;

    private final Map<String, BaselineProfileDoc> cache = new ConcurrentHashMap<>();
    private final Set<String> trainedSeriesIds = ConcurrentHashMap.newKeySet();
    private volatile int trainedRowsLoaded = 0;
    private volatile String activeTrainedSource = "classpath";

    public BaselineProfileService(BaselineProfileRepository baselineProfileRepository, ObjectMapper objectMapper) {
        this.baselineProfileRepository = baselineProfileRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadBaselines() {
        Map<String, List<TrainedRow>> trainedBySeries = loadTrainedDataset();
        Map<String, BaselineProfileDoc> classpathAnchors = loadClasspathAnchors();

        List<BaselineProfileDoc> allDocs = new ArrayList<>();

        for (ApplianceType type : ApplianceType.values()) {
            String mappedSeries = TRAINED_SERIES_MAP.get(type);
            List<TrainedRow> seriesRows = mappedSeries != null
                    ? trainedBySeries.getOrDefault(mappedSeries, Collections.emptyList())
                    : Collections.emptyList();

            double meanSeriesPower = seriesRows.stream()
                    .mapToDouble(TrainedRow::avgPowerW)
                    .filter(w -> w > 0.0)
                    .average()
                    .orElse(1.0);

            Map<String, TrainedRow> rowByHourWeekend = new HashMap<>();
            for (TrainedRow r : seriesRows) {
                rowByHourWeekend.put(r.hour() + ":" + (r.isWeekend() ? 1 : 0), r);
            }

            double anchorW = ANCHOR_WATTS.getOrDefault(type, 100.0);
            double pf = NOMINAL_PF.getOrDefault(type, 0.90);
            double dailyKwh = DAILY_KWH_ANCHOR.getOrDefault(type, 1.0);

            for (int isWeekendIdx = 0; isWeekendIdx <= 1; isWeekendIdx++) {
                boolean isWeekend = isWeekendIdx == 1;
                for (int hour = 0; hour < 24; hour++) {
                    String key = key(type, hour, isWeekend);
                    BaselineProfileDoc anchor = classpathAnchors.get(key);
                    TrainedRow trained = rowByHourWeekend.get(hour + ":" + isWeekendIdx);

                    double ratio = 1.0;
                    double p90Ratio = 1.32;
                    if (trained != null && meanSeriesPower > 0.0) {
                        ratio = Math.max(0.35, Math.min(1.65, trained.avgPowerW() / meanSeriesPower));
                        if (trained.medianKwh() > 0.0001 && trained.p90Kwh() >= trained.medianKwh()) {
                            p90Ratio = Math.max(1.12, Math.min(1.85, trained.p90Kwh() / trained.medianKwh()));
                        }
                    }

                    double diurnalFactor = computeDiurnalFactor(type, hour, isWeekend);
                    double expectedW = anchor != null
                            ? anchor.getExpectedWatts()
                            : round2(anchorW * (0.65 * diurnalFactor + 0.35 * ratio));

                    double hourFraction = (hour + 1) / 24.0;
                    double expectedP50 = anchor != null && anchor.getExpectedP50Kwh() > 0
                            ? round3(Math.max(anchor.getExpectedP50Kwh(), dailyKwh * hourFraction * ratio))
                            : round3(dailyKwh * hourFraction * (0.75 + 0.25 * ratio));

                    double expectedP90 = anchor != null && anchor.getExpectedP90Kwh() > expectedP50
                            ? round3(Math.max(anchor.getExpectedP90Kwh(), expectedP50 * p90Ratio))
                            : round3(expectedP50 * p90Ratio);

                    double nominalPf = anchor != null ? anchor.getNominalPowerFactor() : pf;

                    BaselineProfileDoc doc = BaselineProfileDoc.builder()
                            .id(type.name() + "_" + hour + "_" + (isWeekend ? "WE" : "WD"))
                            .applianceType(type)
                            .hour(hour)
                            .isWeekend(isWeekend)
                            .expectedWatts(expectedW)
                            .expectedP50Kwh(expectedP50)
                            .expectedP90Kwh(expectedP90)
                            .nominalPowerFactor(nominalPf)
                            .build();

                    cache.put(key, doc);
                    allDocs.add(doc);
                }
            }
        }

        try {
            if (baselineProfileRepository.count() == 0) {
                baselineProfileRepository.saveAll(allDocs);
                log.info("Seeded {} baseline profile documents into MongoDB Atlas.", allDocs.size());
            } else {
                baselineProfileRepository.saveAll(allDocs);
                log.info("Synchronized {} baseline profile documents with MongoDB Atlas.", allDocs.size());
            }
        } catch (Exception e) {
            log.warn("MongoDB baseline sync skipped (running from in-memory trained cache): {}", e.getMessage());
        }
    }

    public BaselineProfileDoc getCurrentBaseline(ApplianceType type, ZonedDateTime now) {
        int hour = now.getHour();
        DayOfWeek dow = now.getDayOfWeek();
        boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
        return getBaseline(type, hour, isWeekend);
    }

    public BaselineProfileDoc getBaseline(ApplianceType type, int hour, boolean isWeekend) {
        BaselineProfileDoc doc = cache.get(key(type, hour, isWeekend));
        if (doc != null) {
            return doc;
        }
        return BaselineProfileDoc.builder()
                .applianceType(type)
                .hour(hour)
                .isWeekend(isWeekend)
                .expectedWatts(ANCHOR_WATTS.getOrDefault(type, 100.0))
                .expectedP50Kwh(1.0)
                .expectedP90Kwh(1.35)
                .nominalPowerFactor(NOMINAL_PF.getOrDefault(type, 0.90))
                .build();
    }

    public List<BaselineProfileDoc> getProfilesForAppliance(ApplianceType type) {
        List<BaselineProfileDoc> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            BaselineProfileDoc doc = cache.get(key(type, hour, false));
            if (doc != null) {
                result.add(doc);
            }
        }
        result.sort(Comparator.comparingInt(BaselineProfileDoc::getHour));
        return result;
    }

    public Map<String, Object> getTrainedModelSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("activeSource", activeTrainedSource);
        summary.put("trainedRowsLoaded", trainedRowsLoaded);
        summary.put("trainedSeriesCount", trainedSeriesIds.size());
        summary.put("cachedApplianceProfiles", cache.size());
        summary.put("applianceSeriesMapping", TRAINED_SERIES_MAP);
        return summary;
    }

    private Map<String, List<TrainedRow>> loadTrainedDataset() {
        Map<String, List<TrainedRow>> map = new HashMap<>();
        File file = resolveTrainedFile();
        if (file == null || !file.exists()) {
            log.warn("Trained dataset file not found at {}; using classpath anchors.", trainedFilePath);
            return map;
        }

        try {
            JsonNode root = objectMapper.readTree(file);
            if (root != null && root.isArray()) {
                int count = 0;
                for (JsonNode node : root) {
                    if (!node.has("series_id")) {
                        continue;
                    }
                    String seriesId = node.get("series_id").asText();
                    int hour = node.path("hour").asInt(0);
                    boolean isWeekend = node.path("is_weekend").asInt(0) == 1;
                    double medianKwh = node.path("median_kwh").asDouble(0.0);
                    double p90Kwh = node.path("p90_kwh").asDouble(0.0);
                    double avgPowerW = node.path("avg_power_w").asDouble(0.0);

                    map.computeIfAbsent(seriesId, k -> new ArrayList<>())
                            .add(new TrainedRow(seriesId, hour, isWeekend, medianKwh, p90Kwh, avgPowerW));
                    trainedSeriesIds.add(seriesId);
                    count++;
                }
                trainedRowsLoaded = count;
                activeTrainedSource = file.getPath();
                log.info("Loaded {} trained baseline rows across {} series from {}",
                        count, trainedSeriesIds.size(), file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed reading trained dataset {}: {}", file.getAbsolutePath(), e.getMessage());
        }
        return map;
    }

    private File resolveTrainedFile() {
        List<String> candidates = List.of(
                trainedFilePath,
                "data/gridmind_baseline_profiles.json",
                "../data/gridmind_baseline_profiles.json"
        );
        for (String path : candidates) {
            if (path == null || path.isBlank()) continue;
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                return f;
            }
        }
        return null;
    }

    private Map<String, BaselineProfileDoc> loadClasspathAnchors() {
        Map<String, BaselineProfileDoc> anchors = new HashMap<>();
        try {
            ClassPathResource resource = new ClassPathResource(classpathProfilesPath);
            if (!resource.exists()) {
                return anchors;
            }
            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                if (root != null && root.isArray()) {
                    for (JsonNode node : root) {
                        if (!node.has("applianceType")) continue;
                        ApplianceType type = ApplianceType.valueOf(node.get("applianceType").asText());
                        int hour = node.path("hour").asInt(0);
                        boolean isWeekend = node.path("isWeekend").asBoolean(false);
                        BaselineProfileDoc doc = BaselineProfileDoc.builder()
                                .applianceType(type)
                                .hour(hour)
                                .isWeekend(isWeekend)
                                .expectedWatts(node.path("expectedWatts").asDouble(100.0))
                                .expectedP50Kwh(node.path("expectedP50Kwh").asDouble(0.5))
                                .expectedP90Kwh(node.path("expectedP90Kwh").asDouble(0.8))
                                .nominalPowerFactor(node.path("nominalPowerFactor").asDouble(0.9))
                                .build();
                        anchors.put(key(type, hour, isWeekend), doc);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not read classpath baseline anchors {}: {}", classpathProfilesPath, e.getMessage());
        }
        return anchors;
    }

    private double computeDiurnalFactor(ApplianceType type, int hour, boolean isWeekend) {
        return switch (type) {
            case AC -> (hour >= 12 && hour <= 23) ? 1.15 : (hour >= 6 && hour <= 10 ? 0.55 : 0.85);
            case FAN -> (hour >= 11 && hour <= 23) ? 1.05 : 0.90;
            case FRIDGE -> 0.85 + 0.25 * Math.sin((hour / 24.0) * Math.PI * 2);
            case GEYSER -> (hour >= 6 && hour <= 9) || (hour >= 19 && hour <= 21) ? 1.02 : 0.28;
            case KETTLE -> (hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 18) ? 1.0 : 0.15;
            case TV -> (hour >= 18 && hour <= 23) || (isWeekend && hour >= 10) ? 1.05 : 0.40;
        };
    }

    private String key(ApplianceType type, int hour, boolean isWeekend) {
        return type.name() + ":" + hour + ":" + (isWeekend ? "1" : "0");
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private record TrainedRow(String seriesId, int hour, boolean isWeekend,
                              double medianKwh, double p90Kwh, double avgPowerW) {}
}
