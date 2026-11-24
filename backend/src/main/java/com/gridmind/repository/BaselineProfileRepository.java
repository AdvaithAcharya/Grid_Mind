package com.gridmind.repository;

import com.gridmind.model.ApplianceType;
import com.gridmind.model.BaselineProfileDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BaselineProfileRepository extends MongoRepository<BaselineProfileDoc, String> {
    Optional<BaselineProfileDoc> findByApplianceTypeAndHourAndIsWeekend(ApplianceType applianceType, int hour, boolean isWeekend);
    List<BaselineProfileDoc> findByApplianceType(ApplianceType applianceType);
}
