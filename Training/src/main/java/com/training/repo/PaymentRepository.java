package com.training.repo;

import com.training.entity.Course;
import com.training.entity.Payment;
import com.training.entity.User;
import com.training.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByStudent(User student);
    List<Payment> findByStudentAndCourseAndPaymentStatus(User student, Course course, PaymentStatus paymentStatus);
}
