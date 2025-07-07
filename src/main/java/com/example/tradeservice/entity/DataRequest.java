package com.example.tradeservice.entity;

import com.example.tradeservice.model.enums.RequestStatus;
import com.example.tradeservice.model.enums.TimeFrame;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "data_requests")
public class DataRequest {
    @Id
    private Integer reqId; // Use IB reqId as primary key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeFrame timeframe;

    @Column(nullable = false)
    private String duration; // "5 D"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private String errorMessage;

    // Getters, setters, constructors...
}
