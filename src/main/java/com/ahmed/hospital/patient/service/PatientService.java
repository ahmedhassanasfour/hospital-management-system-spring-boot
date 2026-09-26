package com.ahmed.hospital.patient.service;

import com.ahmed.hospital.common.exception.BadRequestException;
import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import com.ahmed.hospital.patient.dto.CreatePatientRequest;
import com.ahmed.hospital.patient.dto.PatientResponse;
import com.ahmed.hospital.patient.entity.Patient;
import com.ahmed.hospital.patient.repository.PatientRepository;
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
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public PatientResponse createPatient(
            CreatePatientRequest request
    ) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException(
                    "Email already exists"
            );
        }

        if (patientRepository.existsByNationalId(
                request.nationalId()
        )) {
            throw new BadRequestException(
                    "National ID already exists"
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
                .role(Role.PATIENT)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        Patient patient = Patient.builder()
                .user(savedUser)
                .nationalId(request.nationalId())
                .phone(request.phone())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .address(request.address())
                .build();

        Patient savedPatient =
                patientRepository.save(patient);

        // Audit: patient account created (no password logged)
        auditService.log(
                savedUser.getId(),
                "PATIENT_CREATED",
                "Patient",
                savedPatient.getId(),
                "Patient account created for " + savedUser.getEmail(),
                null
        );

        return new PatientResponse(
                savedPatient.getId(),
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedPatient.getNationalId(),
                savedPatient.getPhone(),
                savedPatient.getDateOfBirth(),
                savedPatient.getGender(),
                savedPatient.getAddress()
        );
    }

    public PatientResponse getPatientById(Long id) {

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Patient not found"
                        ));

        User user = patient.getUser();

        return new PatientResponse(
                patient.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                patient.getNationalId(),
                patient.getPhone(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getAddress()
        );
    }

    public List<PatientResponse> getAllPatients() {

        return patientRepository.findAll()
                .stream()
                .map(patient -> {

                    User user = patient.getUser();

                    return new PatientResponse(
                            patient.getId(),
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            patient.getNationalId(),
                            patient.getPhone(),
                            patient.getDateOfBirth(),
                            patient.getGender(),
                            patient.getAddress()
                    );
                })
                .toList();
    }

}