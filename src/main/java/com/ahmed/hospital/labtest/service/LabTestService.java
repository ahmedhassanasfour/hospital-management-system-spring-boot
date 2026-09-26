package com.ahmed.hospital.labtest.service;

import com.ahmed.hospital.labtest.dto.LabTestRequest;
import com.ahmed.hospital.labtest.dto.LabTestResponse;
import com.ahmed.hospital.labtest.entity.LabTest;
import com.ahmed.hospital.labtest.repository.LabTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LabTestService {

    private final LabTestRepository labTestRepository;

    @CacheEvict(value = "labTests", allEntries = true)
    public LabTestResponse create(LabTestRequest request) {

        if (labTestRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException(
                    "Lab test already exists"
            );
        }

        LabTest labTest = LabTest.builder()
                .name(request.name())
                .description(request.description())
                .active(true)
                .build();

        return toResponse(
                labTestRepository.save(labTest)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "labTests", key = "#id")
    public LabTestResponse getById(Long id) {

        LabTest labTest = labTestRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lab test not found"
                        ));

        return toResponse(labTest);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "labTests", key = "'all'")
    public List<LabTestResponse> getAll() {

        return labTestRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(value = "labTests", allEntries = true)
    public LabTestResponse update(
            Long id,
            LabTestRequest request
    ) {

        LabTest labTest = labTestRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lab test not found"
                        ));

        if (!labTest.getName().equalsIgnoreCase(request.name())
                && labTestRepository.existsByNameIgnoreCase(
                request.name()
        )) {

            throw new IllegalArgumentException(
                    "Lab test already exists"
            );
        }

        labTest.setName(request.name());
        labTest.setDescription(request.description());

        return toResponse(
                labTestRepository.save(labTest)
        );
    }

    @CacheEvict(value = "labTests", allEntries = true)
    public void deactivate(Long id) {

        LabTest labTest = labTestRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lab test not found"
                        ));

        labTest.setActive(false);
    }

    private LabTestResponse toResponse(LabTest labTest) {

        return new LabTestResponse(
                labTest.getId(),
                labTest.getName(),
                labTest.getDescription(),
                labTest.isActive(),
                labTest.getCreatedAt()
        );
    }
}