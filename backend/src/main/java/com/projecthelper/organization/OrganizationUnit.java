package com.projecthelper.organization;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document("organization_units")
@CompoundIndex(name = "parent_name_unique", def = "{'parentId':1,'name':1}", unique = true)
public class OrganizationUnit {
    @Id
    private String id;
    private String name;
    private OrganizationType type;
    private String parentId;
    private String path;
    private int level;
    private int sortOrder;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
