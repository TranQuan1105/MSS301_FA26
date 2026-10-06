package com.fudn.customerservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @Nationalized                       // cot NVARCHAR tren SQL Server (BR16)
    @Column(nullable = false, length = 100)
    private String customerName;

    private String telephone;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    private LocalDate customerBirthday;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus customerStatus;

    @Column(nullable = false)
    private String password;
}
