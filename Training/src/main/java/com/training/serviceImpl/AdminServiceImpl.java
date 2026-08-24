package com.training.serviceImpl;

import com.training.dto.request.CreateExecutorDTO;
import com.training.dto.request.CreateFacultyDTO;
import com.training.dto.responce.AdminDashboardStatsDTO;
import com.training.dto.responce.ExecutorResponseDTO;
import com.training.dto.responce.FacultyResponseDTO;
import com.training.entity.Executer;
import com.training.entity.Faculty;
import com.training.entity.User;
import com.training.enums.Role;
import com.training.exception.BadRequestException;
import com.training.exception.ResourceAlreadyExistsException;
import com.training.exception.ResourceNotFoundException;
import com.training.repo.*;
import com.training.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final ExecuterRepository executerRepository;
    private final StudentRepository studentRepository;
    private final StudentLeadRepository studentLeadRepository;
    private final PasswordEncoder passwordEncoder;

    private String validateAndGetPhone(String phone, String phoneNumber) {
        String finalPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : phoneNumber;
        if (finalPhone == null || finalPhone.trim().isEmpty()) {
            throw new BadRequestException("Phone number is required");
        }
        finalPhone = finalPhone.trim();
        String cleanPhone = finalPhone.replace(" ", "").replace("-", "");
        if (cleanPhone.startsWith("+91")) {
            cleanPhone = cleanPhone.substring(3);
        } else if (cleanPhone.startsWith("91") && cleanPhone.length() == 12) {
            cleanPhone = cleanPhone.substring(2);
        }
        
        if (!cleanPhone.matches("^[6-9]\\d{9}$")) {
            throw new BadRequestException("Phone number must contain a valid Indian 10-digit mobile number");
        }
        return cleanPhone;
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public FacultyResponseDTO createFaculty(CreateFacultyDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists: " + dto.getEmail());
        }

        String phoneVal = validateAndGetPhone(dto.getPhone(), dto.getPhoneNumber());
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) 
                ? dto.getPassword().trim() : "ChangeMe@123";

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(phoneVal)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.FACULTY)
                .status("ACTIVE")
                .build();
        user = userRepository.save(user);

        String facultyCode = "FAC-" + (2000 + user.getId());
        Faculty faculty = Faculty.builder()
                .user(user)
                .facultyCode(facultyCode)
                .status("ACTIVE")
                .department(dto.getDepartment())
                .build();
        faculty = facultyRepository.save(faculty);

        return FacultyResponseDTO.builder()
                .profileId("fac-prof-" + user.getId())
                .facultyId(facultyCode)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role("FACULTY")
                .status(faculty.getStatus())
                .department(faculty.getDepartment())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ExecutorResponseDTO createExecutor(CreateExecutorDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists: " + dto.getEmail());
        }

        String phoneVal = validateAndGetPhone(dto.getPhone(), dto.getPhoneNumber());
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) 
                ? dto.getPassword().trim() : "ChangeMe@123";

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(phoneVal)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.EXECUTOR)
                .status("ACTIVE")
                .build();
        user = userRepository.save(user);

        String executorCode = "EXE-" + (3000 + user.getId());
        Executer executor = Executer.builder()
                .user(user)
                .executorCode(executorCode)
                .status("ACTIVE")
                .build();
        executor = executerRepository.save(executor);

        return ExecutorResponseDTO.builder()
                .profileId("exe-prof-" + user.getId())
                .executorId(executorCode)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role("EXECUTOR")
                .status(executor.getStatus())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ExecutorResponseDTO updateExecutor(String executorIdStr, CreateExecutorDTO dto) {
        Executer executor = findExecutorByIdOrCode(executorIdStr);
        User user = executor.getUser();

        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            user.setFullName(dto.getFullName().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty() && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail().trim())) {
                throw new ResourceAlreadyExistsException("Email already in use: " + dto.getEmail());
            }
            user.setEmail(dto.getEmail().trim());
        }
        if (dto.getPhone() != null || dto.getPhoneNumber() != null) {
            String phoneVal = validateAndGetPhone(dto.getPhone(), dto.getPhoneNumber());
            user.setPhone(phoneVal);
        }
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
        }

        userRepository.save(user);

        return ExecutorResponseDTO.builder()
                .profileId("exe-prof-" + user.getId())
                .executorId(executor.getExecutorCode() != null ? executor.getExecutorCode() : "EXE-" + executor.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role("EXECUTOR")
                .status(executor.getStatus())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ExecutorResponseDTO updateExecutorStatus(String executorIdStr, String status) {
        Executer executor = findExecutorByIdOrCode(executorIdStr);
        String formattedStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ACTIVE";
        executor.setStatus(formattedStatus);
        if (executor.getUser() != null) {
            executor.getUser().setStatus(formattedStatus);
            userRepository.save(executor.getUser());
        }
        executerRepository.save(executor);

        User user = executor.getUser();
        return ExecutorResponseDTO.builder()
                .profileId("exe-prof-" + (user != null ? user.getId() : executor.getId()))
                .executorId(executor.getExecutorCode() != null ? executor.getExecutorCode() : "EXE-" + executor.getId())
                .fullName(user != null ? user.getFullName() : "Executor")
                .email(user != null ? user.getEmail() : "")
                .phone(user != null ? user.getPhone() : "")
                .role("EXECUTOR")
                .status(executor.getStatus())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void deleteExecutor(String executorIdStr) {
        Executer executor = findExecutorByIdOrCode(executorIdStr);
        User user = executor.getUser();
        executerRepository.delete(executor);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    private Executer findExecutorByIdOrCode(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new BadRequestException("Executor ID is required");
        }
        String cleanKey = key.trim();
        Optional<Executer> byCode = executerRepository.findByExecutorCode(cleanKey);
        if (byCode.isPresent()) return byCode.get();

        Optional<Executer> byEmail = executerRepository.findByUserEmail(cleanKey);
        if (byEmail.isPresent()) return byEmail.get();

        try {
            Long id = Long.parseLong(cleanKey.replace("EXE-", "").replace("exe-prof-", ""));
            return executerRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Executor not found with ID: " + key));
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Executor not found with ID: " + key);
        }
    }

    @Override
    public List<FacultyResponseDTO> getAllFaculty() {
        return facultyRepository.findAll().stream().map(f -> FacultyResponseDTO.builder()
                .profileId("fac-prof-" + (f.getUser() != null ? f.getUser().getId() : f.getId()))
                .facultyId(f.getFacultyCode() != null ? f.getFacultyCode() : "FAC-" + f.getId())
                .fullName(f.getUser() != null ? f.getUser().getFullName() : "Faculty")
                .email(f.getUser() != null ? f.getUser().getEmail() : "")
                .phone(f.getUser() != null ? f.getUser().getPhone() : "")
                .role("FACULTY")
                .status(f.getStatus())
                .department(f.getDepartment())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<ExecutorResponseDTO> getAllExecutors() {
        return executerRepository.findAll().stream().map(e -> ExecutorResponseDTO.builder()
                .profileId("exe-prof-" + (e.getUser() != null ? e.getUser().getId() : e.getId()))
                .executorId(e.getExecutorCode() != null ? e.getExecutorCode() : "EXE-" + e.getId())
                .fullName(e.getUser() != null ? e.getUser().getFullName() : "Executor")
                .email(e.getUser() != null ? e.getUser().getEmail() : "")
                .phone(e.getUser() != null ? e.getUser().getPhone() : "")
                .role("EXECUTOR")
                .status(e.getStatus())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "dashboardStats")
    public AdminDashboardStatsDTO getDashboardStats() {
        long totalStudents = studentRepository.count();
        long newLeads = studentLeadRepository.countByStatus(com.training.enums.LeadStatus.NEW);
        long totalExecutors = executerRepository.count();
        long totalFaculty = facultyRepository.count();

        return AdminDashboardStatsDTO.builder()
                .totalStudents(totalStudents)
                .newLeads(newLeads)
                .totalExecutors(totalExecutors)
                .totalFaculty(totalFaculty)
                .build();
    }
}
