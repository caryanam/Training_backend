package com.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.dto.request.*;
import com.training.enums.CourseCategory;
import com.training.enums.PlanDuration;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EduFlowApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String adminToken;
    private static String studentToken;
    private static String executorToken;
    private static String facultyToken;
    private static String createdFacultyId;
    private static String createdExecutorId;
    private static String createdLeadId;
    private static String createdCourseId;
    private static String createdPlanId;
    private static String createdLectureId;

    @Test
    @Order(1)
    void test1_AdminLogin() throws Exception {
        LoginRequestDTO loginReq = LoginRequestDTO.builder()
                .email("admin@gmail.com")
                .password("Admin@123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.role", is("ADMIN")))
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map map = objectMapper.readValue(responseStr, Map.class);
        Map data = (Map) map.get("data");
        adminToken = (String) data.get("token");
    }

    @Test
    @Order(2)
    void test2_CreateFacultyByAdmin() throws Exception {
        CreateFacultyDTO dto = CreateFacultyDTO.builder()
                .fullName("Dr. Rajesh Kumar")
                .email("rajesh.kumar@eduflow.com")
                .phone("+91 98111 22334")
                .department("Engineering")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/admin/faculty")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("FACULTY")))
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map map = objectMapper.readValue(responseStr, Map.class);
        Map data = (Map) map.get("data");
        createdFacultyId = (String) data.get("facultyId");
    }

    @Test
    @Order(3)
    void test3_CreateExecutorByAdmin() throws Exception {
        CreateExecutorDTO dto = CreateExecutorDTO.builder()
                .fullName("Priya Sen")
                .email("priya.executor@eduflow.com")
                .phone("+91 98222 33445")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/admin/executors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("EXECUTOR")))
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map map = objectMapper.readValue(responseStr, Map.class);
        Map data = (Map) map.get("data");
        createdExecutorId = (String) data.get("executorId");
    }

    @Test
    @Order(4)
    void test4_StudentRegistration() throws Exception {
        RegisterStudentDTO dto = RegisterStudentDTO.builder()
                .fullName("Rahul Sharma")
                .email("rahul.sharma@example.com")
                .phone("+91 98765 43210")
                .password("Password@123")
                .interestedCourse("Java Full Stack Development")
                .education("B.Tech Computer Science")
                .city("Bangalore")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.leadStatus", is("NEW")))
                .andExpect(jsonPath("$.data.role", is("STUDENT")))
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map map = objectMapper.readValue(responseStr, Map.class);
        Map data = (Map) map.get("data");
        createdLeadId = (String) data.get("leadId");
    }

    @Test
    @Order(5)
    void test5_StudentLogin() throws Exception {
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .email("rahul.sharma@example.com")
                .password("Password@123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.role", is("STUDENT")))
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map map = objectMapper.readValue(responseStr, Map.class);
        Map data = (Map) map.get("data");
        studentToken = (String) data.get("token");
    }

    @Test
    @Order(6)
    void test6_GetLeadsByAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/leads?status=NEW&search=Rahul")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(7)
    void test7_AssignExecutorToLead() throws Exception {
        AssignExecutorDTO dto = AssignExecutorDTO.builder()
                .executorId(createdExecutorId)
                .build();

        mockMvc.perform(put("/api/v1/leads/" + createdLeadId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", equalToIgnoringCase("ASSIGNED")));
    }

    @Test
    @Order(8)
    void test8_CreateCourseByAdmin() throws Exception {
        CreateCourseRequest.CoursePlanRequest planReq = CreateCourseRequest.CoursePlanRequest.builder()
                .duration(PlanDuration.THREE_MONTHS)
                .price(new BigDecimal("14999.00"))
                .build();

        CreateCourseRequest dto = CreateCourseRequest.builder()
                .title("Cloud Native DevOps & Kubernetes Masterclass")
                .description("Production CI/CD pipelines, Terraform IaC, and AWS Kubernetes clusters.")
                .category(CourseCategory.DEVOPS_CLOUD)
                .facultyId(createdFacultyId)
                .plans(List.of(planReq))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map map = objectMapper.readValue(responseStr, Map.class);
        Map data = (Map) map.get("data");
        createdCourseId = (String) data.get("courseCode");
        List<Map> plans = (List<Map>) data.get("plans");
        createdPlanId = String.valueOf(plans.get(0).get("id"));
    }

    @Test
    @Order(9)
    void test9_GetCourseDetailsByAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/courses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(10)
    void test10_VerifyPaymentByStudent() throws Exception {
        VerifyPaymentDTO dto = VerifyPaymentDTO.builder()
                .courseId(createdCourseId)
                .planId(createdPlanId)
                .amount(new BigDecimal("14999.00"))
                .paymentMethod("UPI (Razorpay)")
                .providerOrderId("ORD_99881")
                .providerPaymentId("PAY_77221")
                .providerSignature("valid_signature_hash")
                .build();

        mockMvc.perform(post("/api/v1/payments/verify")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.paymentStatus", is("SUCCESS")))
                .andExpect(jsonPath("$.data.enrollment.status", is("ACTIVE")));
    }

    @Test
    @Order(11)
    void test11_CreateLectureByAdmin() throws Exception {
        CreateLectureDTO dto = CreateLectureDTO.builder()
                .courseId(createdCourseId)
                .facultyId(createdFacultyId)
                .title("Spring Boot 3 & Dependency Injection Deep Dive")
                .description("Inversion of Control, Beans lifecycle, and annotations.")
                .lectureDate(LocalDate.now())
                .startTime(LocalTime.of(18, 0, 0))
                .endTime(LocalTime.of(19, 30, 0))
                .lectureUrl("https://meet.google.com/abc-spring-boot")
                .recordingUrl("https://cdn.eduflow.internal/recordings/spring-boot.mp4")
                .isDownloadable(true)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/lectures")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", containsString("Spring Boot 3")))
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map map = objectMapper.readValue(responseStr, Map.class);
        Map data = (Map) map.get("data");
        createdLectureId = (String) data.get("lectureId");
    }

    @Test
    @Order(12)
    void test12_GetLectureAccessByStudent() throws Exception {
        mockMvc.perform(get("/api/v1/lectures/" + createdLectureId + "/access")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.hasAccess", is(true)))
                .andExpect(jsonPath("$.data.lectureUrl", notNullValue()));
    }
}
