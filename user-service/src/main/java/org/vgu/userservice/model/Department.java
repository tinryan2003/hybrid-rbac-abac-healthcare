package org.vgu.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "hospital_id", length = 50)
    private String hospitalId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "updation_date")
    private LocalDateTime updationDate;

    @PrePersist
    protected void onCreate() {
        creationDate = LocalDateTime.now();
        updationDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updationDate = LocalDateTime.now();
    }
}
