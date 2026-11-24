package com.gridmind.repository;

import com.gridmind.model.TelemetrySample;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TelemetrySampleRepository extends MongoRepository<TelemetrySample, String> {
    List<TelemetrySample> findByApplianceIdOrderByTimestampDesc(String applianceId, Pageable pageable);
    List<TelemetrySample> findByApplianceIdAndTimestampAfterOrderByTimestampAsc(String applianceId, Instant after);
    void deleteByTimestampBefore(Instant before);
}
