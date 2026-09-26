package com.ahmed.hospital.file.service;

import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.doctor.repository.DoctorRepository;
import com.ahmed.hospital.file.dto.MedicalFileResponse;
import com.ahmed.hospital.file.entity.FileType;
import com.ahmed.hospital.file.entity.MedicalFile;
import com.ahmed.hospital.file.repository.MedicalFileRepository;
import com.ahmed.hospital.labresult.entity.LabResult;
import com.ahmed.hospital.labresult.repository.LabResultRepository;
import com.ahmed.hospital.patient.entity.Patient;
import com.ahmed.hospital.patient.repository.PatientRepository;
import com.ahmed.hospital.prescription.entity.Prescription;
import com.ahmed.hospital.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalFileService {

    private final MedicalFileRepository medicalFileRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LabResultRepository labResultRepository;
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;

    public MedicalFileResponse uploadMedicalDocument(
            MultipartFile file,
            String authenticatedEmail
    ) {

        Patient patient = patientRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        ));

        return storeFile(
                file,
                FileType.MEDICAL_DOCUMENT,
                patient,
                null,
                null
        );
    }

    public MedicalFileResponse uploadPrescriptionAttachment(
            Long prescriptionId,
            MultipartFile file,
            String authenticatedEmail
    ) {

        Doctor doctor = doctorRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        ));

        Prescription prescription =
                prescriptionRepository.findById(prescriptionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Prescription not found"
                                ));

        if (!prescription.getDoctor().getId()
                .equals(doctor.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this prescription"
            );
        }

        return storeFile(
                file,
                FileType.PRESCRIPTION_ATTACHMENT,
                prescription.getPatient(),
                prescription,
                null
        );
    }

    public MedicalFileResponse uploadLabResultFile(
            Long labResultId,
            MultipartFile file,
            String authenticatedEmail
    ) {

        Doctor doctor = doctorRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        ));

        LabResult labResult =
                labResultRepository.findById(labResultId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Lab result not found"
                                ));

        if (!labResult.getLabOrder()
                .getDoctor()
                .getId()
                .equals(doctor.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this lab result"
            );
        }

        return storeFile(
                file,
                FileType.LAB_RESULT,
                labResult.getLabOrder().getPatient(),
                null,
                labResult
        );
    }

    @Transactional(readOnly = true)
    public List<MedicalFileResponse> getMyFiles(
            String authenticatedEmail
    ) {

        Patient patient = patientRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        ));

        return medicalFileRepository
                .findByPatientIdOrderByUploadedAtDesc(
                        patient.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Path downloadForPatient(
            Long fileId,
            String authenticatedEmail
    ) {

        Patient patient = patientRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        ));

        MedicalFile file =
                medicalFileRepository.findById(fileId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "File not found"
                                ));

        if (!file.getPatient().getId()
                .equals(patient.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this file"
            );
        }

        return fileStorageService.load(
                file.getStoredName()
        );
    }

    private MedicalFileResponse storeFile(
            MultipartFile file,
            FileType fileType,
            Patient patient,
            Prescription prescription,
            LabResult labResult
    ) {

        fileValidationService.validate(file);

        String storedName =
                fileStorageService.store(file);

        MedicalFile medicalFile = MedicalFile.builder()
                .originalName(file.getOriginalFilename())
                .storedName(storedName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .storagePath("uploads/" + storedName)
                .fileType(fileType)
                .patient(patient)
                .prescription(prescription)
                .labResult(labResult)
                .build();

        return toResponse(
                medicalFileRepository.save(medicalFile)
        );
    }

    private MedicalFileResponse toResponse(
            MedicalFile file
    ) {

        return new MedicalFileResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSize(),
                file.getFileType(),
                file.getPatient().getId(),
                file.getPrescription() != null
                        ? file.getPrescription().getId()
                        : null,
                file.getLabResult() != null
                        ? file.getLabResult().getId()
                        : null,
                file.getUploadedAt()
        );
    }
}