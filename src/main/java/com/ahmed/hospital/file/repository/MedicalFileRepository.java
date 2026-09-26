package com.ahmed.hospital.file.repository;

import com.ahmed.hospital.file.entity.FileType;
import com.ahmed.hospital.file.entity.MedicalFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalFileRepository
        extends JpaRepository<MedicalFile, Long> {

    List<MedicalFile> findByPatientIdOrderByUploadedAtDesc(
            Long patientId
    );

    List<MedicalFile> findByPatientIdAndFileTypeOrderByUploadedAtDesc(
            Long patientId,
            FileType fileType
    );

    Optional<MedicalFile> findByStoredName(String storedName);

    List<MedicalFile> findByPrescriptionIdOrderByUploadedAtDesc(
            Long prescriptionId
    );

    List<MedicalFile> findByLabResultIdOrderByUploadedAtDesc(
            Long labResultId
    );
}