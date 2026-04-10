package org.vgu.labservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.labservice.dto.LabResultRequest;
import org.vgu.labservice.dto.LabResultResponse;
import org.vgu.labservice.exception.LabNotFoundException;
import org.vgu.labservice.model.LabOrder;
import org.vgu.labservice.model.LabOrderItem;
import org.vgu.labservice.model.LabResult;
import org.vgu.labservice.model.LabTestCatalog;
import org.vgu.labservice.repository.LabOrderItemRepository;
import org.vgu.labservice.repository.LabOrderRepository;
import org.vgu.labservice.repository.LabResultRepository;
import org.vgu.labservice.repository.LabTestCatalogRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LabResultService {

    private final LabResultRepository labResultRepository;
    private final LabOrderRepository labOrderRepository;
    private final LabOrderItemRepository labOrderItemRepository;
    private final LabTestCatalogRepository labTestCatalogRepository;

    public LabResultResponse getLabResultById(Long resultId) {
        LabResult result = labResultRepository.findById(resultId)
                .orElseThrow(() -> new LabNotFoundException("Lab result not found with ID: " + resultId));
        return mapToLabResultResponse(result);
    }

    public List<LabResultResponse> getLabResultsByOrderId(Long labOrderId) {
        return labResultRepository.findByLabOrderId(labOrderId).stream()
                .map(this::mapToLabResultResponse)
                .collect(Collectors.toList());
    }

    public List<LabResultResponse> getLabResultsByPatientId(Long patientId) {
        List<LabOrder> orders = labOrderRepository.findByPatientId(patientId);
        return orders.stream()
                .flatMap(order -> labResultRepository.findByLabOrderId(order.getLabOrderId()).stream())
                .map(this::mapToLabResultResponse)
                .collect(Collectors.toList());
    }

    public List<LabResultResponse> getAllLabResults() {
        return labResultRepository.findAll().stream()
                .map(this::mapToLabResultResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LabResultResponse createLabResult(LabResultRequest request) {
        // Verify lab order exists
        LabOrder order = labOrderRepository.findById(request.getLabOrderId())
                .orElseThrow(() -> new LabNotFoundException("Lab order not found with ID: " + request.getLabOrderId()));

        // Verify order item exists
        LabOrderItem orderItem = labOrderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new LabNotFoundException("Lab order item not found with ID: " + request.getOrderItemId()));

        // Verify test exists
        LabTestCatalog test = labTestCatalogRepository.findById(request.getTestId())
                .orElseThrow(() -> new LabNotFoundException("Test not found with ID: " + request.getTestId()));

        LabResult result = new LabResult();
        result.setLabOrderId(request.getLabOrderId());
        result.setOrderItemId(request.getOrderItemId());
        result.setTestId(request.getTestId());
        result.setResultValue(request.getResultValue());
        result.setResultUnit(request.getResultUnit());
        result.setReferenceRange(request.getReferenceRange());
        result.setResultStatus(request.getResultStatus() != null ? request.getResultStatus() : "PENDING");
        result.setInterpretation(request.getInterpretation());
        result.setFlags(request.getFlags());
        result.setSpecimenAdequacy(request.getSpecimenAdequacy());
        result.setComments(request.getComments());
        result.setPerformedByLabTechId(request.getPerformedByLabTechId());
        result.setSensitivityLevel(request.getSensitivityLevel() != null ? request.getSensitivityLevel() : "NORMAL");

        LabResult savedResult = labResultRepository.save(result);

        // Update order item status to COMPLETED
        orderItem.setStatus("COMPLETED");
        labOrderItemRepository.save(orderItem);

        // Check if all items are completed, then update order status
        boolean allCompleted = order.getOrderItems().stream()
                .allMatch(item -> "COMPLETED".equals(item.getStatus()));
        if (allCompleted) {
            order.setStatus("COMPLETED");
            order.setCompletedAt(java.time.LocalDateTime.now());
            labOrderRepository.save(order);
        }

        return mapToLabResultResponse(savedResult);
    }

    @Transactional
    public LabResultResponse updateLabResult(Long resultId, LabResultRequest request) {
        LabResult result = labResultRepository.findById(resultId)
                .orElseThrow(() -> new LabNotFoundException("Lab result not found with ID: " + resultId));

        if (request.getResultValue() != null) result.setResultValue(request.getResultValue());
        if (request.getResultUnit() != null) result.setResultUnit(request.getResultUnit());
        if (request.getReferenceRange() != null) result.setReferenceRange(request.getReferenceRange());
        if (request.getResultStatus() != null) result.setResultStatus(request.getResultStatus());
        if (request.getInterpretation() != null) result.setInterpretation(request.getInterpretation());
        if (request.getFlags() != null) result.setFlags(request.getFlags());
        if (request.getSpecimenAdequacy() != null) result.setSpecimenAdequacy(request.getSpecimenAdequacy());
        if (request.getComments() != null) result.setComments(request.getComments());

        LabResult updatedResult = labResultRepository.save(result);
        return mapToLabResultResponse(updatedResult);
    }

    private LabResultResponse mapToLabResultResponse(LabResult result) {
        LabTestCatalog test = labTestCatalogRepository.findById(result.getTestId()).orElse(null);
        String testName = test != null ? test.getTestName() : null;

        return LabResultResponse.builder()
                .resultId(result.getResultId())
                .labOrderId(result.getLabOrderId())
                .orderItemId(result.getOrderItemId())
                .testId(result.getTestId())
                .testName(testName)
                .resultValue(result.getResultValue())
                .resultUnit(result.getResultUnit())
                .referenceRange(result.getReferenceRange())
                .resultStatus(result.getResultStatus())
                .interpretation(result.getInterpretation())
                .flags(result.getFlags())
                .specimenAdequacy(result.getSpecimenAdequacy())
                .comments(result.getComments())
                .performedByLabTechId(result.getPerformedByLabTechId())
                .verifiedByLabTechId(result.getVerifiedByLabTechId())
                .approvedByPathologistId(result.getApprovedByPathologistId())
                .sensitivityLevel(result.getSensitivityLevel())
                .resultDate(result.getResultDate())
                .verifiedAt(result.getVerifiedAt())
                .approvedAt(result.getApprovedAt())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}
