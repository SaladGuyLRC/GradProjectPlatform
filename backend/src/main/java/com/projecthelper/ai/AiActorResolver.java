package com.projecthelper.ai;

import com.projecthelper.common.BusinessException;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiActorResolver {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public User targetStudent(String studentName, boolean mentorRequiresName) {
        User actor = currentUserService.require();
        if (actor.getRole() == UserRole.STUDENT) {
            if (studentName != null && !studentName.isBlank() && !actor.getRealName().equals(studentName.trim())) {
                throw BusinessException.forbidden("Students can only query their own data");
            }
            return actor;
        }
        if (actor.getRole() == UserRole.MENTOR) {
            if ((studentName == null || studentName.isBlank()) && mentorRequiresName) {
                throw BusinessException.badRequest("Provide the student's name");
            }
            List<User> students = studentName == null || studentName.isBlank()
                    ? userRepository.findByMentorIdOrderByRealName(actor.getId())
                    : userRepository.findByMentorIdAndRealName(actor.getId(), studentName.trim());
            if (students.isEmpty()) throw BusinessException.notFound("No assigned student was found");
            if (students.size() > 1) throw BusinessException.conflict("The student is not unique; provide more detail");
            return students.getFirst();
        }
        throw BusinessException.forbidden("Administrators cannot use AI to read student business data");
    }
}
