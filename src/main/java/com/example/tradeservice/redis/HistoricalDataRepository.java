package com.example.tradeservice.redis;

import com.example.tradeservice.entity.HistoricalData;
import com.example.tradeservice.entity.Position;
import com.example.tradeservice.model.enums.TimeFrame;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoricalDataRepository extends CrudRepository<HistoricalData, Long> {
    List<HistoricalData> findByPositionAndTimeframeOrderByTimestampAsc(
            Position position, TimeFrame timeframe);

    List<HistoricalData> findByPositionAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
            Position position, TimeFrame timeframe, LocalDateTime start, LocalDateTime end);

    @Query("SELECT h FROM HistoricalData h WHERE h.position.conid = :conid " +
            "AND h.timeframe = :timeframe AND h.timestamp >= :since ORDER BY h.timestamp ASC")
    List<HistoricalData> findRecentData(@Param("conid") Integer conid,
                                        @Param("timeframe") TimeFrame timeframe,
                                        @Param("since") LocalDateTime since);
}
