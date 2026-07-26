package com.projecthelper.organization;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends MongoRepository<OrganizationUnit, String> {
    List<OrganizationUnit> findAllByOrderByLevelAscSortOrderAscNameAsc();
    List<OrganizationUnit> findByParentId(String parentId);
    Optional<OrganizationUnit> findByTypeAndNameAndParentId(OrganizationType type, String name, String parentId);
    boolean existsByParentId(String parentId);
}
