package com.ahmed.hospital.doctor.service;

import com.ahmed.hospital.common.exception.BadRequestException;
import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import com.ahmed.hospital.doctor.dto.CreateDoctorRequest;
import com.ahmed.hospital.doctor.dto.DoctorResponse;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.doctor.repository.DoctorRepository;
import com.ahmed.hospital.user.entity.Role;
import com.ahmed.hospital.user.entity.User;
import com.ahmed.hospital.user.repository.UserRepository;
import com.ahmed.hospital.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public DoctorResponse createDoctor(
            CreateDoctorRequest request
    ) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException(
                    "Email already exists"
            );
        }

        if (doctorRepository.existsByLicenseNumber(
                request.licenseNumber()
        )) {
            throw new BadRequestException(
                    "License number already exists"
            );
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .role(Role.DOCTOR)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        Doctor doctor = Doctor.builder()
                .user(savedUser)
                .specialization(request.specialization())
                .licenseNumber(request.licenseNumber())
                .phone(request.phone())
                .bio(request.bio())
                .build();

        Doctor savedDoctor =
                doctorRepository.save(doctor);

        // Audit: doctor account created (no password logged)
        auditService.log(
                savedUser.getId(),
                "DOCTOR_CREATED",
                "Doctor",
                savedDoctor.getId(),
                "Doctor account created for " + savedUser.getEmail() + " (" + savedDoctor.getSpecialization() + ")",
                null
        );

        return new DoctorResponse(
                savedDoctor.getId(),
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedDoctor.getSpecialization(),
                savedDoctor.getLicenseNumber(),
                savedDoctor.getPhone(),
                savedDoctor.getBio()
        );
    }

    public DoctorResponse getDoctorById(Long id) {

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Doctor not found"
                        ));

        User user = doctor.getUser();

        return new DoctorResponse(
                doctor.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                doctor.getSpecialization(),
                doctor.getLicenseNumber(),
                doctor.getPhone(),
                doctor.getBio()
        );
    }

    public List<DoctorResponse> getAllDoctors() {

        return doctorRepository.findAll()
                .stream()
                .map(doctor -> {

                    User user = doctor.getUser();

                    return new DoctorResponse(
                            doctor.getId(),
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            doctor.getSpecialization(),
                            doctor.getLicenseNumber(),
                            doctor.getPhone(),
                            doctor.getBio()
                    );
                })
                .toList();
    }

}