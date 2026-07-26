package com.projecthelper.organization;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrganizationRepository extends MongoRepository<OrganizationUnit, String> {
    List<OrganizationUnit> findAllByOrderByLevelAscSortOrderAscNameAsc();
    List<OrganizationUnit> findByParentId(String parentId);
    boolean existsByParentId(String parentId);
}
