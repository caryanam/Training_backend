package com.training.notification;

import com.training.entity.Course;
import com.training.entity.Enrollment;
import com.training.entity.Faculty;
import com.training.entity.Payment;
import com.training.entity.User;

public interface NotificationService {
    void notifyStudentEnrollmentActivated(User student, Course course, Enrollment enrollment);
    void notifyFacultyStudentAssigned(Faculty faculty, User student, Course course);
    void notifyExecutorOnboardingCompleted(User executor, User student, Course course);
    void notifyPaymentStatus(User student, Payment payment);
}
