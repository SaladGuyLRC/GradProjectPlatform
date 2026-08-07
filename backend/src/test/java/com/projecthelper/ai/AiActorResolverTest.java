package com.projecthelper.ai;

import com.projecthelper.common.BusinessException;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiActorResolverTest {
    @Mock CurrentUserService currentUser;
    @Mock UserRepository users;

    @AfterEach void clearContext() { AiExecutionContext.clear(); }

    @Test
    void studentCannotResolveAnotherStudent() {
        User student = User.builder().id("s1").realName("Alice").role(UserRole.STUDENT).build();
        when(currentUser.require()).thenReturn(student);
        AiActorResolver resolver = new AiActorResolver(currentUser, users);

        assertEquals(student, resolver.targetStudent(null, true));
        assertThrows(BusinessException.class, () -> resolver.targetStudent("Bob", true));
    }

    @Test
    void mentorOnlyResolvesAssignedStudent() {
        User mentor = User.builder().id("m1").realName("Mentor").role(UserRole.MENTOR).build();
        User assigned = User.builder().id("s1").realName("Alice").mentorId("m1").role(UserRole.STUDENT).build();
        when(currentUser.require()).thenReturn(mentor);
        when(users.findByMentorIdAndRealName("m1", "Alice")).thenReturn(List.of(assigned));
        AiActorResolver resolver = new AiActorResolver(currentUser, users);

        assertEquals(assigned, resolver.targetStudent("Alice", true));
        when(users.findByMentorIdAndRealName("m1", "Bob")).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> resolver.targetStudent("Bob", true));
    }

    @Test
    void administratorCannotResolveStudentBusinessData() {
        User admin = User.builder().id("a1").realName("Admin").role(UserRole.ADMIN).build();
        when(currentUser.require()).thenReturn(admin);
        AiActorResolver resolver = new AiActorResolver(currentUser, users);

        assertThrows(BusinessException.class, () -> resolver.targetStudent(null, true));
    }
}
