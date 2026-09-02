package com.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.dto.request.*;
import com.training.enums.CourseCategory;
import com.training.enums.PlanDuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class StudentOnboardingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String executorToken;
    private String facultyToken;
    private String studentToken;

    private String courseId;
    private String planId;
    private String facultyId;
    private String leadId;
    private String studentProfileId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Admin Login
        LoginRequestDTO adminLogin = LoginRequestDTO.builder()
                .email("admin@gmail.com")
                .password("Admin@123")
                .build();

        MvcResult adminRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = extractDataField(adminRes, "token");

        // 2. Create Faculty
        CreateFacultyDTO facDto = CreateFacultyDTO.builder()
                .fullName("Prof. Ramesh Verma")
                .email("ramesh.faculty@eduflow.com")
                .phone("9811122233")
                .department("Computer Science")
                .password("Faculty@123")
                .build();
        MvcResult facRes = mockMvc.perform(post("/api/v1/admin/faculty")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facDto)))
                .andExpect(status().isCreated())
                .andReturn();
        facultyId = extractDataField(facRes, "facultyId");

        // Login as Faculty
        LoginRequestDTO facLogin = LoginRequestDTO.builder()
                .email("ramesh.faculty@eduflow.com")
                .password("Faculty@123")
                .build();
        MvcResult facLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facLogin)))
                .andExpect(status().isOk())
                .andReturn();
        facultyToken = extractDataField(facLoginRes, "token");

        // 3. Create Executor
        CreateExecutorDTO exeDto = CreateExecutorDTO.builder()
                .fullName("Anita Roy")
                .email("anita.executor@eduflow.com")
                .phone("9822233344")
                .password("Executor@123")
                .build();
        mockMvc.perform(post("/api/v1/admin/executors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exeDto)))
                .andExpect(status().isCreated());

        // Login as Executor
        LoginRequestDTO exeLogin = LoginRequestDTO.builder()
                .email("anita.executor@eduflow.com")
                .password("Executor@123")
                .build();
        MvcResult exeLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exeLogin)))
                .andExpect(status().isOk())
                .andReturn();
        executorToken = extractDataField(exeLoginRes, "token");

        // 4. Register a Student Lead
        RegisterStudentDTO stuDto = RegisterStudentDTO.builder()
                .fullName("Aarav Patel")
                .email("aarav.patel@test.com")
                .phone("9833344455")
                .password("Student@123")
                .interestedCourse("Full Stack Java Masterclass")
                .education("B.Tech IT")
                .city("Pune")
                .build();
        MvcResult stuRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stuDto)))
                .andExpect(status().isCreated())
                .andReturn();
        leadId = extractDataField(stuRes, "leadId");
        studentProfileId = extractDataField(stuRes, "profileId");

        // Login as Student
        LoginRequestDTO stuLogin = LoginRequestDTO.builder()
                .email("aarav.patel@test.com")
                .password("Student@123")
                .build();
        MvcResult stuLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stuLogin)))
                .andExpect(status().isOk())
                .andReturn();
        studentToken = extractDataField(stuLoginRes, "token");

        // 5. Create Course with Plans by Admin
        CreateCourseRequest.CoursePlanRequest p1 = CreateCourseRequest.CoursePlanRequest.builder()
                .duration(PlanDuration.THREE_MONTHS)
                .price(new BigDecimal("25000.00"))
                .build();
        CreateCourseRequest courseReq = CreateCourseRequest.builder()
                .title("Full Stack Java Masterclass")
                .description("Complete Java Spring Boot and React development curriculum.")
                .category(CourseCategory.JAVA_DEVELOPMENT)
                .facultyId(facultyId)
                .plans(List.of(p1))
                .build();

        MvcResult courseRes = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Map courseData = extractDataMap(courseRes);
        courseId = (String) courseData.get("courseCode");
        List<Map> plans = (List<Map>) courseData.get("plans");
        planId = String.valueOf(plans.get(0).get("id"));
    }

    @Test
    @DisplayName("Dummy Payment Creation — Validates DB source of truth for price and PENDING status")
    void testCreateDummyPayment() throws Exception {
        CreateDummyPaymentDTO dto = CreateDummyPaymentDTO.builder()
                .studentId(studentProfileId)
                .courseId(courseId)
                .planId(planId)
                .build();

        mockMvc.perform(post("/api/v1/payments/dummy/create")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.amount", is(25000.00)))
                .andExpect(jsonPath("$.data.transactionId", startsWith("DUMMY_TXN_")))
                .andExpect(jsonPath("$.data.paymentId", notNullValue()));
    }

    @Test
    @DisplayName("Dummy Payment Simulation — Complete with SUCCESS, FAILED, and CANCELLED")
    void testCompleteDummyPaymentSimulation() throws Exception {
        // 1. Create Payment
        CreateDummyPaymentDTO createDto = CreateDummyPaymentDTO.builder()
                .studentId(studentProfileId)
                .courseId(courseId)
                .planId(planId)
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/payments/dummy/create")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String paymentId = String.valueOf(extractDataMap(createRes).get("paymentId"));

        // 2. Complete with SUCCESS
        CompleteDummyPaymentDTO completeDto = CompleteDummyPaymentDTO.builder()
                .paymentId(paymentId)
                .result("SUCCESS")
                .build();

        mockMvc.perform(post("/api/v1/payments/dummy/complete")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.providerPaymentId", startsWith("DUMMY_PAY_")));

        // 3. Duplicate completion attempt must be rejected
        mockMvc.perform(post("/api/v1/payments/dummy/complete")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Production Student Onboarding — Existing Lead with Dummy Payment SUCCESS")
    void testOnboardExistingLeadWithPaymentSuccess() throws Exception {
        // 1. Create & Complete Dummy Payment
        CreateDummyPaymentDTO createDto = CreateDummyPaymentDTO.builder()
                .studentId(studentProfileId)
                .courseId(courseId)
                .planId(planId)
                .build();
        MvcResult createRes = mockMvc.perform(post("/api/v1/payments/dummy/create")
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();
        String paymentId = String.valueOf(extractDataMap(createRes).get("paymentId"));

        CompleteDummyPaymentDTO completeDto = CompleteDummyPaymentDTO.builder()
                .paymentId(paymentId)
                .result("SUCCESS")
                .build();
        mockMvc.perform(post("/api/v1/payments/dummy/complete")
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeDto)))
                .andExpect(status().isOk());

        // 2. Onboard Student by Executor
        StudentOnboardingDTO onboardingDto = StudentOnboardingDTO.builder()
                .leadId(leadId)
                .courseId(courseId)
                .planId(planId)
                .facultyId(facultyId)
                .paymentId(paymentId)
                .syllabusExplained(true)
                .scheduleExplained(true)
                .validityExplained(true)
                .notes("Onboarded after successful dummy payment simulation")
                .build();

        mockMvc.perform(post("/api/v1/onboarding/students")
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onboardingDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.enrollmentStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.data.paymentStatus", is("SUCCESS")))
                .andExpect(jsonPath("$.data.enrollmentId", startsWith("ENR-")))
                .andExpect(jsonPath("$.data.onboardingCode", startsWith("ONB-")))
                .andExpect(jsonPath("$.data.expiryDate", notNullValue()));

        // 3. Verify lead is marked ENROLLED
        mockMvc.perform(get("/api/v1/leads?search=Aarav")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status", is("enrolled")));
    }

    @Test
    @DisplayName("Production Student Onboarding — Brand New Student Creation Atomically")
    void testOnboardBrandNewStudent() throws Exception {
        // Direct admin enrollment / onboarding
        StudentOnboardingDTO onboardingDto = StudentOnboardingDTO.builder()
                .fullName("Kavita Sharma")
                .email("kavita.sharma@example.com")
                .phone("9844455566")
                .education("MCA")
                .city("Mumbai")
                .courseId(courseId)
                .planId(planId)
                .facultyId(facultyId)
                .directEnrollment(true)
                .syllabusExplained(true)
                .scheduleExplained(true)
                .validityExplained(true)
                .notes("Admin direct scholarship onboarding")
                .build();

        mockMvc.perform(post("/api/v1/onboarding/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onboardingDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Kavita Sharma")))
                .andExpect(jsonPath("$.data.email", is("kavita.sharma@example.com")))
                .andExpect(jsonPath("$.data.enrollmentStatus", is("ACTIVE")));
    }

    @Test
    @DisplayName("Duplicate Active Enrollment Prevention — Rejects second onboarding for active course")
    void testPreventDuplicateActiveEnrollment() throws Exception {
        StudentOnboardingDTO dto = StudentOnboardingDTO.builder()
                .leadId(leadId)
                .courseId(courseId)
                .planId(planId)
                .directEnrollment(true)
                .syllabusExplained(true)
                .scheduleExplained(true)
                .validityExplained(true)
                .build();

        // First onboarding succeeds
        mockMvc.perform(post("/api/v1/onboarding/students")
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Second onboarding for identical active course must return 409 Conflict
        mockMvc.perform(post("/api/v1/onboarding/students")
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already actively enrolled")));
    }

    @Test
    @DisplayName("Security Authorization — Only ADMIN and EXECUTOR allowed; STUDENT and FACULTY forbidden")
    void testOnboardingSecurityRoleEnforcement() throws Exception {
        StudentOnboardingDTO dto = StudentOnboardingDTO.builder()
                .leadId(leadId)
                .courseId(courseId)
                .planId(planId)
                .directEnrollment(true)
                .syllabusExplained(true)
                .scheduleExplained(true)
                .validityExplained(true)
                .build();

        // Student attempt -> 403 Forbidden
        mockMvc.perform(post("/api/v1/onboarding/students")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        // Faculty attempt -> 403 Forbidden
        mockMvc.perform(post("/api/v1/onboarding/students")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        // Executor attempt -> 201 Created
        mockMvc.perform(post("/api/v1/onboarding/students")
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Lecture Access Control — Enrolled student has access, non-enrolled gets 403")
    void testLectureAccessAuthorizationFlow() throws Exception {
        // 1. Create a Lecture for the course
        CreateLectureDTO lecDto = CreateLectureDTO.builder()
                .courseId(courseId)
                .facultyId(facultyId)
                .title("Advanced Multithreading & Virtual Threads")
                .description("Java 21 concurrency.")
                .lectureDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .lectureUrl("https://meet.google.com/java-adv")
                .recordingUrl("https://cdn.eduflow.com/rec/java.mp4")
                .isDownloadable(true)
                .build();

        MvcResult lecRes = mockMvc.perform(post("/api/v1/lectures")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lecDto)))
                .andExpect(status().isCreated())
                .andReturn();
        String lectureId = (String) extractDataMap(lecRes).get("lectureId");

        // 2. Before onboarding: Student lecture access is 403 Forbidden
        mockMvc.perform(get("/api/v1/lectures/" + lectureId + "/access")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.hasAccess", is(false)));

        // 3. Onboard student
        StudentOnboardingDTO onbDto = StudentOnboardingDTO.builder()
                .leadId(leadId)
                .courseId(courseId)
                .planId(planId)
                .directEnrollment(true)
                .syllabusExplained(true)
                .scheduleExplained(true)
                .validityExplained(true)
                .build();
        mockMvc.perform(post("/api/v1/onboarding/students")
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onbDto)))
                .andExpect(status().isCreated());

        // 4. After onboarding: Student lecture access is 200 OK & hasAccess: true
        mockMvc.perform(get("/api/v1/lectures/" + lectureId + "/access")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasAccess", is(true)))
                .andExpect(jsonPath("$.data.lectureUrl", is("https://meet.google.com/java-adv")));
    }

    @Test
    @DisplayName("Test Mobile Number Validation: Accept only 10-digits starting with 6-9, reject invalid numbers")
    void testMobileNumberValidation() throws Exception {
        // 1. Invalid: Starting with 5 (not in 6-9)
        RegisterStudentDTO invalidPrefix = RegisterStudentDTO.builder()
                .fullName("Test User")
                .email("test.prefix@example.com")
                .phone("5876543210")
                .password("Password@123")
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPrefix)))
                .andExpect(status().isBadRequest());

        // 2. Invalid: Less than 10 digits
        RegisterStudentDTO shortNumber = RegisterStudentDTO.builder()
                .fullName("Test User")
                .email("test.short@example.com")
                .phone("9876543")
                .password("Password@123")
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortNumber)))
                .andExpect(status().isBadRequest());

        // 3. Valid: 10 digits starting with 7
        RegisterStudentDTO validNum = RegisterStudentDTO.builder()
                .fullName("Test User Valid")
                .email("test.valid7@example.com")
                .phone("7890123456")
                .password("Password@123")
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validNum)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.phone", is("7890123456")));

        // 4. Valid: 10 digits starting with 6
        RegisterStudentDTO validNum6 = RegisterStudentDTO.builder()
                .fullName("Test User Valid 6")
                .email("test.valid6@example.com")
                .phone("6890123456")
                .password("Password@123")
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validNum6)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.phone", is("6890123456")));
    }

    private String extractDataField(MvcResult result, String field) throws Exception {
        Map map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map data = (Map) map.get("data");
        return String.valueOf(data.get(field));
    }

    private Map extractDataMap(MvcResult result) throws Exception {
        Map map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (Map) map.get("data");
    }
}
