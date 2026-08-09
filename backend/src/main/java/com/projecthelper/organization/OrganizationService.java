package com.projecthelper.organization;

import com.projecthelper.common.BusinessException;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import com.projecthelper.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public List<OrganizationUnit> list() {
        return organizationRepository.findAllByOrderByLevelAscSortOrderAscNameAsc();
    }

    public OrganizationUnit create(OrganizationCommand command) {
        validateParent(command.type(), command.parentId(), null);
        Instant now = Instant.now();
        OrganizationUnit unit = OrganizationUnit.builder().name(command.name().trim()).type(command.type())
                .parentId(command.parentId()).sortOrder(command.sortOrder()).active(true)
                .createdAt(now).updatedAt(now).build();
        applyHierarchy(unit);
        return organizationRepository.save(unit);
    }

    public OrganizationUnit update(String id, OrganizationCommand command) {
        OrganizationUnit unit = get(id);
        validateParent(command.type(), command.parentId(), id);
        unit.setName(command.name().trim());
        unit.setType(command.type());
        unit.setParentId(command.parentId());
        unit.setSortOrder(command.sortOrder());
        unit.setUpdatedAt(Instant.now());
        applyHierarchy(unit);
        return organizationRepository.save(unit);
    }

    public void delete(String id) {
        if (organizationRepository.existsByParentId(id)) throw BusinessException.conflict("Delete child majors first");
        boolean used = userRepository.findAll().stream().anyMatch(u -> id.equals(u.getCollegeId()) || id.equals(u.getMajorId()));
        if (used) throw BusinessException.conflict("This organization still has members and cannot be deleted");
        organizationRepository.delete(get(id));
    }

    public List<CollegeNode> memberTree() {
        List<OrganizationUnit> units = list().stream().filter(OrganizationUnit::isActive).toList();
        Map<String, List<User>> mentorsByMajor = new HashMap<>();
        for (User user : userRepository.findAll()) {
            if (user.getRole() == UserRole.MENTOR && user.getStatus() == UserStatus.ACTIVE && user.getMajorId() != null) {
                mentorsByMajor.computeIfAbsent(user.getMajorId(), key -> new ArrayList<>()).add(user);
            }
        }
        return units.stream().filter(u -> u.getType() == OrganizationType.COLLEGE).map(college -> {
            List<MajorNode> majors = units.stream()
                    .filter(u -> u.getType() == OrganizationType.MAJOR && college.getId().equals(u.getParentId()))
                    .map(major -> new MajorNode(major.getId(), major.getName(), mentorsByMajor
                            .getOrDefault(major.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(User::getRealName))
                            .map(mentor -> new MentorNode(mentor.getId(), mentor.getRealName(),
                                    userRepository.findByMentorIdOrderByRealName(mentor.getId()).stream()
                                            .filter(student -> student.getStatus() == UserStatus.ACTIVE)
                                            .map(student -> new StudentNode(student.getId(), student.getRealName(), student.getStudentNo()))
                                            .toList())).toList())).toList();
            return new CollegeNode(college.getId(), college.getName(), majors);
        }).toList();
    }

    public OrganizationUnit get(String id) {
        return organizationRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Organization not found"));
    }

    private void validateParent(OrganizationType type, String parentId, String selfId) {
        if (type == OrganizationType.COLLEGE) {
            if (parentId != null && !parentId.isBlank()) throw BusinessException.badRequest("A college cannot have a parent organization");
            return;
        }
        if (parentId == null || parentId.isBlank() || parentId.equals(selfId)) {
            throw BusinessException.badRequest("A major must belong to a college");
        }
        if (get(parentId).getType() != OrganizationType.COLLEGE) throw BusinessException.badRequest("A major parent must be a college");
    }

    private void applyHierarchy(OrganizationUnit unit) {
        if (unit.getType() == OrganizationType.COLLEGE) {
            unit.setParentId(null);
            unit.setLevel(1);
            unit.setPath(":");
        } else {
            OrganizationUnit parent = get(unit.getParentId());
            unit.setLevel(2);
            unit.setPath(":" + parent.getId() + ":");
        }
    }

    public record OrganizationCommand(String name, OrganizationType type, String parentId, int sortOrder) {}
    public record CollegeNode(String id, String name, List<MajorNode> majors) {}
    public record MajorNode(String id, String name, List<MentorNode> mentors) {}
    public record MentorNode(String id, String name, List<StudentNode> students) {}
    public record StudentNode(String id, String name, String studentNo) {}
}
