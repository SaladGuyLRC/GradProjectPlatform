package com.projecthelper.organization;

import com.projecthelper.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;

    @GetMapping("/api/members/tree")
    public ApiResponse<List<OrganizationService.CollegeNode>> tree() {
        return ApiResponse.success(organizationService.memberTree());
    }

    @GetMapping("/api/admin/organizations")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OrganizationUnit>> list() {
        return ApiResponse.success(organizationService.list());
    }

    @PostMapping("/api/admin/organizations")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrganizationUnit> create(@Valid @RequestBody OrganizationRequest request) {
        return ApiResponse.success(organizationService.create(request.command()));
    }

    @PutMapping("/api/admin/organizations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrganizationUnit> update(@PathVariable String id, @Valid @RequestBody OrganizationRequest request) {
        return ApiResponse.success(organizationService.update(id, request.command()));
    }

    @DeleteMapping("/api/admin/organizations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        organizationService.delete(id);
        return ApiResponse.success(null);
    }

    public record OrganizationRequest(@NotBlank String name, @NotNull OrganizationType type,
                                      String parentId, int sortOrder) {
        OrganizationService.OrganizationCommand command() {
            return new OrganizationService.OrganizationCommand(name, type, parentId, sortOrder);
        }
    }
}
