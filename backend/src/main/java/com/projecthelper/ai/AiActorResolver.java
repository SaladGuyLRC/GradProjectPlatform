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
                throw BusinessException.forbidden("学生只能查询自己的数据");
            }
            return actor;
        }
        if (actor.getRole() == UserRole.MENTOR) {
            if ((studentName == null || studentName.isBlank()) && mentorRequiresName) {
                throw BusinessException.badRequest("请说明学生姓名");
            }
            List<User> students = studentName == null || studentName.isBlank()
                    ? userRepository.findByMentorIdOrderByRealName(actor.getId())
                    : userRepository.findByMentorIdAndRealName(actor.getId(), studentName.trim());
            if (students.isEmpty()) throw BusinessException.notFound("未找到名下学生");
            if (students.size() > 1) throw BusinessException.conflict("学生不唯一，请提供更明确的信息");
            return students.getFirst();
        }
        throw BusinessException.forbidden("管理员AI不读取学生业务数据");
    }
}
