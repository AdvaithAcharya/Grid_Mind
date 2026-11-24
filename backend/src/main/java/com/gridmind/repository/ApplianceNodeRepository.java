package com.gridmind.repository;

import com.gridmind.model.ApplianceNode;
import com.gridmind.model.ApplianceType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplianceNodeRepository extends MongoRepository<ApplianceNode, String> {
    Optional<ApplianceNode> findByApplianceType(ApplianceType applianceType);
}
