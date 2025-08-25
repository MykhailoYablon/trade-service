package com.example.tradeservice.entity;

import com.example.tradeservice.model.enums.TimeFrame;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.redis.core.RedisHash;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "historical_data",
        indexes = {
                @Index(name = "idx_position_timeframe_timestamp",
                        columnList = "position_id, timeframe")
        })
@Entity
@RedisHash("historical_data")
public class HistoricalData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    @JsonBackReference
    private Position position;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeFrame timeframe;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal open;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal high;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal low;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal close;

    @Column(nullable = false)
    private Long volume;

    @Column(nullable = false)
    private Integer count;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal wap; // Weighted Average Price

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
