package com.strategy.trade.service;

import com.strategy.trade.entity.Position;
import com.strategy.trade.mapper.PositionMapper;
import com.strategy.trade.model.PositionHolder;
import com.strategy.trade.service.impl.PositionServiceImpl;
import com.strategy.trade.service.impl.PositionTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionTracker positionTracker;

    @Mock
    private PositionMapper positionMapper;

    @InjectMocks
    private PositionServiceImpl positionService;

    private PositionHolder mockPositionHolder;
    private Position mockPosition;

    @BeforeEach
    void setUp() {
        // Create mock Contract for PositionHolder
        com.ib.client.Contract mockContract = new com.ib.client.Contract();
        mockContract.conid(12345);
        mockContract.symbol("AAPL");
        
        mockPositionHolder = new PositionHolder(mockContract, com.ib.client.Decimal.get(100), 150.0);

        mockPosition = Position.builder()
                .conid(12345)
                .symbol("AAPL")
                .secType("STK")
                .exchange("SMART")
                .currency("USD")
                .quantity(new BigDecimal("100"))
                .avgPrice(new BigDecimal("150.00"))
                .build();
    }

    @Test
    void getAllPositions_ShouldReturnListOfPositions() {
        // Given
        List<PositionHolder> positionHolders = Arrays.asList(mockPositionHolder);
        when(positionTracker.getAllPositions()).thenReturn(positionHolders);
        when(positionMapper.convertToTrade(mockPositionHolder)).thenReturn(mockPosition);

        // When
        List<Position> result = positionService.getAllPositions();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockPosition, result.get(0));
        verify(positionTracker).getAllPositions();
        verify(positionMapper).convertToTrade(mockPositionHolder);
    }

    @Test
    void getAllPositions_ShouldReturnEmptyList_WhenNoPositions() {
        // Given
        when(positionTracker.getAllPositions()).thenReturn(Arrays.asList());

        // When
        List<Position> result = positionService.getAllPositions();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(positionTracker).getAllPositions();
        verify(positionMapper, never()).convertToTrade(any());
    }

    @Test
    void getPositionById_ShouldReturnPosition_WhenPositionExists() {
        // Given
        String symbol = "TSLA";
        when(positionTracker.getPositionBySymbol(symbol)).thenReturn(mockPositionHolder);
        when(positionMapper.convertToTrade(mockPositionHolder)).thenReturn(mockPosition);

        // When
        Optional<Position> result = positionService.getPositionBySymbol(symbol);

        // Then
        assertTrue(result.isPresent());
        assertEquals(mockPosition, result.get());
        verify(positionTracker).getPositionBySymbol(symbol);
        verify(positionMapper).convertToTrade(mockPositionHolder);
    }

    @Test
    void getPositionById_ShouldReturnEmptyOptional_WhenPositionDoesNotExist() {
        // Given
        String symbol = "TSLA";
        when(positionTracker.getPositionBySymbol(symbol)).thenReturn(null);
        when(positionMapper.convertToTrade(null)).thenReturn(null);

        // When
        Optional<Position> result = positionService.getPositionBySymbol(symbol);

        // Then
        assertFalse(result.isPresent());
        verify(positionTracker).getPositionBySymbol(symbol);
        verify(positionMapper).convertToTrade(null);
    }

    @Test
    void createPosition_ShouldReturnNull_AsPerCurrentImplementation() {
        // Given
        Position positionToCreate = Position.builder()
                .conid(12345)
                .symbol("TSLA")
                .secType("STK")
                .exchange("SMART")
                .currency("USD")
                .quantity(new BigDecimal("50"))
                .avgPrice(new BigDecimal("200.00"))
                .build();

        // When
        Position result = positionService.createPosition(positionToCreate);

        // Then
        assertNull(result);
        // Note: This test reflects the current implementation which returns null
        // When the createPosition method is properly implemented, this test should be updated
    }

    @Test
    void getAllPositions_ShouldHandleMultiplePositions() {
        // Given
        com.ib.client.Contract mockContract2 = new com.ib.client.Contract();
        mockContract2.conid(67890);
        mockContract2.symbol("GOOGL");
        
        PositionHolder positionHolder2 = new PositionHolder(mockContract2, com.ib.client.Decimal.get(25), 2500.0);

        Position position2 = Position.builder()
                .conid(67890)
                .symbol("GOOGL")
                .secType("STK")
                .exchange("SMART")
                .currency("USD")
                .quantity(new BigDecimal("25"))
                .avgPrice(new BigDecimal("2500.00"))
                .build();

        List<PositionHolder> positionHolders = Arrays.asList(mockPositionHolder, positionHolder2);
        when(positionTracker.getAllPositions()).thenReturn(positionHolders);
        when(positionMapper.convertToTrade(mockPositionHolder)).thenReturn(mockPosition);
        when(positionMapper.convertToTrade(positionHolder2)).thenReturn(position2);

        // When
        List<Position> result = positionService.getAllPositions();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(mockPosition, result.get(0));
        assertEquals(position2, result.get(1));
        verify(positionTracker).getAllPositions();
        verify(positionMapper).convertToTrade(mockPositionHolder);
        verify(positionMapper).convertToTrade(positionHolder2);
    }
}
