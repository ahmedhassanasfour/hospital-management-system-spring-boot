package com.ahmed.hospital.medicine.service;

import com.ahmed.hospital.medicine.dto.MedicineRequest;
import com.ahmed.hospital.medicine.dto.MedicineResponse;
import com.ahmed.hospital.medicine.entity.Medicine;
import com.ahmed.hospital.medicine.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicineService {

    private final MedicineRepository medicineRepository;

    @CacheEvict(value = "medicines", allEntries = true)
    public MedicineResponse create(MedicineRequest request) {

        if (medicineRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Medicine already exists");
        }

        Medicine medicine = Medicine.builder()
                .name(request.name())
                .description(request.description())
                .active(true)
                .build();

        return toResponse(medicineRepository.save(medicine));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "medicines", key = "#id")
    public MedicineResponse getById(Long id) {

        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Medicine not found"));

        return toResponse(medicine);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "medicines", key = "'all'")
    public List<MedicineResponse> getAll() {

        return medicineRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(value = "medicines", allEntries = true)
    public MedicineResponse update(Long id, MedicineRequest request) {

        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Medicine not found"));

        if (!medicine.getName().equalsIgnoreCase(request.name())
                && medicineRepository.existsByNameIgnoreCase(request.name())) {

            throw new IllegalArgumentException("Medicine already exists");
        }

        medicine.setName(request.name());
        medicine.setDescription(request.description());

        return toResponse(medicineRepository.save(medicine));
    }

    @CacheEvict(value = "medicines", allEntries = true)
    public void deactivate(Long id) {

        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Medicine not found"));

        medicine.setActive(false);
    }

    private MedicineResponse toResponse(Medicine medicine) {

        return new MedicineResponse(
                medicine.getId(),
                medicine.getName(),
                medicine.getDescription(),
                medicine.isActive(),
                medicine.getCreatedAt()
        );
    }
}