package com.training.serviceImpl;

import com.training.config.JwtService;
import com.training.dto.request.LoginRequestDTO;
import com.training.dto.request.RegisterStudentDTO;
import com.training.dto.request.ResendOtpDTO;
import com.training.dto.request.VerifyOtpDTO;
import com.training.dto.responce.LoginResponseDTO;
import com.training.dto.responce.RegisterStudentResponseDTO;
import com.training.entity.OtpVerification;
import com.training.entity.Student;
import com.training.entity.StudentLead;
import com.training.entity.User;
import com.training.enums.LeadStatus;
import com.training.enums.OtpPurpose;
import com.training.enums.Role;
import com.training.exception.BadRequestException;
import com.training.exception.InvalidCredentialsException;
import com.training.exception.ResourceAlreadyExistsException;
import com.training.exception.UnauthorizedException;
import com.training.repo.OtpVerificationRepository;
import com.training.repo.StudentLeadRepository;
import com.training.repo.StudentRepository;
import com.training.repo.UserRepository;
import com.training.service.AuthService;
import com.training.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentLeadRepository studentLeadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public String registerStudent(RegisterStudentDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists: " + dto.getEmail());
        }

        // Validate Full Name length (min 2 chars)
        if (dto.getFullName() == null || dto.getFullName().trim().length() < 2) {
            throw new BadRequestException("Full name must be at least 2 characters");
        }

        // Validate Password length (min 8 chars)
        if (dto.getPassword() == null || dto.getPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }

        // Validate phone number
        String phoneVal = dto.getPhone();
        if (phoneVal == null || phoneVal.trim().isEmpty()) {
            throw new BadRequestException("Phone number is required");
        }
        phoneVal = phoneVal.trim();
        String cleanPhone = phoneVal.replace(" ", "").replace("-", "");
        if (cleanPhone.startsWith("+91")) {
            cleanPhone = cleanPhone.substring(3);
        } else if (cleanPhone.startsWith("91") && cleanPhone.length() == 12) {
            cleanPhone = cleanPhone.substring(2);
        }
        if (!cleanPhone.matches("^[6-9]\\d{9}$")) {
            throw new BadRequestException("Mobile number must be a valid 10-digit number starting with 6, 7, 8, or 9");
        }
        if (userRepository.existsByPhone(cleanPhone)) {
            throw new ResourceAlreadyExistsException("Mobile number already exists: " + cleanPhone);
        }

        String courseVal = (dto.getInterestedCourse() != null && !dto.getInterestedCourse().trim().isEmpty())
                ? dto.getInterestedCourse().trim() : null;
        String eduVal = (dto.getEducation() != null && !dto.getEducation().trim().isEmpty())
                ? dto.getEducation().trim() : null;
        String cityVal = (dto.getCity() != null && !dto.getCity().trim().isEmpty())
                ? dto.getCity().trim() : null;

        // 1. Create User (UNVERIFIED)
        User user = User.builder()
                .fullName(dto.getFullName().trim())
                .email(dto.getEmail().trim())
                .phone(cleanPhone)
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.STUDENT)
                .status("UNVERIFIED")
                .build();
        user = userRepository.save(user);

        // 2. Create Student Profile
        String studentCode = "STU-" + (1000 + user.getId());
        Student student = Student.builder()
                .user(user)
                .studentCode(studentCode)
                .interestedCourse(courseVal)
                .education(eduVal)
                .city(cityVal)
                .build();
        studentRepository.save(student);

        // 3. Create Student Lead
        String leadCode = "lead-" + (9900 + user.getId());
        StudentLead lead = StudentLead.builder()
                .leadCode(leadCode)
                .student(student)
                .fullName(dto.getFullName().trim())
                .email(dto.getEmail().trim())
                .phone(cleanPhone)
                .interestedCourse(courseVal)
                .education(eduVal)
                .city(cityVal)
                .status(LeadStatus.NEW)
                .build();
        studentLeadRepository.save(lead);

        generateAndSendOtp(user.getEmail(), user.getFullName());

        return "OTP sent successfully. Please check your email.";
    }

    private void generateAndSendOtp(String email, String fullName) {
        String rawOtp = String.format("%06d", new Random().nextInt(999999));
        String hashedOtp = passwordEncoder.encode(rawOtp);

        OtpVerification otpEntity = otpVerificationRepository.findByIdentifierAndPurpose(email, OtpPurpose.REGISTRATION)
                .orElse(OtpVerification.builder()
                        .identifier(email)
                        .purpose(OtpPurpose.REGISTRATION)
                        .build());

        otpEntity.setOtpHash(hashedOtp);
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpEntity.setAttempts(0);
        otpEntity.setVerified(false);
        otpEntity.setLastSentAt(LocalDateTime.now());
        otpVerificationRepository.save(otpEntity);

        emailService.sendOtpEmail(email, rawOtp, fullName);
    }

    @Override
    @Transactional
    public String verifyRegistrationOtp(VerifyOtpDTO dto) {
        OtpVerification otpEntity = otpVerificationRepository.findByIdentifierAndPurpose(dto.getEmail(), OtpPurpose.REGISTRATION)
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email"));

        if (otpEntity.isVerified()) {
            throw new BadRequestException("OTP already verified");
        }

        if (otpEntity.getAttempts() >= 5) {
            throw new BadRequestException("Maximum OTP verification attempts reached. Please resend OTP.");
        }

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please resend OTP.");
        }

        if (!passwordEncoder.matches(dto.getOtp(), otpEntity.getOtpHash())) {
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            otpVerificationRepository.save(otpEntity);
            throw new BadRequestException("Invalid OTP");
        }

        otpEntity.setVerified(true);
        otpVerificationRepository.save(otpEntity);

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceAlreadyExistsException("User not found"));
        user.setStatus("ACTIVE");
        userRepository.save(user);

        return "Registration verified successfully. You can now login.";
    }

    @Override
    @Transactional
    public String resendRegistrationOtp(ResendOtpDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if ("ACTIVE".equals(user.getStatus())) {
            throw new BadRequestException("User is already verified");
        }

        OtpVerification otpEntity = otpVerificationRepository.findByIdentifierAndPurpose(dto.getEmail(), OtpPurpose.REGISTRATION)
                .orElse(null);

        if (otpEntity != null && otpEntity.getLastSentAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Please wait 60 seconds before resending OTP");
        }

        generateAndSendOtp(user.getEmail(), user.getFullName());
        return "OTP resent successfully";
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO dto) {
        String identifier = dto.getIdentifier() != null ? dto.getIdentifier().trim() : "";
        String identifierLower = identifier.toLowerCase();

        User user = userRepository.findByEmailOrPhone(identifierLower, identifier)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        boolean matches = passwordEncoder.matches(dto.getPassword(), user.getPassword());

        if (!matches) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("Account is not verified. Please verify your OTP.");
        }

        String roleName = user.getRole().name();
        String token = jwtService.generateToken(user.getEmail(), "ROLE_" + roleName);

        LoginResponseDTO.UserInfoDTO userInfo = LoginResponseDTO.UserInfoDTO.builder()
                .profileId(String.valueOf(user.getId()))
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(roleName)
                .build();

        return LoginResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }
}
