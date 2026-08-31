package com.training.notification;

import com.training.entity.Course;
import com.training.entity.Enrollment;
import com.training.entity.Faculty;
import com.training.entity.Payment;
import com.training.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public void notifyStudentEnrollmentActivated(User student, Course course, Enrollment enrollment) {
        String studentEmail = student != null ? student.getEmail() : "unknown";
        String courseName = course != null ? course.getName() : "course";
        String expDate = enrollment != null && enrollment.getExpiryDate() != null 
                ? enrollment.getExpiryDate().toString() : "N/A";

        log.info("[NOTIFICATION] Enrollment activated for student: {} in course: {}. Valid until: {}",
                studentEmail, courseName, expDate);

        sendEmailSafely(
                studentEmail,
                "Your Course Enrollment has been Activated - " + courseName,
                "Hello " + (student != null ? student.getFullName() : "Student") + ",\n\n"
                        + "Your enrollment for course '" + courseName + "' has been successfully activated.\n"
                        + "Enrollment Code: " + (enrollment != null ? enrollment.getEnrollmentCode() : "") + "\n"
                        + "Expiry Date: " + expDate + "\n\n"
                        + "You now have full access to your lectures."
        );
    }

    @Override
    public void notifyFacultyStudentAssigned(Faculty faculty, User student, Course course) {
        String facultyEmail = (faculty != null && faculty.getUser() != null) 
                ? faculty.getUser().getEmail() : null;
        String studentName = student != null ? student.getFullName() : "Student";
        String courseName = course != null ? course.getName() : "Course";

        log.info("[NOTIFICATION] New student {} assigned to faculty {} for course {}",
                studentName, facultyEmail, courseName);

        if (facultyEmail != null) {
            sendEmailSafely(
                    facultyEmail,
                    "New Student Assigned - " + courseName,
                    "Hello Professor,\n\n"
                            + "A new student, " + studentName + ", has been enrolled and assigned to your course '"
                            + courseName + "'."
            );
        }
    }

    @Override
    public void notifyExecutorOnboardingCompleted(User executor, User student, Course course) {
        String executorEmail = executor != null ? executor.getEmail() : null;
        String studentName = student != null ? student.getFullName() : "Student";
        String courseName = course != null ? course.getName() : "Course";

        log.info("[NOTIFICATION] Onboarding workflow completed by executor {} for student {} in {}",
                executorEmail, studentName, courseName);
    }

    @Override
    public void notifyPaymentStatus(User student, Payment payment) {
        String studentEmail = student != null ? student.getEmail() : "unknown";
        String txnId = payment != null ? payment.getTransactionId() : "N/A";
        String status = payment != null && payment.getPaymentStatus() != null 
                ? payment.getPaymentStatus().name() : "UNKNOWN";

        log.info("[NOTIFICATION] Payment status update for student: {}. Txn: {}, Status: {}",
                studentEmail, txnId, status);
    }

    private void sendEmailSafely(String to, String subject, String body) {
        if (mailSender == null || to == null || to.trim().isEmpty() || !to.contains("@")) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to dispatch email notification to {}: {}", to, e.getMessage());
        }
    }
}
