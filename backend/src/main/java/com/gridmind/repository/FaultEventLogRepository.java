package com.gridmind.repository;

import com.gridmind.model.FaultEventLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaultEventLogRepository extends MongoRepository<FaultEventLog, String> {
    List<FaultEventLog> findByApplianceIdOrderByTimestampDesc(String applianceId, Pageable pageable);
    List<FaultEventLog> findByResolvedFalseOrderByTimestampDesc();
}
