package org.vgu.labservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.labservice.dto.*;
import org.vgu.labservice.exception.LabNotFoundException;
import org.vgu.labservice.model.LabOrder;
import org.vgu.labservice.model.LabOrderItem;
import org.vgu.labservice.model.LabTestCatalog;
import org.vgu.labservice.repository.LabOrderItemRepository;
import org.vgu.labservice.repository.LabOrderRepository;
import org.vgu.labservice.repository.LabTestCatalogRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LabOrderService {

    private final LabOrderRepository labOrderRepository;
    private final LabOrderItemRepository labOrderItemRepository;
    private final LabTestCatalogRepository labTestCatalogRepository;

    public LabOrderResponse getLabOrderById(Long labOrderId) {
        LabOrder order = labOrderRepository.findById(labOrderId)
                .orElseThrow(() -> new LabNotFoundException("Lab order not found with ID: " + labOrderId));
        return mapToLabOrderResponse(order);
    }

    /** PIP: Resource attributes for ABAC. */
    public Optional<Map<String, Object>> getPipResourceAttributes(Long resourceId) {
        return labOrderRepository.findById(resourceId).map(order -> {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("owner_id", String.valueOf(order.getPatientId()));
            if (order.getCreatedByKeycloakId() != null && !order.getCreatedByKeycloakId().isBlank())
                attrs.put("created_by", order.getCreatedByKeycloakId());
            if (order.getDepartmentId() != null) attrs.put("department_id", String.valueOf(order.getDepartmentId()));
            if (order.getHospitalId() != null) attrs.put("hospital_id", order.getHospitalId());
            if (order.getStatus() != null) attrs.put("status", order.getStatus());
            if (order.getSensitivityLevel() != null) attrs.put("sensitivity_level", order.getSensitivityLevel());
            return attrs;
        });
    }

    public List<LabOrderResponse> getLabOrdersByPatient(Long patientId) {
        return labOrderRepository.findByPatientId(patientId).stream()
                .map(this::mapToLabOrderResponse)
                .collect(Collectors.toList());
    }

    public List<LabOrderResponse> getLabOrdersByDoctor(Long doctorId) {
        return labOrderRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToLabOrderResponse)
                .collect(Collectors.toList());
    }

    public List<LabOrderResponse> getLabOrdersByStatus(String status) {
        return labOrderRepository.findByStatus(status).stream()
                .map(this::mapToLabOrderResponse)
                .collect(Collectors.toList());
    }

    public List<LabOrderResponse> getLabOrdersByHospital(String hospitalId) {
        return labOrderRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToLabOrderResponse)
                .collect(Collectors.toList());
    }

    public List<LabOrderResponse> getAllLabOrders() {
        return labOrderRepository.findAll().stream()
                .map(this::mapToLabOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Flexible query supporting optional status, patientId, doctorId filters.
     * Used by GET /lab/orders?status=&patientId=&doctorId= from frontend.
     */
    public List<LabOrderResponse> getLabOrders(String status, Long patientId, Long doctorId) {
        List<LabOrder> results;
        if (patientId != null) {
            results = labOrderRepository.findByPatientId(patientId);
            if (status != null) results = results.stream().filter(o -> status.equals(o.getStatus())).collect(Collectors.toList());
        } else if (doctorId != null) {
            results = labOrderRepository.findByDoctorId(doctorId);
            if (status != null) results = results.stream().filter(o -> status.equals(o.getStatus())).collect(Collectors.toList());
        } else if (status != null) {
            results = labOrderRepository.findByStatus(status);
        } else {
            results = labOrderRepository.findAll();
        }
        return results.stream().map(this::mapToLabOrderResponse).collect(Collectors.toList());
    }

    @Transactional
    public LabOrderResponse createLabOrder(LabOrderRequest request, String creatorKeycloakUserId) {
        LabOrder order = new LabOrder();
        order.setPatientId(request.getPatientId());
        order.setDoctorId(request.getDoctorId());
        order.setAppointmentId(request.getAppointmentId());
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "LAB");
        order.setClinicalDiagnosis(request.getClinicalDiagnosis());
        order.setClinicalNotes(request.getClinicalNotes());
        order.setUrgency(request.getUrgency() != null ? request.getUrgency() : "ROUTINE");
        order.setStatus("PENDING");
        order.setDepartmentId(request.getDepartmentId());
        order.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : "HOSPITAL_A");
        order.setSensitivityLevel(request.getSensitivityLevel() != null ? request.getSensitivityLevel() : "NORMAL");
        order.setCreatedByKeycloakId(creatorKeycloakUserId);

        LabOrder savedOrder = labOrderRepository.save(order);

        // Create order items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (LabOrderItemRequest itemRequest : request.getItems()) {
                LabTestCatalog test = labTestCatalogRepository.findById(itemRequest.getTestId())
                        .orElseThrow(() -> new LabNotFoundException("Test not found with ID: " + itemRequest.getTestId()));

                LabOrderItem item = new LabOrderItem();
                item.setLabOrder(savedOrder);
                item.setTest(test);
                item.setStatus("PENDING");
                item.setPriority(itemRequest.getPriority() != null ? itemRequest.getPriority() : 1);
                item.setPrice(itemRequest.getPrice() != null ? itemRequest.getPrice() : test.getPrice());
                labOrderItemRepository.save(item);
            }
        }

        // Reload order with items
        LabOrder orderWithItems = labOrderRepository.findById(savedOrder.getLabOrderId())
                .orElseThrow(() -> new LabNotFoundException("Lab order not found after creation"));
        return mapToLabOrderResponse(orderWithItems);
    }

    @Transactional
    public LabOrderResponse updateLabOrderStatus(Long labOrderId, String status, String keycloakUserId) {
        LabOrder order = labOrderRepository.findById(labOrderId)
                .orElseThrow(() -> new LabNotFoundException("Lab order not found with ID: " + labOrderId));

        order.setStatus(status);

        if ("IN_PROGRESS".equals(status) && order.getStartedAt() == null) {
            order.setStartedAt(java.time.LocalDateTime.now());
            order.setProcessedByLabTechId(getLabTechIdFromKeycloak(keycloakUserId));
        } else if ("COMPLETED".equals(status)) {
            order.setCompletedAt(java.time.LocalDateTime.now());
        } else if ("COLLECTED".equals(status)) {
            order.setSpecimenCollectedAt(java.time.LocalDateTime.now());
            order.setSpecimenCollectedByKeycloakId(keycloakUserId);
        }

        LabOrder updatedOrder = labOrderRepository.save(order);
        return mapToLabOrderResponse(updatedOrder);
    }

    private Long getLabTechIdFromKeycloak(String keycloakUserId) {
        // TODO: Call user-service to get lab tech ID from keycloak user ID
        // For now, return null
        return null;
    }

    private LabOrderResponse mapToLabOrderResponse(LabOrder order) {
        List<LabOrderItemResponse> items = order.getOrderItems() != null
                ? order.getOrderItems().stream().map(this::mapToItemResponse).collect(Collectors.toList())
                : List.of();

        return LabOrderResponse.builder()
                .labOrderId(order.getLabOrderId())
                .patientId(order.getPatientId())
                .doctorId(order.getDoctorId())
                .appointmentId(order.getAppointmentId())
                .orderDate(order.getOrderDate())
                .orderType(order.getOrderType())
                .clinicalDiagnosis(order.getClinicalDiagnosis())
                .clinicalNotes(order.getClinicalNotes())
                .urgency(order.getUrgency())
                .status(order.getStatus())
                .specimenCollectedAt(order.getSpecimenCollectedAt())
                .specimenCollectedByKeycloakId(order.getSpecimenCollectedByKeycloakId())
                .processedByLabTechId(order.getProcessedByLabTechId())
                .startedAt(order.getStartedAt())
                .completedAt(order.getCompletedAt())
                .hospitalId(order.getHospitalId())
                .departmentId(order.getDepartmentId())
                .sensitivityLevel(order.getSensitivityLevel())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .orderItems(items)
                .build();
    }

    private LabOrderItemResponse mapToItemResponse(LabOrderItem item) {
        String testName = item.getTest() != null ? item.getTest().getTestName() : null;
        return LabOrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .labOrderId(item.getLabOrder().getLabOrderId())
                .testId(item.getTest().getTestId())
                .testName(testName)
                .status(item.getStatus())
                .priority(item.getPriority())
                .price(item.getPrice())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
