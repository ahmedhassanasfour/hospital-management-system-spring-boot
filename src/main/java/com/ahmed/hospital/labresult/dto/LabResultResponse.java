package com.ahmed.hospital.labresult.dto;

import java.time.LocalDateTime;

public record LabResultResponse(

        Long id,

        Long labOrderId,

        Long patientId,

        Long doctorId,

        Long labTestId,

        String labTestName,

        String resultValue,

        String referenceRange,

        String notes,

        LocalDateTime resultDate

) {
}