package com.training.config;

import com.training.entity.Course;
import com.training.entity.CoursePlan;
import com.training.entity.Faculty;
import com.training.entity.User;
import com.training.enums.CourseCategory;
import com.training.enums.CourseStatus;
import com.training.enums.PlanDuration;
import com.training.enums.Role;
import com.training.repo.CoursePlanRepository;
import com.training.repo.CourseRepository;
import com.training.repo.FacultyRepository;
import com.training.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Initialize Default Faculty Accounts if empty
        Faculty defaultFaculty = null;

        if (facultyRepository.count() == 0) {
            User facUser1 = User.builder()
                    .fullName("Pratiksha")
                    .email("pratiksha@codextechnology.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.FACULTY)
                    .status("ACTIVE")
                    .phone("9876543210")
                    .build();
            facUser1 = userRepository.save(facUser1);

            Faculty fac1 = Faculty.builder()
                    .user(facUser1)
                    .facultyCode("FAC-2001")
                    .department("Software Engineering & Cloud")
                    .status("ACTIVE")
                    .build();
            defaultFaculty = facultyRepository.save(fac1);

            User facUser2 = User.builder()
                    .fullName("Dr. Rajesh Sharma")
                    .email("rajesh.sharma@codextechnology.com")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.FACULTY)
                    .status("ACTIVE")
                    .phone("9812345678")
                    .build();
            facUser2 = userRepository.save(facUser2);

            Faculty fac2 = Faculty.builder()
                    .user(facUser2)
                    .facultyCode("FAC-2002")
                    .department("Computer Science & Architecture")
                    .status("ACTIVE")
                    .build();
            facultyRepository.save(fac2);

            log.info("Default Faculty accounts initialized.");
        } else {
            defaultFaculty = facultyRepository.findAll().get(0);
        }

        // 2. Initialize Default Courses in MySQL if database courses table is empty
        if (courseRepository.count() == 0) {
            // Course 1: Full Stack Web Development
            Course course1 = Course.builder()
                    .courseCode("COURSE-1001")
                    .name("Full Stack Web Development")
                    .description("Comprehensive hands-on training in React, Node.js, Express, and MySQL/PostgreSQL architectures.")
                    .category(CourseCategory.WEB_DEVELOPMENT)
                    .status(CourseStatus.ACTIVE)
                    .faculty(defaultFaculty)
                    .plans(new ArrayList<>())
                    .curriculum(new ArrayList<>())
                    .build();
            course1 = courseRepository.save(course1);

            CoursePlan plan1_1 = CoursePlan.builder()
                    .course(course1)
                    .duration(PlanDuration.ONE_MONTH)
                    .price(new BigDecimal("7000"))
                    .currency("INR")
                    .build();
            CoursePlan plan1_2 = CoursePlan.builder()
                    .course(course1)
                    .duration(PlanDuration.TWO_MONTHS)
                    .price(new BigDecimal("14000"))
                    .currency("INR")
                    .build();
            CoursePlan plan1_3 = CoursePlan.builder()
                    .course(course1)
                    .duration(PlanDuration.THREE_MONTHS)
                    .price(new BigDecimal("21000"))
                    .currency("INR")
                    .build();
            coursePlanRepository.save(plan1_1);
            coursePlanRepository.save(plan1_2);
            coursePlanRepository.save(plan1_3);

            // Course 2: Java Microservices & Cloud Architecture
            Course course2 = Course.builder()
                    .courseCode("COURSE-1002")
                    .name("Java Microservices & Cloud Architecture")
                    .description("Master Spring Boot 3, Spring Cloud, Docker, Kubernetes, and enterprise microservice patterns.")
                    .category(CourseCategory.SOFTWARE_ENGINEERING)
                    .status(CourseStatus.ACTIVE)
                    .faculty(defaultFaculty)
                    .plans(new ArrayList<>())
                    .curriculum(new ArrayList<>())
                    .build();
            course2 = courseRepository.save(course2);

            CoursePlan plan2_1 = CoursePlan.builder()
                    .course(course2)
                    .duration(PlanDuration.ONE_MONTH)
                    .price(new BigDecimal("8000"))
                    .currency("INR")
                    .build();
            CoursePlan plan2_2 = CoursePlan.builder()
                    .course(course2)
                    .duration(PlanDuration.TWO_MONTHS)
                    .price(new BigDecimal("16000"))
                    .currency("INR")
                    .build();
            CoursePlan plan2_3 = CoursePlan.builder()
                    .course(course2)
                    .duration(PlanDuration.THREE_MONTHS)
                    .price(new BigDecimal("24000"))
                    .currency("INR")
                    .build();
            coursePlanRepository.save(plan2_1);
            coursePlanRepository.save(plan2_2);
            coursePlanRepository.save(plan2_3);

            // Course 3: Data Science & AI Engineering
            Course course3 = Course.builder()
                    .courseCode("COURSE-1003")
                    .name("Data Science & AI Engineering")
                    .description("Machine Learning models, Deep Learning, Python Data Pipelines, LLM Integration, and Analytics.")
                    .category(CourseCategory.DATA_SCIENCE)
                    .status(CourseStatus.ACTIVE)
                    .faculty(defaultFaculty)
                    .plans(new ArrayList<>())
                    .curriculum(new ArrayList<>())
                    .build();
            course3 = courseRepository.save(course3);

            CoursePlan plan3_1 = CoursePlan.builder()
                    .course(course3)
                    .duration(PlanDuration.ONE_MONTH)
                    .price(new BigDecimal("9000"))
                    .currency("INR")
                    .build();
            CoursePlan plan3_2 = CoursePlan.builder()
                    .course(course3)
                    .duration(PlanDuration.TWO_MONTHS)
                    .price(new BigDecimal("18000"))
                    .currency("INR")
                    .build();
            CoursePlan plan3_3 = CoursePlan.builder()
                    .course(course3)
                    .duration(PlanDuration.THREE_MONTHS)
                    .price(new BigDecimal("27000"))
                    .currency("INR")
                    .build();
            coursePlanRepository.save(plan3_1);
            coursePlanRepository.save(plan3_2);
            coursePlanRepository.save(plan3_3);

            log.info("Default courses initialized successfully in MySQL database.");
        }
    }
}
