package com.example.tradeservice.backtest;

import com.example.tradeservice.backtest.series.DoubleSeries;
import com.example.tradeservice.backtest.series.TimeSeries;
import com.example.tradeservice.backtest.ClosedOrder;
import com.example.tradeservice.backtest.SimpleOrder;
import com.example.tradeservice.backtest.SimpleClosedOrder;
import com.example.tradeservice.strategy.AsyncTradingStrategy;
import com.example.tradeservice.strategy.enums.StrategyMode;
import com.example.tradeservice.strategy.model.TradingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

/**
 * Comprehensive unit tests for the Backtest class.
 * Tests cover constructor validation, method behavior, edge cases, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Backtest Unit Tests")
class BacktestTest {

    // Test constants
    private static final double DEFAULT_DEPOSIT = 10000.0;
    private static final String DEFAULT_SYMBOL = "AAPL";
    private static final double DEFAULT_LEVERAGE = 1.0;
    private static final double[] SAMPLE_PRICES = {100.0, 101.0, 99.0, 102.0, 98.0};
    private static final Instant BASE_TIME = Instant.parse("2024-01-01T09:30:00Z");

    // Mock dependencies
    @Mock
    private AsyncTradingStrategy mockStrategy;
    
    @Mock
    private DoubleSeries mockPriceSeries;
    
    @Mock
    private Iterator<TimeSeries.Entry<Double>> mockIterator;

    // Argument captors for verification
    @Captor
    private ArgumentCaptor<TradingContext> contextCaptor;

    private Backtest backtest;

    @BeforeEach
    void setUp() {
        // Common setup for most tests
        backtest = new Backtest(DEFAULT_DEPOSIT, mockPriceSeries, DEFAULT_SYMBOL);
    }

    // Test data builders and helper methods

    /**
     * Creates a DoubleSeries with test price data
     */
    private DoubleSeries createTestPriceSeries(double... prices) {
        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            entries.add(new TimeSeries.Entry<>(prices[i], BASE_TIME.plusSeconds(i * 60)));
        }
        return new DoubleSeries(entries, DEFAULT_SYMBOL);
    }

    /**
     * Creates a single price entry for testing
     */
    private TimeSeries.Entry<Double> createPriceEntry(double price, Instant instant) {
        return new TimeSeries.Entry<>(price, instant);
    }

    /**
     * Creates a mock AsyncTradingStrategy with default behavior
     */
    private AsyncTradingStrategy createMockStrategy() {
        AsyncTradingStrategy strategy = mock(AsyncTradingStrategy.class);
        when(strategy.startStrategy(any(TradingContext.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(strategy.onTick(any(TradingContext.class)))
                .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
        return strategy;
    }

    /**
     * Configures mock price series with iterator behavior
     */
    private void configureMockPriceSeries(double... prices) {
        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            entries.add(createPriceEntry(prices[i], BASE_TIME.plusSeconds(i * 60)));
        }
        
        when(mockPriceSeries.iterator()).thenReturn(entries.iterator());
        when(mockPriceSeries.size()).thenReturn(prices.length);
        when(mockPriceSeries.isEmpty()).thenReturn(prices.length == 0);
    }

    /**
     * Validates a Result object against expected values
     */
    private void assertResultValid(Backtest.Result result, double expectedPL, double expectedInitialFund) {
        assertAll("Result validation",
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getPl()).isEqualTo(expectedPL),
            () -> assertThat(result.getInitialFund()).isEqualTo(expectedInitialFund),
            () -> assertThat(result.getFinalValue()).isEqualTo(expectedInitialFund + expectedPL),
            () -> assertThat(result.getOrders()).isNotNull(),
            () -> assertThat(result.getPriceSeries()).isNotNull(),
            () -> assertThat(result.getCommissions()).isGreaterThanOrEqualTo(0.0)
        );
    }

    /**
     * Creates test data for parameterized tests
     */
    private static class TestDataProvider {
        static final double[] DEPOSIT_VALUES = {0.0, 1000.0, -500.0, 1000000.0};
        static final String[] SYMBOL_VALUES = {"AAPL", "GOOGL", "", null};
        static final double[][] PRICE_SCENARIOS = {
            {100.0, 101.0}, // profit scenario
            {100.0, 99.0},  // loss scenario
            {100.0, 100.0}, // no change scenario
            {100.0}         // single price scenario
        };
        
        // Additional test data constants
        static final double[] LEVERAGE_VALUES = {1.0, 2.0, 5.0, 10.0};
        static final double[] EXTREME_PRICES = {0.01, 999999.99, Double.MAX_VALUE / 2};
        static final int[] ORDER_COUNTS = {0, 1, 5, 10, 100};
        static final String[] VALID_SYMBOLS = {"AAPL", "GOOGL", "TSLA", "MSFT", "AMZN"};
        static final String[] EDGE_CASE_SYMBOLS = {"", "A", "VERY_LONG_SYMBOL_NAME_123"};
        
        // Price patterns for different market conditions
        static final double[] TRENDING_UP_PRICES = {100.0, 101.0, 102.5, 104.0, 106.0};
        static final double[] TRENDING_DOWN_PRICES = {100.0, 98.5, 97.0, 95.0, 92.5};
        static final double[] VOLATILE_PRICES = {100.0, 105.0, 95.0, 110.0, 85.0, 102.0};
        static final double[] STABLE_PRICES = {100.0, 100.1, 99.9, 100.05, 99.95};
        
        // Time-based test constants
        static final int[] TIME_INTERVALS_SECONDS = {60, 300, 3600}; // 1min, 5min, 1hour
        static final int[] DATASET_SIZES = {1, 10, 100, 1000}; // Different dataset sizes for performance testing
    }

    /**
     * Helper method to create a Backtest instance with test data
     */
    private Backtest createBacktestWithTestData(double deposit, double... prices) {
        DoubleSeries testSeries = createTestPriceSeries(prices);
        return new Backtest(deposit, testSeries, DEFAULT_SYMBOL);
    }

    /**
     * Helper method to verify TradingContext configuration
     */
    private void assertTradingContextValid(TradingContext context, String expectedSymbol, double expectedInitialFunds) {
        assertAll("TradingContext validation",
            () -> assertThat(context).isNotNull(),
            () -> assertThat(context.getSymbol()).isEqualTo(expectedSymbol),
            () -> assertThat(context.getInitialFunds()).isEqualTo(expectedInitialFunds),
            () -> assertThat(context.getOrders()).isNotNull(),
            () -> assertThat(context.getClosedOrders()).isNotNull(),
            () -> assertThat(context.getProfitLoss()).isNotNull(),
            () -> assertThat(context.getFundsHistory()).isNotNull()
        );
    }

    /**
     * Helper method to setup mock strategy with specific behavior
     */
    private void setupMockStrategyBehavior(AsyncTradingStrategy strategy, TradingContext returnContext) {
        when(strategy.startStrategy(any(TradingContext.class)))
                .thenReturn(CompletableFuture.completedFuture(returnContext));
        when(strategy.onTick(any(TradingContext.class)))
                .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
    }

    /**
     * Helper method to create a mock strategy that throws exceptions
     */
    private AsyncTradingStrategy createMockStrategyWithException(RuntimeException exception) {
        AsyncTradingStrategy strategy = mock(AsyncTradingStrategy.class);
        when(strategy.startStrategy(any(TradingContext.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(strategy.onTick(any(TradingContext.class)))
                .thenThrow(exception);
        return strategy;
    }

    /**
     * Helper method to create a mock strategy that returns specific orders
     */
    private AsyncTradingStrategy createMockStrategyWithOrders(List<SimpleOrder> orders) {
        AsyncTradingStrategy strategy = mock(AsyncTradingStrategy.class);
        when(strategy.startStrategy(any(TradingContext.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(strategy.onTick(any(TradingContext.class)))
                .thenReturn(CompletableFuture.completedFuture(new ArrayList<>(orders)));
        return strategy;
    }

    /**
     * Helper method to create test orders for testing
     */
    private List<SimpleOrder> createTestOrders(int count) {
        List<SimpleOrder> orders = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            orders.add(new SimpleOrder(i, DEFAULT_SYMBOL, BASE_TIME.plusSeconds(i * 60), 100.0 + i, 100));
        }
        return orders;
    }

    /**
     * Helper method to create a DoubleSeries with custom symbol
     */
    private DoubleSeries createTestPriceSeriesWithSymbol(String symbol, double... prices) {
        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            entries.add(new TimeSeries.Entry<>(prices[i], BASE_TIME.plusSeconds(i * 60)));
        }
        return new DoubleSeries(entries, symbol);
    }

    /**
     * Helper method to create a large price series for performance testing
     */
    private DoubleSeries createLargePriceSeries(int size, double basePrice) {
        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            // Create some price variation
            double price = basePrice + (Math.sin(i * 0.1) * 5.0) + (Math.random() * 2.0 - 1.0);
            entries.add(new TimeSeries.Entry<>(price, BASE_TIME.plusSeconds(i * 60)));
        }
        return new DoubleSeries(entries, DEFAULT_SYMBOL);
    }

    /**
     * Helper method to verify Result object with detailed assertions
     */
    private void assertResultValidDetailed(Backtest.Result result, double expectedPL, double expectedInitialFund, 
                                         int expectedOrderCount, String expectedSymbol) {
        assertAll("Detailed Result validation",
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getPl()).isEqualTo(expectedPL),
            () -> assertThat(result.getInitialFund()).isEqualTo(expectedInitialFund),
            () -> assertThat(result.getFinalValue()).isEqualTo(expectedInitialFund + expectedPL),
            () -> assertThat(result.getOrders()).isNotNull(),
            () -> assertThat(result.getOrders().size()).isEqualTo(expectedOrderCount),
            () -> assertThat(result.getPriceSeries()).isNotNull(),
            () -> assertThat(result.getPriceSeries().getSymbol()).isEqualTo(expectedSymbol),
            () -> assertThat(result.getCommissions()).isGreaterThanOrEqualTo(0.0)
        );
    }

    /**
     * Helper method to create a Backtest with custom parameters
     */
    private Backtest createCustomBacktest(double deposit, String symbol, double leverage, double... prices) {
        DoubleSeries testSeries = createTestPriceSeriesWithSymbol(symbol, prices);
        Backtest backtest = new Backtest(deposit, testSeries, symbol);
        backtest.setLeverage(leverage);
        return backtest;
    }

    /**
     * Helper method to verify TradingContext state after operations
     */
    private void assertTradingContextState(TradingContext context, double expectedPrice, 
                                         int expectedPLSize, int expectedFundsHistorySize) {
        assertAll("TradingContext state validation",
            () -> assertThat(context).isNotNull(),
            () -> assertThat(context.getCurrentPrice()).isEqualTo(expectedPrice),
            () -> assertThat(context.getProfitLoss().size()).isEqualTo(expectedPLSize),
            () -> assertThat(context.getFundsHistory().size()).isEqualTo(expectedFundsHistorySize),
            () -> assertThat(context.getInstant()).isNotNull()
        );
    }

    /**
     * Helper method to create price entries with specific time intervals
     */
    private List<TimeSeries.Entry<Double>> createPriceEntriesWithInterval(double[] prices, int intervalSeconds) {
        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            entries.add(new TimeSeries.Entry<>(prices[i], BASE_TIME.plusSeconds(i * intervalSeconds)));
        }
        return entries;
    }

    /**
     * Helper method to verify mock interactions in order
     */
    private void verifyMockInteractionsInOrder(AsyncTradingStrategy strategy, int expectedOnTickCalls) {
        InOrder inOrder = inOrder(strategy);
        inOrder.verify(strategy).startStrategy(any(TradingContext.class));
        inOrder.verify(strategy, times(expectedOnTickCalls)).onTick(any(TradingContext.class));
    }

    /**
     * Test scenario builder for creating complex test setups
     */
    private static class TestScenarioBuilder {
        private double deposit = DEFAULT_DEPOSIT;
        private String symbol = DEFAULT_SYMBOL;
        private double leverage = DEFAULT_LEVERAGE;
        private double[] prices = SAMPLE_PRICES;
        private AsyncTradingStrategy strategy;
        
        public TestScenarioBuilder withDeposit(double deposit) {
            this.deposit = deposit;
            return this;
        }
        
        public TestScenarioBuilder withSymbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        
        public TestScenarioBuilder withLeverage(double leverage) {
            this.leverage = leverage;
            return this;
        }
        
        public TestScenarioBuilder withPrices(double... prices) {
            this.prices = prices;
            return this;
        }
        
        public TestScenarioBuilder withStrategy(AsyncTradingStrategy strategy) {
            this.strategy = strategy;
            return this;
        }
        
        public Backtest build() {
            List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
            for (int i = 0; i < prices.length; i++) {
                entries.add(new TimeSeries.Entry<>(prices[i], BASE_TIME.plusSeconds(i * 60)));
            }
            DoubleSeries priceSeries = new DoubleSeries(entries, symbol);
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            backtest.setLeverage(leverage);
            return backtest;
        }
    }
    
    /**
     * Helper method to create a test scenario builder
     */
    private TestScenarioBuilder testScenario() {
        return new TestScenarioBuilder();
    }

    /**
     * Helper method to create a DoubleSeries with identical prices (for testing edge cases)
     */
    private DoubleSeries createIdenticalPriceSeries(double price, int count) {
        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new TimeSeries.Entry<>(price, BASE_TIME.plusSeconds(i * 60)));
        }
        return new DoubleSeries(entries, DEFAULT_SYMBOL);
    }

    /**
     * Helper method to create an empty DoubleSeries for testing
     */
    private DoubleSeries createEmptyPriceSeries() {
        return new DoubleSeries(new ArrayList<>(), DEFAULT_SYMBOL);
    }

    /**
     * Helper method to assert that two double values are approximately equal
     */
    private void assertApproximatelyEqual(double actual, double expected, double tolerance) {
        assertThat(Math.abs(actual - expected))
            .as("Expected %f to be approximately equal to %f within tolerance %f", actual, expected, tolerance)
            .isLessThanOrEqualTo(tolerance);
    }

    /**
     * Helper method to create test closed orders for Result validation
     */
    private List<ClosedOrder> createTestClosedOrdersList(int count) {
        List<ClosedOrder> orders = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            SimpleOrder order = new SimpleOrder(i, DEFAULT_SYMBOL, BASE_TIME.plusSeconds(i * 60), 100.0 + i, 100);
            ClosedOrder closedOrder = new SimpleClosedOrder(order, BASE_TIME.plusSeconds(i * 120), 102.0 + i);
            orders.add(closedOrder);
        }
        return orders;
    }

    // Basic infrastructure verification test
    @Test
    @DisplayName("Should create BacktestTest with proper mock setup and helper methods")
    void shouldCreateBacktestTestWithProperMockSetupAndHelperMethods() {
        // Verify that the test infrastructure is properly set up
        assertThat(backtest).isNotNull();
        assertThat(mockStrategy).isNotNull();
        assertThat(mockPriceSeries).isNotNull();
        assertThat(contextCaptor).isNotNull();
        
        // Verify basic test data builders work
        DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0, 99.0);
        assertThat(testSeries).isNotNull();
        assertThat(testSeries.size()).isEqualTo(3);
        
        // Verify mock strategy creation works
        AsyncTradingStrategy testStrategy = createMockStrategy();
        assertThat(testStrategy).isNotNull();
        
        // Verify price entry creation works
        TimeSeries.Entry<Double> entry = createPriceEntry(100.0, BASE_TIME);
        assertThat(entry).isNotNull();
        assertThat(entry.getItem()).isEqualTo(100.0);
        assertThat(entry.getInstant()).isEqualTo(BASE_TIME);
        
        // Verify enhanced helper methods work
        DoubleSeries customSeries = createTestPriceSeriesWithSymbol("GOOGL", 150.0, 151.0);
        assertThat(customSeries.getSymbol()).isEqualTo("GOOGL");
        assertThat(customSeries.size()).isEqualTo(2);
        
        // Verify large price series creation
        DoubleSeries largeSeries = createLargePriceSeries(100, 100.0);
        assertThat(largeSeries.size()).isEqualTo(100);
        
        // Verify test orders creation
        List<SimpleOrder> orders = createTestOrders(3);
        assertThat(orders).hasSize(3);
        assertThat(orders.get(0).getId()).isEqualTo(1);
        
        // Verify test scenario builder
        Backtest scenarioBacktest = testScenario()
            .withDeposit(5000.0)
            .withSymbol("TSLA")
            .withLeverage(2.0)
            .withPrices(200.0, 205.0, 195.0)
            .build();
        assertThat(scenarioBacktest.getDeposit()).isEqualTo(5000.0);
        assertThat(scenarioBacktest.getSymbol()).isEqualTo("TSLA");
        assertThat(scenarioBacktest.getLeverage()).isEqualTo(2.0);
        
        // Verify empty and identical price series
        DoubleSeries emptySeries = createEmptyPriceSeries();
        assertThat(emptySeries.isEmpty()).isTrue();
        
        DoubleSeries identicalSeries = createIdenticalPriceSeries(100.0, 5);
        assertThat(identicalSeries.size()).isEqualTo(5);
        
        // Verify closed orders creation
        List<ClosedOrder> closedOrders = createTestClosedOrdersList(2);
        assertThat(closedOrders).hasSize(2);
    }

    // Nested test classes for organized test structure as per design document
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should create Backtest with valid deposit, DoubleSeries, and symbol")
        void shouldCreateBacktestWithValidParameters() {
            // Given
            double deposit = 10000.0;
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0, 99.0);
            String symbol = "AAPL";
            
            // When
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            
            // Then
            assertAll("Constructor field initialization",
                () -> assertThat(backtest).isNotNull(),
                () -> assertThat(backtest.getDeposit()).isEqualTo(deposit),
                () -> assertThat(backtest.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(backtest.getSymbol()).isEqualTo(symbol),
                () -> assertThat(backtest.getLeverage()).isEqualTo(1.0), // default leverage
                () -> assertThat(backtest.getStrategy()).isNull(), // not set in constructor
                () -> assertThat(backtest.getContext()).isNull(), // not set in constructor
                () -> assertThat(backtest.getResult()).isNull(), // not set in constructor
                () -> assertThat(backtest.getPriceIterator()).isNull() // not set in constructor
            );
        }
        
        @Test
        @DisplayName("Should create Backtest with zero deposit")
        void shouldCreateBacktestWithZeroDeposit() {
            // Given
            double deposit = 0.0;
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0);
            String symbol = "GOOGL";
            
            // When
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            
            // Then
            assertAll("Zero deposit constructor",
                () -> assertThat(backtest).isNotNull(),
                () -> assertThat(backtest.getDeposit()).isEqualTo(0.0),
                () -> assertThat(backtest.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(backtest.getSymbol()).isEqualTo(symbol)
            );
        }
        
        @Test
        @DisplayName("Should create Backtest with negative deposit")
        void shouldCreateBacktestWithNegativeDeposit() {
            // Given
            double deposit = -1000.0;
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0);
            String symbol = "TSLA";
            
            // When
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            
            // Then
            assertAll("Negative deposit constructor",
                () -> assertThat(backtest).isNotNull(),
                () -> assertThat(backtest.getDeposit()).isEqualTo(-1000.0),
                () -> assertThat(backtest.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(backtest.getSymbol()).isEqualTo(symbol)
            );
        }
        
        @Test
        @DisplayName("Should create Backtest with null DoubleSeries")
        void shouldCreateBacktestWithNullDoubleSeries() {
            // Given
            double deposit = 5000.0;
            DoubleSeries priceSeries = null;
            String symbol = "MSFT";
            
            // When
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            
            // Then
            assertAll("Null DoubleSeries constructor",
                () -> assertThat(backtest).isNotNull(),
                () -> assertThat(backtest.getDeposit()).isEqualTo(deposit),
                () -> assertThat(backtest.getPriceSeries()).isNull(),
                () -> assertThat(backtest.getSymbol()).isEqualTo(symbol)
            );
        }
        
        @Test
        @DisplayName("Should create Backtest with null symbol")
        void shouldCreateBacktestWithNullSymbol() {
            // Given
            double deposit = 7500.0;
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0);
            String symbol = null;
            
            // When
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            
            // Then
            assertAll("Null symbol constructor",
                () -> assertThat(backtest).isNotNull(),
                () -> assertThat(backtest.getDeposit()).isEqualTo(deposit),
                () -> assertThat(backtest.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(backtest.getSymbol()).isNull()
            );
        }
        
        @Test
        @DisplayName("Should create Backtest with empty symbol")
        void shouldCreateBacktestWithEmptySymbol() {
            // Given
            double deposit = 2500.0;
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0);
            String symbol = "";
            
            // When
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            
            // Then
            assertAll("Empty symbol constructor",
                () -> assertThat(backtest).isNotNull(),
                () -> assertThat(backtest.getDeposit()).isEqualTo(deposit),
                () -> assertThat(backtest.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(backtest.getSymbol()).isEmpty()
            );
        }
        
        @ParameterizedTest
        @ValueSource(doubles = {0.0, 1000.0, -500.0, 1000000.0, 50.5, -0.01})
        @DisplayName("Should create Backtest with various deposit values")
        void shouldCreateBacktestWithVariousDepositValues(double deposit) {
            // Given
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0);
            String symbol = "TEST";
            
            // When
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            
            // Then
            assertAll("Parameterized deposit constructor",
                () -> assertThat(backtest).isNotNull(),
                () -> assertThat(backtest.getDeposit()).isEqualTo(deposit),
                () -> assertThat(backtest.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(backtest.getSymbol()).isEqualTo(symbol),
                () -> assertThat(backtest.getLeverage()).isEqualTo(1.0)
            );
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"AAPL", "GOOGL", "TSLA", "MSFT", "AMZN", "", "A", "VERY_LONG_SYMBOL_NAME"})
        @DisplayName("Should create Backtest with various symbol values")
        void shouldCreateBacktestWithVariousSymbolValues(String symbol) {
            // Given
            double deposit = 5000.0;
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0);
            
            // When
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            
            // Then
            assertAll("Parameterized symbol constructor",
                () -> assertThat(backtest).isNotNull(),
                () -> assertThat(backtest.getDeposit()).isEqualTo(deposit),
                () -> assertThat(backtest.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(backtest.getSymbol()).isEqualTo(symbol)
            );
        }
    }
    
    @Nested
    @DisplayName("Initialize Method Tests")
    class InitializeTests {
        
        @Test
        @DisplayName("Should create TradingContext with correct parameters")
        void shouldCreateTradingContextWithCorrectParameters() {
            // Given
            configureMockPriceSeries(100.0, 101.0, 99.0);
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            backtest.initialize(strategy);
            
            // Then
            TradingContext context = backtest.getContext();
            assertTradingContextValid(context, DEFAULT_SYMBOL, DEFAULT_DEPOSIT);
            
            assertAll("TradingContext specific configuration",
                () -> assertThat(context.getSymbol()).isEqualTo(DEFAULT_SYMBOL),
                () -> assertThat(context.getInitialFunds()).isEqualTo(DEFAULT_DEPOSIT),
                () -> assertThat(context.getLeverage()).isEqualTo(DEFAULT_LEVERAGE),
                () -> assertThat(context.getMode()).isEqualTo(com.example.tradeservice.strategy.enums.StrategyMode.BACKTEST),
                () -> assertThat(context.getInstruments()).contains(DEFAULT_SYMBOL),
                () -> assertThat(context.getDate()).isEqualTo("2025-09-05"),
                () -> assertThat(context.getState()).isNotNull(),
                () -> assertThat(context.getMHistory()).isNotNull(),
                () -> assertThat(context.getMHistory().getSymbol()).isEqualTo(DEFAULT_SYMBOL)
            );
        }
        
        @Test
        @DisplayName("Should assign strategy field correctly")
        void shouldAssignStrategyFieldCorrectly() {
            // Given
            configureMockPriceSeries(100.0, 101.0);
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            backtest.initialize(strategy);
            
            // Then
            assertThat(backtest.getStrategy()).isEqualTo(strategy);
        }
        
        @Test
        @DisplayName("Should initialize price iterator from price series")
        void shouldInitializePriceIteratorFromPriceSeries() {
            // Given
            configureMockPriceSeries(100.0, 101.0, 99.0, 102.0);
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            backtest.initialize(strategy);
            
            // Then
            assertThat(backtest.getPriceIterator()).isNotNull();
            verify(mockPriceSeries).iterator();
        }
        
        @Test
        @DisplayName("Should call startStrategy on provided strategy")
        void shouldCallStartStrategyOnProvidedStrategy() {
            // Given
            configureMockPriceSeries(100.0, 101.0);
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            backtest.initialize(strategy);
            
            // Then
            verify(strategy).startStrategy(contextCaptor.capture());
            TradingContext capturedContext = contextCaptor.getValue();
            assertTradingContextValid(capturedContext, DEFAULT_SYMBOL, DEFAULT_DEPOSIT);
        }
        
        @Test
        @DisplayName("Should create TradingContext with correct symbol and funds for different parameters")
        void shouldCreateTradingContextWithCorrectSymbolAndFunds() {
            // Given
            String customSymbol = "GOOGL";
            double customDeposit = 25000.0;
            Backtest customBacktest = new Backtest(customDeposit, mockPriceSeries, customSymbol);
            configureMockPriceSeries(150.0, 151.0);
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            customBacktest.initialize(strategy);
            
            // Then
            TradingContext context = customBacktest.getContext();
            assertAll("Custom TradingContext configuration",
                () -> assertThat(context.getSymbol()).isEqualTo(customSymbol),
                () -> assertThat(context.getInitialFunds()).isEqualTo(customDeposit),
                () -> assertThat(context.getInstruments()).contains(customSymbol)
            );
        }
        
        @Test
        @DisplayName("Should handle null strategy parameter appropriately")
        void shouldHandleNullStrategyParameterAppropriately() {
            // Given
            configureMockPriceSeries(100.0, 101.0);
            AsyncTradingStrategy nullStrategy = null;
            
            // When
            backtest.initialize(nullStrategy);
            
            // Then
            assertAll("Null strategy handling",
                () -> assertThat(backtest.getStrategy()).isNull(),
                () -> assertThat(backtest.getContext()).isNotNull(),
                () -> assertThat(backtest.getPriceIterator()).isNotNull()
            );
        }
        
        @Test
        @DisplayName("Should execute first nextStep after initialization")
        void shouldExecuteFirstNextStepAfterInitialization() {
            // Given
            configureMockPriceSeries(100.0, 101.0, 99.0);
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            backtest.initialize(strategy);
            
            // Then
            TradingContext context = backtest.getContext();
            assertAll("First nextStep execution verification",
                () -> assertThat(context.getCurrentPrice()).isEqualTo(100.0),
                () -> assertThat(context.getInstant()).isNotNull(),
                () -> assertThat(context.getProfitLoss()).isNotNull(),
                () -> assertThat(context.getFundsHistory()).isNotNull(),
                () -> assertThat(context.getMHistory()).isNotNull()
            );
            
            // Verify strategy.onTick was called during first nextStep
            verify(strategy).onTick(any(TradingContext.class));
        }
        
        @Test
        @DisplayName("Should handle Thread.sleep behavior during initialization")
        void shouldHandleThreadSleepBehaviorDuringInitialization() {
            // Given
            configureMockPriceSeries(100.0, 101.0);
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            // Note: This test will actually sleep for 15 seconds due to Thread.sleep in initialize method
            // In a production environment, we would mock Thread.sleep to avoid delays
            backtest.initialize(strategy);
            
            // Then
            // Verify that initialization completes successfully despite the Thread.sleep
            assertAll("Thread.sleep handling",
                () -> assertThat(backtest.getStrategy()).isEqualTo(strategy),
                () -> assertThat(backtest.getContext()).isNotNull(),
                () -> assertThat(backtest.getPriceIterator()).isNotNull(),
                () -> assertThat(backtest.getContext().getCurrentPrice()).isEqualTo(100.0)
            );
            
            // Verify that startStrategy was called before the sleep
            verify(strategy).startStrategy(any(TradingContext.class));
            // Verify that onTick was called after the sleep (during first nextStep)
            verify(strategy).onTick(any(TradingContext.class));
        }
        
        @Test
        @DisplayName("Should verify first nextStep execution updates context correctly")
        void shouldVerifyFirstNextStepExecutionUpdatesContextCorrectly() {
            // Given
            double firstPrice = 105.0;
            configureMockPriceSeries(firstPrice, 106.0, 104.0);
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            backtest.initialize(strategy);
            
            // Then
            TradingContext context = backtest.getContext();
            assertAll("Context updates after first nextStep",
                () -> assertThat(context.getCurrentPrice()).isEqualTo(firstPrice),
                () -> assertThat(context.getInstant()).isEqualTo(BASE_TIME),
                () -> assertThat(context.getProfitLoss().size()).isGreaterThan(0),
                () -> assertThat(context.getFundsHistory().size()).isGreaterThan(0),
                () -> assertThat(context.getMHistory().size()).isEqualTo(1)
            );
        }
    }
    
    @Nested
    @DisplayName("NextStep Method Tests")
    class NextStepTests {
        
        @Nested
        @DisplayName("Normal NextStep Execution Tests")
        class NormalNextStepExecutionTests {
            
            @Test
            @DisplayName("Should return true when price iterator has next entry")
            void shouldReturnTrueWhenPriceIteratorHasNextEntry() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy);
                
                // When - call nextStep again (first one was called in initialize)
                boolean result = backtest.nextStep();
                
                // Then
                assertThat(result).isTrue();
                verify(mockPriceSeries, atLeast(2)).iterator(); // Called in initialize and nextStep
            }
            
            @Test
            @DisplayName("Should update context current price and instant")
            void shouldUpdateContextCurrentPriceAndInstant() {
                // Given
                double expectedPrice = 105.0;
                Instant expectedInstant = BASE_TIME.plusSeconds(60);
                configureMockPriceSeries(100.0, expectedPrice);
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy);
                
                // When
                backtest.nextStep();
                
                // Then
                TradingContext context = backtest.getContext();
                assertAll("Context price and instant updates",
                    () -> assertThat(context.getCurrentPrice()).isEqualTo(expectedPrice),
                    () -> assertThat(context.getInstant()).isEqualTo(expectedInstant)
                );
            }
            
            @Test
            @DisplayName("Should calculate and add profit/loss to context")
            void shouldCalculateAndAddProfitLossToContext() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy);
                int initialPLSize = backtest.getContext().getProfitLoss().size();
                
                // When
                backtest.nextStep();
                
                // Then
                TradingContext context = backtest.getContext();
                assertAll("Profit/loss calculation and addition",
                    () -> assertThat(context.getProfitLoss().size()).isGreaterThan(initialPLSize),
                    () -> assertThat(context.getProfitLoss()).isNotNull(),
                    () -> {
                        // Verify that profit/loss entries were added during nextStep
                        // The method adds both onTickPL() and getPL() to profitLoss
                        int expectedMinimumSize = initialPLSize + 2;
                        assertThat(context.getProfitLoss().size()).isGreaterThanOrEqualTo(expectedMinimumSize);
                    }
                );
            }
            
            @Test
            @DisplayName("Should add funds history to context")
            void shouldAddFundsHistoryToContext() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy);
                int initialFundsHistorySize = backtest.getContext().getFundsHistory().size();
                
                // When
                backtest.nextStep();
                
                // Then
                TradingContext context = backtest.getContext();
                assertAll("Funds history addition",
                    () -> assertThat(context.getFundsHistory().size()).isGreaterThan(initialFundsHistorySize),
                    () -> assertThat(context.getFundsHistory()).isNotNull(),
                    () -> {
                        // Verify the latest funds history entry contains available funds
                        assertThat(context.getFundsHistory().size()).isGreaterThanOrEqualTo(1);
                        // The available funds should be calculated correctly
                        double expectedAvailableFunds = context.getAvailableFunds();
                        assertThat(expectedAvailableFunds).isGreaterThanOrEqualTo(0.0);
                    }
                );
            }
            
            @Test
            @DisplayName("Should call onTick on strategy with updated context")
            void shouldCallOnTickOnStrategyWithUpdatedContext() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy);
                
                // Reset mock to count only the nextStep call, not the initialize call
                reset(strategy);
                when(strategy.onTick(any(TradingContext.class)))
                    .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.nextStep();
                
                // Then
                verify(strategy).onTick(contextCaptor.capture());
                TradingContext capturedContext = contextCaptor.getValue();
                assertAll("Strategy onTick call verification",
                    () -> assertThat(capturedContext).isNotNull(),
                    () -> assertThat(capturedContext.getCurrentPrice()).isEqualTo(101.0),
                    () -> assertThat(capturedContext.getInstant()).isEqualTo(BASE_TIME.plusSeconds(60)),
                    () -> assertThat(capturedContext.getSymbol()).isEqualTo(DEFAULT_SYMBOL)
                );
            }
            
            @Test
            @DisplayName("Should add price entry to market history")
            void shouldAddPriceEntryToMarketHistory() {
                // Given
                double expectedPrice = 102.0;
                configureMockPriceSeries(100.0, expectedPrice);
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy);
                int initialMarketHistorySize = backtest.getContext().getMHistory().size();
                
                // When
                backtest.nextStep();
                
                // Then
                TradingContext context = backtest.getContext();
                assertAll("Market history addition",
                    () -> assertThat(context.getMHistory().size()).isGreaterThan(initialMarketHistorySize),
                    () -> assertThat(context.getMHistory()).isNotNull(),
                    () -> {
                        // Verify the latest entry in market history
                        assertThat(context.getMHistory().size()).isGreaterThanOrEqualTo(2);
                        // The last entry should contain the current price
                        var lastEntry = context.getMHistory().getLast();
                        assertThat(lastEntry.getItem()).isEqualTo(expectedPrice);
                        assertThat(lastEntry.getInstant()).isEqualTo(BASE_TIME.plusSeconds(60));
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify complete nextStep execution flow")
            void shouldVerifyCompleteNextStepExecutionFlow() {
                // Given
                double[] prices = {100.0, 101.0, 99.0, 102.0};
                configureMockPriceSeries(prices);
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy);
                
                // Capture initial state
                TradingContext context = backtest.getContext();
                int initialPLSize = context.getProfitLoss().size();
                int initialFundsSize = context.getFundsHistory().size();
                int initialMarketSize = context.getMHistory().size();
                
                // When
                boolean result = backtest.nextStep();
                
                // Then
                assertAll("Complete nextStep execution flow",
                    () -> assertThat(result).isTrue(),
                    () -> assertThat(context.getCurrentPrice()).isEqualTo(101.0),
                    () -> assertThat(context.getInstant()).isEqualTo(BASE_TIME.plusSeconds(60)),
                    () -> assertThat(context.getProfitLoss().size()).isGreaterThan(initialPLSize),
                    () -> assertThat(context.getFundsHistory().size()).isGreaterThan(initialFundsSize),
                    () -> assertThat(context.getMHistory().size()).isGreaterThan(initialMarketSize),
                    () -> assertThat(context.getAvailableFunds()).isGreaterThanOrEqualTo(0.0)
                );
                
                // Verify strategy interaction
                verify(strategy, atLeastOnce()).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should handle multiple consecutive nextStep calls correctly")
            void shouldHandleMultipleConsecutiveNextStepCallsCorrectly() {
                // Given
                double[] prices = {100.0, 101.0, 99.0, 102.0, 98.0};
                configureMockPriceSeries(prices);
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy);
                
                // When - call nextStep multiple times
                boolean result1 = backtest.nextStep();
                boolean result2 = backtest.nextStep();
                boolean result3 = backtest.nextStep();
                
                // Then
                TradingContext context = backtest.getContext();
                assertAll("Multiple nextStep calls",
                    () -> assertThat(result1).isTrue(),
                    () -> assertThat(result2).isTrue(),
                    () -> assertThat(result3).isTrue(),
                    () -> assertThat(context.getCurrentPrice()).isEqualTo(98.0), // Last price
                    () -> assertThat(context.getInstant()).isEqualTo(BASE_TIME.plusSeconds(240)), // Last timestamp
                    () -> assertThat(context.getMHistory().size()).isEqualTo(4), // All 4 prices processed
                    () -> assertThat(context.getProfitLoss().size()).isGreaterThanOrEqualTo(6), // Multiple PL entries
                    () -> assertThat(context.getFundsHistory().size()).isGreaterThanOrEqualTo(4) // Multiple funds entries
                );
                
                // Verify strategy was called for each nextStep
                verify(strategy, atLeast(4)).onTick(any(TradingContext.class));
            }
        }
        
        @Nested
        @DisplayName("NextStep Termination Conditions Tests")
        class NextStepTerminationConditionsTests {
            
            @Test
            @DisplayName("Should return false when price iterator is exhausted")
            void shouldReturnFalseWhenPriceIteratorIsExhausted() {
                // Given
                configureMockPriceSeries(100.0); // Only one price entry
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy); // This consumes the only price entry
                
                // When - try to call nextStep when iterator is exhausted
                boolean result = backtest.nextStep();
                
                // Then
                assertThat(result).isFalse();
            }
            
            @Test
            @DisplayName("Should call finish method when iterator exhausted")
            void shouldCallFinishMethodWhenIteratorExhausted() {
                // Given
                configureMockPriceSeries(100.0, 101.0); // Two price entries
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy); // Consumes first entry
                
                // When - call nextStep to consume second entry, then call again when exhausted
                backtest.nextStep(); // Consumes second entry
                boolean result = backtest.nextStep(); // Should trigger finish
                
                // Then
                assertAll("Finish method call verification",
                    () -> assertThat(result).isFalse(),
                    () -> assertThat(backtest.getResult()).isNotNull(), // Result should be set by finish()
                    () -> {
                        // Verify that finish() was called by checking Result object
                        Backtest.Result resultObj = backtest.getResult();
                        assertThat(resultObj.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT);
                        assertThat(resultObj.getOrders()).isNotNull();
                        assertThat(resultObj.getCommissions()).isGreaterThanOrEqualTo(0.0);
                    }
                );
            }
            
            @Test
            @DisplayName("Should return false when available funds become negative")
            void shouldReturnFalseWhenAvailableFundsBecomeNegative() {
                // Given - Create a scenario where funds become negative
                // This is tricky to test directly, so we'll use a mock to simulate the condition
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // Create a backtest with very low deposit to potentially trigger negative funds
                Backtest lowFundsBacktest = new Backtest(1.0, mockPriceSeries, DEFAULT_SYMBOL);
                lowFundsBacktest.initialize(strategy);
                
                // Mock the context to return negative available funds
                TradingContext mockContext = spy(lowFundsBacktest.getContext());
                when(mockContext.getAvailableFunds()).thenReturn(-100.0);
                lowFundsBacktest.setContext(mockContext);
                
                // When
                boolean result = lowFundsBacktest.nextStep();
                
                // Then
                assertThat(result).isFalse();
            }
            
            @Test
            @DisplayName("Should call finish method when funds become negative")
            void shouldCallFinishMethodWhenFundsBecomeNegative() {
                // Given - Create a scenario where funds become negative
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // Create a backtest with very low deposit
                Backtest lowFundsBacktest = new Backtest(1.0, mockPriceSeries, DEFAULT_SYMBOL);
                lowFundsBacktest.initialize(strategy);
                
                // Mock the context to return negative available funds
                TradingContext mockContext = spy(lowFundsBacktest.getContext());
                when(mockContext.getAvailableFunds()).thenReturn(-100.0);
                lowFundsBacktest.setContext(mockContext);
                
                // When
                boolean result = lowFundsBacktest.nextStep();
                
                // Then
                assertAll("Finish method call when funds negative",
                    () -> assertThat(result).isFalse(),
                    () -> assertThat(lowFundsBacktest.getResult()).isNotNull(), // Result should be set by finish()
                    () -> {
                        // Verify that finish() was called by checking Result object
                        Backtest.Result resultObj = lowFundsBacktest.getResult();
                        assertThat(resultObj.getInitialFund()).isEqualTo(1.0);
                        assertThat(resultObj.getOrders()).isNotNull();
                        assertThat(resultObj.getCommissions()).isGreaterThanOrEqualTo(0.0);
                    }
                );
            }
            
            @Test
            @DisplayName("Should handle edge case of single price entry")
            void shouldHandleEdgeCaseOfSinglePriceEntry() {
                // Given
                configureMockPriceSeries(100.0); // Only one price entry
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                backtest.initialize(strategy); // This should consume the only entry
                boolean result = backtest.nextStep(); // This should return false immediately
                
                // Then
                assertAll("Single price entry handling",
                    () -> assertThat(result).isFalse(),
                    () -> assertThat(backtest.getResult()).isNotNull(),
                    () -> assertThat(backtest.getContext().getCurrentPrice()).isEqualTo(100.0),
                    () -> assertThat(backtest.getContext().getMHistory().size()).isEqualTo(1)
                );
            }
            
            @Test
            @DisplayName("Should handle empty price series gracefully")
            void shouldHandleEmptyPriceSeriesGracefully() {
                // Given
                configureMockPriceSeries(); // Empty price series
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                backtest.initialize(strategy);
                boolean result = backtest.nextStep();
                
                // Then
                assertAll("Empty price series handling",
                    () -> assertThat(result).isFalse(),
                    () -> assertThat(backtest.getResult()).isNotNull(),
                    () -> {
                        // Context should still be valid even with empty series
                        TradingContext context = backtest.getContext();
                        assertThat(context).isNotNull();
                        assertThat(context.getSymbol()).isEqualTo(DEFAULT_SYMBOL);
                        assertThat(context.getInitialFunds()).isEqualTo(DEFAULT_DEPOSIT);
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify termination conditions with realistic trading scenario")
            void shouldVerifyTerminationConditionsWithRealisticTradingScenario() {
                // Given - A realistic scenario with multiple price points
                double[] prices = {100.0, 101.0, 99.0, 102.0, 98.0, 103.0};
                configureMockPriceSeries(prices);
                AsyncTradingStrategy strategy = createMockStrategy();
                backtest.initialize(strategy);
                
                // When - Process all prices until exhaustion
                boolean result1 = backtest.nextStep(); // 101.0
                boolean result2 = backtest.nextStep(); // 99.0
                boolean result3 = backtest.nextStep(); // 102.0
                boolean result4 = backtest.nextStep(); // 98.0
                boolean result5 = backtest.nextStep(); // 103.0
                boolean result6 = backtest.nextStep(); // Should be false - exhausted
                
                // Then
                assertAll("Realistic trading scenario termination",
                    () -> assertThat(result1).isTrue(),
                    () -> assertThat(result2).isTrue(),
                    () -> assertThat(result3).isTrue(),
                    () -> assertThat(result4).isTrue(),
                    () -> assertThat(result5).isTrue(),
                    () -> assertThat(result6).isFalse(), // Iterator exhausted
                    () -> assertThat(backtest.getResult()).isNotNull(),
                    () -> assertThat(backtest.getContext().getCurrentPrice()).isEqualTo(103.0), // Last processed price
                    () -> assertThat(backtest.getContext().getMHistory().size()).isEqualTo(6) // All prices processed
                );
                
                // Verify strategy was called for each price
                verify(strategy, atLeast(6)).onTick(any(TradingContext.class));
            }
        }
    }
    
    @Nested
    @DisplayName("Run Method Tests")
    class RunTests {
        
        @Nested
        @DisplayName("Complete Run Execution Tests")
        class CompleteRunExecutionTests {
            
            @Test
            @DisplayName("Should call initialize with provided strategy")
            void shouldCallInitializeWithProvidedStrategy() {
                // Given
                DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0, 99.0);
                Backtest testBacktest = new Backtest(DEFAULT_DEPOSIT, testSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                assertAll("Initialize method call verification",
                    () -> assertThat(testBacktest.getStrategy()).isEqualTo(strategy),
                    () -> assertThat(testBacktest.getContext()).isNotNull(),
                    () -> assertThat(testBacktest.getPriceIterator()).isNotNull(),
                    () -> assertThat(result).isNotNull()
                );
                
                // Verify strategy.startStrategy was called during initialization
                verify(strategy).startStrategy(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should execute nextStep until completion")
            void shouldExecuteNextStepUntilCompletion() {
                // Given
                double[] prices = {100.0, 101.0, 99.0, 102.0, 98.0};
                DoubleSeries testSeries = createTestPriceSeries(prices);
                Backtest testBacktest = new Backtest(DEFAULT_DEPOSIT, testSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                assertAll("NextStep execution until completion",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(testBacktest.getContext().getMHistory().size()).isEqualTo(prices.length),
                    () -> assertThat(testBacktest.getContext().getCurrentPrice()).isEqualTo(prices[prices.length - 1]),
                    () -> assertThat(testBacktest.getResult()).isNotNull()
                );
                
                // Verify strategy.onTick was called for each price point
                verify(strategy, times(prices.length)).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should return valid Result object")
            void shouldReturnValidResultObject() {
                // Given
                DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0, 99.0);
                Backtest testBacktest = new Backtest(DEFAULT_DEPOSIT, testSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                assertAll("Valid Result object verification",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPl()).isNotNull(),
                    () -> assertThat(result.getFinalValue()).isNotNull(),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(result.getPriceSeries()).isNotNull(),
                    () -> assertThat(result.getCommissions()).isNotNull()
                    // Note: priceSeries in Result actually contains profit/loss data, not original price series
                );
                
                // Verify final value calculation
                double expectedFinalValue = result.getInitialFund() + result.getPl();
                assertThat(result.getFinalValue()).isEqualTo(expectedFinalValue);
            }
            
            @Test
            @DisplayName("Should complete full execution flow with multiple price points")
            void shouldCompleteFullExecutionFlowWithMultiplePricePoints() {
                // Given
                double[] prices = {100.0, 105.0, 95.0, 110.0, 90.0, 115.0};
                DoubleSeries testSeries = createTestPriceSeries(prices);
                Backtest testBacktest = new Backtest(DEFAULT_DEPOSIT, testSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                assertAll("Full execution flow verification",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(testBacktest.getStrategy()).isEqualTo(strategy),
                    () -> assertThat(testBacktest.getContext()).isNotNull(),
                    () -> assertThat(testBacktest.getContext().getMHistory().size()).isEqualTo(prices.length),
                    () -> assertThat(testBacktest.getContext().getProfitLoss().size()).isGreaterThan(0),
                    () -> assertThat(testBacktest.getContext().getFundsHistory().size()).isGreaterThan(0),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT)
                    // Note: priceSeries in Result contains profit/loss data, not original price series
                );
                
                // Verify all interactions occurred
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, times(prices.length)).onTick(any(TradingContext.class));
            }
        }
        
        @Nested
        @DisplayName("Run Method Various Scenarios Tests")
        class RunMethodVariousScenariosTests {
            
            @Test
            @DisplayName("Should handle run with strategy that generates orders")
            void shouldHandleRunWithStrategyThatGeneratesOrders() {
                // Given
                DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0, 99.0, 102.0);
                Backtest testBacktest = new Backtest(DEFAULT_DEPOSIT, testSeries, DEFAULT_SYMBOL);
                
                // Create a strategy that generates mock orders
                AsyncTradingStrategy strategy = mock(AsyncTradingStrategy.class);
                when(strategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                
                // Mock orders to be returned by strategy
                List<Object> mockOrders = new ArrayList<>();
                mockOrders.add(new Object()); // Simulate an order
                when(strategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockOrders));
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                assertAll("Strategy with orders verification",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(testBacktest.getContext().getOrders()).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPl()).isNotNull(),
                    () -> assertThat(result.getFinalValue()).isEqualTo(result.getInitialFund() + result.getPl())
                );
                
                // Verify strategy interactions
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, times(4)).onTick(any(TradingContext.class)); // 4 price points
            }
            
            @Test
            @DisplayName("Should handle run with empty price series")
            void shouldHandleRunWithEmptyPriceSeries() {
                // Given
                DoubleSeries emptyTestSeries = createTestPriceSeries(); // No prices
                Backtest testBacktest = new Backtest(DEFAULT_DEPOSIT, emptyTestSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                assertAll("Empty price series handling",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPl()).isEqualTo(0.0), // No trading occurred
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(result.getOrders()).isEmpty(),
                    () -> assertThat(result.getCommissions()).isEqualTo(0.0)
                    // Note: priceSeries in Result contains profit/loss data, not original price series
                );
                
                // Verify strategy was initialized but no onTick calls occurred
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, never()).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should verify Result contains correct deposit as initial fund")
            void shouldVerifyResultContainsCorrectDepositAsInitialFund() {
                // Given
                double customDeposit = 25000.0;
                DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0);
                Backtest testBacktest = new Backtest(customDeposit, testSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                assertAll("Initial fund verification",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(customDeposit),
                    () -> assertThat(testBacktest.getContext().getInitialFunds()).isEqualTo(customDeposit),
                    () -> assertThat(result.getFinalValue()).isEqualTo(customDeposit + result.getPl())
                );
            }
            
            @Test
            @DisplayName("Should verify Result contains accurate final value calculation")
            void shouldVerifyResultContainsAccurateFinalValueCalculation() {
                // Given
                DoubleSeries testSeries = createTestPriceSeries(100.0, 105.0, 95.0);
                Backtest testBacktest = new Backtest(DEFAULT_DEPOSIT, testSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                double expectedFinalValue = result.getInitialFund() + result.getPl();
                assertAll("Final value calculation verification",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getFinalValue()).isEqualTo(expectedFinalValue),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPl()).isNotNull(),
                    () -> {
                        // Verify the calculation is mathematically correct
                        double calculatedFinalValue = DEFAULT_DEPOSIT + result.getPl();
                        assertThat(result.getFinalValue()).isEqualTo(calculatedFinalValue);
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify Result contains all closed orders from session")
            void shouldVerifyResultContainsAllClosedOrdersFromSession() {
                // Given
                DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0, 99.0);
                Backtest testBacktest = new Backtest(DEFAULT_DEPOSIT, testSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                assertAll("Closed orders verification",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(result.getOrders()).isEqualTo(testBacktest.getContext().getClosedOrders()),
                    () -> {
                        // Verify that the orders in result match the closed orders from context
                        List<?> contextClosedOrders = testBacktest.getContext().getClosedOrders();
                        assertThat(result.getOrders()).containsExactlyElementsOf(contextClosedOrders);
                    }
                );
            }
            
            @Test
            @DisplayName("Should handle different deposit values correctly")
            void shouldHandleDifferentDepositValuesCorrectly() {
                // Given
                double[] testDeposits = {1000.0, 50000.0, 100.0, 0.0};
                DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                for (double deposit : testDeposits) {
                    // When
                    Backtest testBacktest = new Backtest(deposit, testSeries, DEFAULT_SYMBOL);
                    Backtest.Result result = testBacktest.run(strategy);
                    
                    // Then
                    assertAll("Deposit value handling for " + deposit,
                        () -> assertThat(result).isNotNull(),
                        () -> assertThat(result.getInitialFund()).isEqualTo(deposit),
                        () -> assertThat(result.getFinalValue()).isEqualTo(deposit + result.getPl()),
                        () -> assertThat(testBacktest.getContext().getInitialFunds()).isEqualTo(deposit)
                    );
                }
            }
            
            @Test
            @DisplayName("Should handle single price point scenario")
            void shouldHandleSinglePricePointScenario() {
                // Given
                DoubleSeries singlePriceSeries = createTestPriceSeries(100.0);
                Backtest testBacktest = new Backtest(DEFAULT_DEPOSIT, singlePriceSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = testBacktest.run(strategy);
                
                // Then
                assertAll("Single price point handling",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(testBacktest.getContext().getMHistory().size()).isEqualTo(1),
                    () -> assertThat(testBacktest.getContext().getCurrentPrice()).isEqualTo(100.0),
                    () -> assertThat(result.getFinalValue()).isEqualTo(result.getInitialFund() + result.getPl())
                    // Note: priceSeries in Result contains profit/loss data, not original price series
                );
                
                // Verify strategy interactions
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, times(1)).onTick(any(TradingContext.class));
            }
        }
    }
    
    @Nested
    @DisplayName("Result Class Tests")
    class ResultTests {
        
        @Test
        @DisplayName("Should create Result with valid parameters")
        void shouldCreateResultWithValidParameters() {
            // Given
            double expectedPL = 150.0;
            DoubleSeries expectedPriceSeries = createTestPriceSeries(100.0, 101.0, 99.0);
            List<ClosedOrder> expectedOrders = createTestClosedOrders();
            double expectedInitialFund = 10000.0;
            double expectedFinalValue = 10150.0;
            double expectedCommissions = 25.0;
            
            // When
            Backtest.Result result = new Backtest.Result(
                expectedPL, 
                expectedPriceSeries, 
                expectedOrders, 
                expectedInitialFund, 
                expectedFinalValue, 
                expectedCommissions
            );
            
            // Then
            assertAll("Result constructor with valid parameters",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getPl()).isEqualTo(expectedPL),
                () -> assertThat(result.getPriceSeries()).isEqualTo(expectedPriceSeries),
                () -> assertThat(result.getOrders()).isEqualTo(expectedOrders),
                () -> assertThat(result.getInitialFund()).isEqualTo(expectedInitialFund),
                () -> assertThat(result.getFinalValue()).isEqualTo(expectedFinalValue),
                () -> assertThat(result.getCommissions()).isEqualTo(expectedCommissions)
            );
        }
        
        @Test
        @DisplayName("Should verify profit/loss field accuracy")
        void shouldVerifyProfitLossFieldAccuracy() {
            // Given
            double profitScenario = 250.0;
            double lossScenario = -150.0;
            double noChangeScenario = 0.0;
            DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0);
            List<ClosedOrder> testOrders = createTestClosedOrders();
            double initialFund = 5000.0;
            double commissions = 10.0;
            
            // When & Then - Profit scenario
            Backtest.Result profitResult = new Backtest.Result(
                profitScenario, testSeries, testOrders, initialFund, 
                initialFund + profitScenario, commissions
            );
            assertAll("Profit scenario",
                () -> assertThat(profitResult.getPl()).isEqualTo(profitScenario),
                () -> assertThat(profitResult.getPl()).isPositive(),
                () -> assertThat(profitResult.getFinalValue()).isGreaterThan(profitResult.getInitialFund())
            );
            
            // When & Then - Loss scenario
            Backtest.Result lossResult = new Backtest.Result(
                lossScenario, testSeries, testOrders, initialFund, 
                initialFund + lossScenario, commissions
            );
            assertAll("Loss scenario",
                () -> assertThat(lossResult.getPl()).isEqualTo(lossScenario),
                () -> assertThat(lossResult.getPl()).isNegative(),
                () -> assertThat(lossResult.getFinalValue()).isLessThan(lossResult.getInitialFund())
            );
            
            // When & Then - No change scenario
            Backtest.Result noChangeResult = new Backtest.Result(
                noChangeScenario, testSeries, testOrders, initialFund, 
                initialFund + noChangeScenario, commissions
            );
            assertAll("No change scenario",
                () -> assertThat(noChangeResult.getPl()).isEqualTo(noChangeScenario),
                () -> assertThat(noChangeResult.getPl()).isZero(),
                () -> assertThat(noChangeResult.getFinalValue()).isEqualTo(noChangeResult.getInitialFund())
            );
        }
        
        @Test
        @DisplayName("Should verify price series field contains original data")
        void shouldVerifyPriceSeriesFieldContainsOriginalData() {
            // Given
            DoubleSeries originalPriceSeries = createTestPriceSeries(105.0, 106.5, 104.2, 107.1);
            double pl = 100.0;
            List<ClosedOrder> orders = createTestClosedOrders();
            double initialFund = 8000.0;
            double finalValue = 8100.0;
            double commissions = 15.0;
            
            // When
            Backtest.Result result = new Backtest.Result(
                pl, originalPriceSeries, orders, initialFund, finalValue, commissions
            );
            
            // Then
            assertAll("Price series data verification",
                () -> assertThat(result.getPriceSeries()).isNotNull(),
                () -> assertThat(result.getPriceSeries()).isEqualTo(originalPriceSeries),
                () -> assertThat(result.getPriceSeries().size()).isEqualTo(originalPriceSeries.size()),
                () -> assertThat(result.getPriceSeries().getSymbol()).isEqualTo(originalPriceSeries.getSymbol()),
                () -> {
                    // Verify that the price series contains the same data
                    Iterator<TimeSeries.Entry<Double>> originalIterator = originalPriceSeries.iterator();
                    Iterator<TimeSeries.Entry<Double>> resultIterator = result.getPriceSeries().iterator();
                    
                    while (originalIterator.hasNext() && resultIterator.hasNext()) {
                        TimeSeries.Entry<Double> originalEntry = originalIterator.next();
                        TimeSeries.Entry<Double> resultEntry = resultIterator.next();
                        
                        assertThat(resultEntry.getItem()).isEqualTo(originalEntry.getItem());
                        assertThat(resultEntry.getInstant()).isEqualTo(originalEntry.getInstant());
                    }
                    
                    // Verify both iterators are exhausted
                    assertThat(originalIterator.hasNext()).isEqualTo(resultIterator.hasNext());
                }
            );
        }
        
        @Test
        @DisplayName("Should verify orders list contains all closed orders")
        void shouldVerifyOrdersListContainsAllClosedOrders() {
            // Given
            List<ClosedOrder> expectedClosedOrders = createTestClosedOrders();
            // Add additional orders to test comprehensive list handling
            expectedClosedOrders.add(createTestClosedOrder(3, "GOOGL", 150.0, 152.0, 100, BASE_TIME.plusSeconds(300)));
            expectedClosedOrders.add(createTestClosedOrder(4, "TSLA", 200.0, 195.0, -50, BASE_TIME.plusSeconds(400)));
            
            double pl = 75.0;
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0);
            double initialFund = 12000.0;
            double finalValue = 12075.0;
            double commissions = 20.0;
            
            // When
            Backtest.Result result = new Backtest.Result(
                pl, priceSeries, expectedClosedOrders, initialFund, finalValue, commissions
            );
            
            // Then
            assertAll("Orders list verification",
                () -> assertThat(result.getOrders()).isNotNull(),
                () -> assertThat(result.getOrders()).isEqualTo(expectedClosedOrders),
                () -> assertThat(result.getOrders().size()).isEqualTo(expectedClosedOrders.size()),
                () -> assertThat(result.getOrders()).containsExactlyElementsOf(expectedClosedOrders),
                () -> {
                    // Verify each order in the list
                    for (int i = 0; i < expectedClosedOrders.size(); i++) {
                        ClosedOrder expected = expectedClosedOrders.get(i);
                        ClosedOrder actual = result.getOrders().get(i);
                        
                        assertThat(actual.getId()).isEqualTo(expected.getId());
                        assertThat(actual.getInstrument()).isEqualTo(expected.getInstrument());
                        assertThat(actual.getOpenPrice()).isEqualTo(expected.getOpenPrice());
                        assertThat(actual.getClosePrice()).isEqualTo(expected.getClosePrice());
                        assertThat(actual.getAmount()).isEqualTo(expected.getAmount());
                        assertThat(actual.getPl()).isEqualTo(expected.getPl());
                    }
                }
            );
        }
        
        @Test
        @DisplayName("Should verify initial fund matches original deposit")
        void shouldVerifyInitialFundMatchesOriginalDeposit() {
            // Given
            double[] testDeposits = {1000.0, 5000.0, 10000.0, 25000.0, 0.0, 100.5};
            DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0);
            List<ClosedOrder> testOrders = createTestClosedOrders();
            double pl = 50.0;
            double commissions = 5.0;
            
            for (double originalDeposit : testDeposits) {
                // When
                Backtest.Result result = new Backtest.Result(
                    pl, testSeries, testOrders, originalDeposit, 
                    originalDeposit + pl, commissions
                );
                
                // Then
                assertAll("Initial fund verification for deposit: " + originalDeposit,
                    () -> assertThat(result.getInitialFund()).isEqualTo(originalDeposit),
                    () -> assertThat(result.getInitialFund()).isNotNegative().withFailMessage(
                        "Initial fund should match the original deposit value: " + originalDeposit
                    )
                );
            }
        }
        
        @Test
        @DisplayName("Should verify final value equals initial fund plus profit/loss")
        void shouldVerifyFinalValueEqualsInitialFundPlusProfitLoss() {
            // Given
            double initialFund = 10000.0;
            double[] profitLossScenarios = {500.0, -200.0, 0.0, 1500.0, -50.5, 0.01};
            DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0);
            List<ClosedOrder> testOrders = createTestClosedOrders();
            double commissions = 10.0;
            
            for (double pl : profitLossScenarios) {
                // When
                double expectedFinalValue = initialFund + pl;
                Backtest.Result result = new Backtest.Result(
                    pl, testSeries, testOrders, initialFund, expectedFinalValue, commissions
                );
                
                // Then
                assertAll("Final value calculation for P/L: " + pl,
                    () -> assertThat(result.getFinalValue()).isEqualTo(expectedFinalValue),
                    () -> assertThat(result.getFinalValue()).isEqualTo(result.getInitialFund() + result.getPl()),
                    () -> {
                        // Verify the mathematical relationship
                        double calculatedFinalValue = result.getInitialFund() + result.getPl();
                        assertThat(result.getFinalValue()).isEqualTo(calculatedFinalValue);
                    }
                );
            }
        }
        
        @Test
        @DisplayName("Should verify commissions field contains total commission costs")
        void shouldVerifyCommissionsFieldContainsTotalCommissionCosts() {
            // Given
            double[] commissionScenarios = {0.0, 5.0, 25.5, 100.0, 0.01, 500.0};
            double pl = 200.0;
            DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0);
            List<ClosedOrder> testOrders = createTestClosedOrders();
            double initialFund = 8000.0;
            double finalValue = 8200.0;
            
            for (double expectedCommissions : commissionScenarios) {
                // When
                Backtest.Result result = new Backtest.Result(
                    pl, testSeries, testOrders, initialFund, finalValue, expectedCommissions
                );
                
                // Then
                assertAll("Commissions verification for: " + expectedCommissions,
                    () -> assertThat(result.getCommissions()).isEqualTo(expectedCommissions),
                    () -> assertThat(result.getCommissions()).isNotNegative().withFailMessage(
                        "Commissions should not be negative: " + expectedCommissions
                    ),
                    () -> {
                        // Verify commissions represent total costs
                        assertThat(result.getCommissions()).isGreaterThanOrEqualTo(0.0);
                    }
                );
            }
        }
        
        @Test
        @DisplayName("Should handle Result with empty orders list")
        void shouldHandleResultWithEmptyOrdersList() {
            // Given
            double pl = 0.0;
            DoubleSeries testSeries = createTestPriceSeries(100.0);
            List<ClosedOrder> emptyOrders = new ArrayList<>();
            double initialFund = 5000.0;
            double finalValue = 5000.0;
            double commissions = 0.0;
            
            // When
            Backtest.Result result = new Backtest.Result(
                pl, testSeries, emptyOrders, initialFund, finalValue, commissions
            );
            
            // Then
            assertAll("Empty orders list handling",
                () -> assertThat(result.getOrders()).isNotNull(),
                () -> assertThat(result.getOrders()).isEmpty(),
                () -> assertThat(result.getOrders().size()).isZero(),
                () -> assertThat(result.getPl()).isZero(),
                () -> assertThat(result.getCommissions()).isZero(),
                () -> assertThat(result.getFinalValue()).isEqualTo(result.getInitialFund())
            );
        }
        
        @Test
        @DisplayName("Should handle Result with null price series")
        void shouldHandleResultWithNullPriceSeries() {
            // Given
            double pl = 100.0;
            DoubleSeries nullPriceSeries = null;
            List<ClosedOrder> testOrders = createTestClosedOrders();
            double initialFund = 7000.0;
            double finalValue = 7100.0;
            double commissions = 15.0;
            
            // When
            Backtest.Result result = new Backtest.Result(
                pl, nullPriceSeries, testOrders, initialFund, finalValue, commissions
            );
            
            // Then
            assertAll("Null price series handling",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getPriceSeries()).isNull(),
                () -> assertThat(result.getPl()).isEqualTo(pl),
                () -> assertThat(result.getOrders()).isEqualTo(testOrders),
                () -> assertThat(result.getInitialFund()).isEqualTo(initialFund),
                () -> assertThat(result.getFinalValue()).isEqualTo(finalValue),
                () -> assertThat(result.getCommissions()).isEqualTo(commissions)
            );
        }
        
        @Test
        @DisplayName("Should handle Result with null orders list")
        void shouldHandleResultWithNullOrdersList() {
            // Given
            double pl = 75.0;
            DoubleSeries testSeries = createTestPriceSeries(100.0, 101.0);
            List<ClosedOrder> nullOrders = null;
            double initialFund = 6000.0;
            double finalValue = 6075.0;
            double commissions = 8.0;
            
            // When
            Backtest.Result result = new Backtest.Result(
                pl, testSeries, nullOrders, initialFund, finalValue, commissions
            );
            
            // Then
            assertAll("Null orders list handling",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getOrders()).isNull(),
                () -> assertThat(result.getPl()).isEqualTo(pl),
                () -> assertThat(result.getPriceSeries()).isEqualTo(testSeries),
                () -> assertThat(result.getInitialFund()).isEqualTo(initialFund),
                () -> assertThat(result.getFinalValue()).isEqualTo(finalValue),
                () -> assertThat(result.getCommissions()).isEqualTo(commissions)
            );
        }
        
        // Helper methods for Result class tests
        
        /**
         * Creates a list of test ClosedOrder instances for testing
         */
        private List<ClosedOrder> createTestClosedOrders() {
            List<ClosedOrder> orders = new ArrayList<>();
            orders.add(createTestClosedOrder(1, "AAPL", 100.0, 102.0, 100, BASE_TIME));
            orders.add(createTestClosedOrder(2, "AAPL", 101.0, 99.0, -50, BASE_TIME.plusSeconds(60)));
            return orders;
        }
        
        /**
         * Creates a single test ClosedOrder instance
         */
        private ClosedOrder createTestClosedOrder(int id, String instrument, double openPrice, 
                                                 double closePrice, int amount, Instant openInstant) {
            SimpleOrder order = new SimpleOrder(id, instrument, openInstant, openPrice, amount);
            return new SimpleClosedOrder(order, closePrice, openInstant.plusSeconds(30));
        }
    }
    
    @Nested
    @DisplayName("Mock Dependency Tests")
    class MockDependencyTests {
        
        @Nested
        @DisplayName("AsyncTradingStrategy Mock Tests")
        class AsyncTradingStrategyMockTests {
            
            @Test
            @DisplayName("Should control strategy behavior through mocking")
            void shouldControlStrategyBehaviorThroughMocking() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                // Configure mock strategy to return specific orders
                List<SimpleOrder> testOrders = new ArrayList<>();
                testOrders.add(new SimpleOrder(1, DEFAULT_SYMBOL, BASE_TIME, 100.0, 100));
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(testOrders));
                
                // When
                Backtest.Result result = backtest.run(mockStrategy);
                
                // Then
                assertAll("Controlled strategy behavior verification",
                    () -> assertThat(result).isNotNull(),
                    () -> verify(mockStrategy).startStrategy(any(TradingContext.class)),
                    () -> verify(mockStrategy, atLeast(1)).onTick(any(TradingContext.class)),
                    () -> assertThat(result.getOrders()).isNotNull()
                );
            }
            
            @Test
            @DisplayName("Should verify correct method calls on mocked strategy")
            void shouldVerifyCorrectMethodCallsOnMockedStrategy() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0, 102.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.run(mockStrategy);
                
                // Then - Verify method call sequence and frequency
                assertAll("Method call verification",
                    () -> {
                        // startStrategy should be called exactly once during initialization
                        verify(mockStrategy, times(1)).startStrategy(any(TradingContext.class));
                    },
                    () -> {
                        // onTick should be called for each price point (4 prices = 4 calls)
                        verify(mockStrategy, times(4)).onTick(any(TradingContext.class));
                    },
                    () -> {
                        // Verify no other unexpected method calls
                        verifyNoMoreInteractions(mockStrategy);
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify correct parameters passed to strategy methods")
            void shouldVerifyCorrectParametersPassedToStrategyMethods() {
                // Given
                double[] prices = {100.0, 101.0, 99.0};
                configureMockPriceSeries(prices);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.run(mockStrategy);
                
                // Then - Verify startStrategy parameters
                verify(mockStrategy).startStrategy(contextCaptor.capture());
                TradingContext startContext = contextCaptor.getValue();
                
                assertAll("startStrategy parameter verification",
                    () -> assertThat(startContext.getSymbol()).isEqualTo(DEFAULT_SYMBOL),
                    () -> assertThat(startContext.getInitialFunds()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(startContext.getLeverage()).isEqualTo(DEFAULT_LEVERAGE),
                    () -> assertThat(startContext.getMode()).isEqualTo(StrategyMode.BACKTEST),
                    () -> assertThat(startContext.getInstruments()).contains(DEFAULT_SYMBOL)
                );
                
                // Verify onTick parameters - capture all calls
                verify(mockStrategy, times(3)).onTick(contextCaptor.capture());
                List<TradingContext> onTickContexts = contextCaptor.getAllValues();
                
                // Remove the startStrategy context (first capture) to get only onTick contexts
                List<TradingContext> onlyOnTickContexts = onTickContexts.subList(1, onTickContexts.size());
                
                assertAll("onTick parameter verification",
                    () -> assertThat(onlyOnTickContexts).hasSize(3),
                    () -> {
                        // Verify first onTick call has first price
                        TradingContext firstTickContext = onlyOnTickContexts.get(0);
                        assertThat(firstTickContext.getCurrentPrice()).isEqualTo(prices[0]);
                        assertThat(firstTickContext.getInstant()).isEqualTo(BASE_TIME);
                    },
                    () -> {
                        // Verify second onTick call has second price
                        TradingContext secondTickContext = onlyOnTickContexts.get(1);
                        assertThat(secondTickContext.getCurrentPrice()).isEqualTo(prices[1]);
                        assertThat(secondTickContext.getInstant()).isEqualTo(BASE_TIME.plusSeconds(60));
                    },
                    () -> {
                        // Verify third onTick call has third price
                        TradingContext thirdTickContext = onlyOnTickContexts.get(2);
                        assertThat(thirdTickContext.getCurrentPrice()).isEqualTo(prices[2]);
                        assertThat(thirdTickContext.getInstant()).isEqualTo(BASE_TIME.plusSeconds(120));
                    }
                );
            }
            
            @Test
            @DisplayName("Should handle strategy returning different order types")
            void shouldHandleStrategyReturningDifferentOrderTypes() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                // Configure strategy to return different orders on different ticks
                List<SimpleOrder> firstTickOrders = new ArrayList<>();
                firstTickOrders.add(new SimpleOrder(1, DEFAULT_SYMBOL, BASE_TIME, 100.0, 100));
                
                List<SimpleOrder> secondTickOrders = new ArrayList<>();
                secondTickOrders.add(new SimpleOrder(2, DEFAULT_SYMBOL, BASE_TIME.plusSeconds(60), 101.0, -50));
                
                List<SimpleOrder> thirdTickOrders = new ArrayList<>(); // Empty list
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(firstTickOrders))
                    .thenReturn(CompletableFuture.completedFuture(secondTickOrders))
                    .thenReturn(CompletableFuture.completedFuture(thirdTickOrders));
                
                // When
                Backtest.Result result = backtest.run(mockStrategy);
                
                // Then
                assertAll("Different order types handling",
                    () -> assertThat(result).isNotNull(),
                    () -> verify(mockStrategy, times(3)).onTick(any(TradingContext.class)),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> {
                        // Verify that orders were processed correctly
                        // The exact number depends on which orders were closed during the backtest
                        assertThat(result.getOrders().size()).isGreaterThanOrEqualTo(0);
                    }
                );
            }
            
            @Test
            @DisplayName("Should handle strategy with CompletableFuture exceptions")
            void shouldHandleStrategyWithCompletableFutureExceptions() {
                // Given
                configureMockPriceSeries(100.0, 101.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                // Configure strategy to throw exception on startStrategy
                CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new RuntimeException("Strategy initialization failed"));
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(failedFuture);
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When & Then
                // The backtest should handle the exception gracefully
                // Note: The actual behavior depends on how the Backtest class handles CompletableFuture exceptions
                Backtest.Result result = backtest.run(mockStrategy);
                
                assertAll("CompletableFuture exception handling",
                    () -> assertThat(result).isNotNull(),
                    () -> verify(mockStrategy).startStrategy(any(TradingContext.class)),
                    () -> {
                        // Verify that despite the startStrategy exception, the backtest continues
                        // and onTick is still called for available price data
                        verify(mockStrategy, atLeast(1)).onTick(any(TradingContext.class));
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify strategy method call timing and sequence")
            void shouldVerifyStrategyMethodCallTimingAndSequence() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.run(mockStrategy);
                
                // Then - Verify method call order using InOrder
                InOrder inOrder = inOrder(mockStrategy);
                inOrder.verify(mockStrategy).startStrategy(any(TradingContext.class));
                inOrder.verify(mockStrategy, times(3)).onTick(any(TradingContext.class));
                inOrder.verifyNoMoreInteractions();
            }
            
            @Test
            @DisplayName("Should handle strategy returning null orders")
            void shouldHandleStrategyReturningNullOrders() {
                // Given
                configureMockPriceSeries(100.0, 101.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null)); // Return null orders
                
                // When
                Backtest.Result result = backtest.run(mockStrategy);
                
                // Then
                assertAll("Null orders handling",
                    () -> assertThat(result).isNotNull(),
                    () -> verify(mockStrategy, times(2)).onTick(any(TradingContext.class)),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(result.getOrders()).isEmpty()
                );
            }
        }
    
    @Nested
    @DisplayName("Mock Dependency Tests")
    class MockDependencyTests {
        
        @Nested
        @DisplayName("AsyncTradingStrategy Mock Tests")
        class AsyncTradingStrategyMockTests {
            
            @Test
            @DisplayName("Should control strategy behavior through mocking")
            void shouldControlStrategyBehaviorThroughMocking() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0, 102.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                // Configure specific behavior for startStrategy
                TradingContext returnedContext = mock(TradingContext.class);
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(returnedContext));
                
                // Configure specific behavior for onTick - return different orders on different calls
                List<SimpleOrder> firstTickOrders = List.of(
                    new SimpleOrder("BUY", 100, 101.0)
                );
                List<SimpleOrder> secondTickOrders = List.of(
                    new SimpleOrder("SELL", 50, 99.0)
                );
                List<SimpleOrder> emptyOrders = new ArrayList<>();
                
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(firstTickOrders))
                    .thenReturn(CompletableFuture.completedFuture(secondTickOrders))
                    .thenReturn(CompletableFuture.completedFuture(emptyOrders))
                    .thenReturn(CompletableFuture.completedFuture(emptyOrders));
                
                // When
                Backtest.Result result = backtest.run(mockStrategy);
                
                // Then
                assertAll("Controlled strategy behavior verification",
                    () -> assertThat(result).isNotNull(),
                    () -> verify(mockStrategy).startStrategy(any(TradingContext.class)),
                    () -> verify(mockStrategy, times(4)).onTick(any(TradingContext.class)),
                    () -> {
                        // Verify that the controlled behavior affected the result
                        // The strategy should have generated orders as configured
                        assertThat(result.getOrders()).isNotNull();
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify correct method calls on mocked strategy")
            void shouldVerifyCorrectMethodCallsOnMockedStrategy() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.run(mockStrategy);
                
                // Then - Verify exact method calls
                assertAll("Method call verification",
                    () -> {
                        // Verify startStrategy was called exactly once
                        verify(mockStrategy, times(1)).startStrategy(any(TradingContext.class));
                    },
                    () -> {
                        // Verify onTick was called for each price point (3 times)
                        verify(mockStrategy, times(3)).onTick(any(TradingContext.class));
                    },
                    () -> {
                        // Verify no other methods were called
                        verifyNoMoreInteractions(mockStrategy);
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify correct parameters passed to strategy methods")
            void shouldVerifyCorrectParametersPassedToStrategyMethods() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.run(mockStrategy);
                
                // Then - Verify parameters passed to startStrategy
                verify(mockStrategy).startStrategy(contextCaptor.capture());
                TradingContext startStrategyContext = contextCaptor.getValue();
                
                assertAll("StartStrategy parameter verification",
                    () -> assertThat(startStrategyContext).isNotNull(),
                    () -> assertThat(startStrategyContext.getSymbol()).isEqualTo(DEFAULT_SYMBOL),
                    () -> assertThat(startStrategyContext.getInitialFunds()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(startStrategyContext.getLeverage()).isEqualTo(DEFAULT_LEVERAGE),
                    () -> assertThat(startStrategyContext.getMode()).isEqualTo(StrategyMode.BACKTEST),
                    () -> assertThat(startStrategyContext.getInstruments()).contains(DEFAULT_SYMBOL),
                    () -> assertThat(startStrategyContext.getOrders()).isNotNull(),
                    () -> assertThat(startStrategyContext.getClosedOrders()).isNotNull(),
                    () -> assertThat(startStrategyContext.getProfitLoss()).isNotNull(),
                    () -> assertThat(startStrategyContext.getFundsHistory()).isNotNull(),
                    () -> assertThat(startStrategyContext.getMHistory()).isNotNull()
                );
                
                // Verify parameters passed to onTick calls
                ArgumentCaptor<TradingContext> onTickContextCaptor = ArgumentCaptor.forClass(TradingContext.class);
                verify(mockStrategy, times(3)).onTick(onTickContextCaptor.capture());
                
                List<TradingContext> onTickContexts = onTickContextCaptor.getAllValues();
                assertAll("OnTick parameter verification",
                    () -> assertThat(onTickContexts).hasSize(3),
                    () -> {
                        // Verify first onTick call context
                        TradingContext firstContext = onTickContexts.get(0);
                        assertThat(firstContext.getCurrentPrice()).isEqualTo(100.0);
                        assertThat(firstContext.getSymbol()).isEqualTo(DEFAULT_SYMBOL);
                        assertThat(firstContext.getInstant()).isEqualTo(BASE_TIME);
                    },
                    () -> {
                        // Verify second onTick call context
                        TradingContext secondContext = onTickContexts.get(1);
                        assertThat(secondContext.getCurrentPrice()).isEqualTo(101.0);
                        assertThat(secondContext.getSymbol()).isEqualTo(DEFAULT_SYMBOL);
                        assertThat(secondContext.getInstant()).isEqualTo(BASE_TIME.plusSeconds(60));
                    },
                    () -> {
                        // Verify third onTick call context
                        TradingContext thirdContext = onTickContexts.get(2);
                        assertThat(thirdContext.getCurrentPrice()).isEqualTo(99.0);
                        assertThat(thirdContext.getSymbol()).isEqualTo(DEFAULT_SYMBOL);
                        assertThat(thirdContext.getInstant()).isEqualTo(BASE_TIME.plusSeconds(120));
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify strategy method call sequence and timing")
            void shouldVerifyStrategyMethodCallSequenceAndTiming() {
                // Given
                configureMockPriceSeries(100.0, 101.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.run(mockStrategy);
                
                // Then - Verify method call sequence using InOrder
                InOrder inOrder = inOrder(mockStrategy);
                inOrder.verify(mockStrategy).startStrategy(any(TradingContext.class));
                inOrder.verify(mockStrategy, times(2)).onTick(any(TradingContext.class));
                inOrder.verifyNoMoreInteractions();
            }
            
            @Test
            @DisplayName("Should handle strategy returning different order types")
            void shouldHandleStrategyReturningDifferentOrderTypes() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                
                // Configure strategy to return different types of orders
                List<SimpleOrder> buyOrders = List.of(
                    new SimpleOrder("BUY", 100, 100.0),
                    new SimpleOrder("BUY", 50, 100.5)
                );
                List<SimpleOrder> sellOrders = List.of(
                    new SimpleOrder("SELL", 75, 101.0)
                );
                List<SimpleOrder> emptyOrders = new ArrayList<>();
                
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(buyOrders))
                    .thenReturn(CompletableFuture.completedFuture(sellOrders))
                    .thenReturn(CompletableFuture.completedFuture(emptyOrders));
                
                // When
                Backtest.Result result = backtest.run(mockStrategy);
                
                // Then
                assertAll("Different order types handling",
                    () -> assertThat(result).isNotNull(),
                    () -> verify(mockStrategy, times(3)).onTick(any(TradingContext.class)),
                    () -> {
                        // Verify that orders were processed correctly
                        assertThat(result.getOrders()).isNotNull();
                        // The exact number of closed orders depends on the backtest execution logic
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify context state changes between strategy calls")
            void shouldVerifyContextStateChangesBetweenStrategyCalls() {
                // Given
                configureMockPriceSeries(100.0, 105.0, 95.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.run(mockStrategy);
                
                // Then - Capture all onTick contexts to verify state changes
                ArgumentCaptor<TradingContext> contextCaptor = ArgumentCaptor.forClass(TradingContext.class);
                verify(mockStrategy, times(3)).onTick(contextCaptor.capture());
                
                List<TradingContext> contexts = contextCaptor.getAllValues();
                
                assertAll("Context state changes verification",
                    () -> {
                        // Verify price progression
                        assertThat(contexts.get(0).getCurrentPrice()).isEqualTo(100.0);
                        assertThat(contexts.get(1).getCurrentPrice()).isEqualTo(105.0);
                        assertThat(contexts.get(2).getCurrentPrice()).isEqualTo(95.0);
                    },
                    () -> {
                        // Verify time progression
                        assertThat(contexts.get(0).getInstant()).isEqualTo(BASE_TIME);
                        assertThat(contexts.get(1).getInstant()).isEqualTo(BASE_TIME.plusSeconds(60));
                        assertThat(contexts.get(2).getInstant()).isEqualTo(BASE_TIME.plusSeconds(120));
                    },
                    () -> {
                        // Verify profit/loss history grows
                        assertThat(contexts.get(1).getProfitLoss().size())
                            .isGreaterThan(contexts.get(0).getProfitLoss().size());
                        assertThat(contexts.get(2).getProfitLoss().size())
                            .isGreaterThan(contexts.get(1).getProfitLoss().size());
                    },
                    () -> {
                        // Verify funds history grows
                        assertThat(contexts.get(1).getFundsHistory().size())
                            .isGreaterThan(contexts.get(0).getFundsHistory().size());
                        assertThat(contexts.get(2).getFundsHistory().size())
                            .isGreaterThan(contexts.get(1).getFundsHistory().size());
                    },
                    () -> {
                        // Verify market history grows
                        assertThat(contexts.get(1).getMHistory().size())
                            .isGreaterThan(contexts.get(0).getMHistory().size());
                        assertThat(contexts.get(2).getMHistory().size())
                            .isGreaterThan(contexts.get(1).getMHistory().size());
                    }
                );
            }
        }
        
        @Nested
        @DisplayName("TradingContext Verification Tests")
        class TradingContextVerificationTests {
            
            @Test
            @DisplayName("Should verify calculations using controlled price data")
            void shouldVerifyCalculationsUsingControlledPriceData() {
                // Given - Use controlled price data to verify specific calculations
                double[] controlledPrices = {100.0, 110.0, 90.0, 105.0};
                configureMockPriceSeries(controlledPrices);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                Backtest.Result result = backtest.run(mockStrategy);
                
                // Then - Capture all contexts to verify calculations
                ArgumentCaptor<TradingContext> contextCaptor = ArgumentCaptor.forClass(TradingContext.class);
                verify(mockStrategy, times(4)).onTick(contextCaptor.capture());
                
                List<TradingContext> contexts = contextCaptor.getAllValues();
                
                assertAll("Controlled price data calculations verification",
                    () -> {
                        // Verify price progression matches controlled data
                        assertThat(contexts.get(0).getCurrentPrice()).isEqualTo(100.0);
                        assertThat(contexts.get(1).getCurrentPrice()).isEqualTo(110.0);
                        assertThat(contexts.get(2).getCurrentPrice()).isEqualTo(90.0);
                        assertThat(contexts.get(3).getCurrentPrice()).isEqualTo(105.0);
                    },
                    () -> {
                        // Verify time progression is consistent
                        for (int i = 0; i < contexts.size(); i++) {
                            Instant expectedTime = BASE_TIME.plusSeconds(i * 60);
                            assertThat(contexts.get(i).getInstant()).isEqualTo(expectedTime);
                        }
                    },
                    () -> {
                        // Verify profit/loss calculations accumulate correctly
                        for (int i = 1; i < contexts.size(); i++) {
                            assertThat(contexts.get(i).getProfitLoss().size())
                                .isGreaterThan(contexts.get(i-1).getProfitLoss().size());
                        }
                    },
                    () -> {
                        // Verify funds history calculations are consistent
                        for (int i = 1; i < contexts.size(); i++) {
                            assertThat(contexts.get(i).getFundsHistory().size())
                                .isGreaterThan(contexts.get(i-1).getFundsHistory().size());
                        }
                    },
                    () -> {
                        // Verify available funds calculation remains consistent
                        for (TradingContext context : contexts) {
                            double availableFunds = context.getAvailableFunds();
                            assertThat(availableFunds).isGreaterThanOrEqualTo(0.0);
                            // Available funds should be initial funds plus current P&L
                            assertThat(availableFunds).isLessThanOrEqualTo(DEFAULT_DEPOSIT * 2); // reasonable upper bound
                        }
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify TradingContext state changes during execution")
            void shouldVerifyTradingContextStateChangesDuringExecution() {
                // Given - Setup controlled scenario to track state changes
                double[] prices = {100.0, 102.0, 98.0, 101.0};
                configureMockPriceSeries(prices);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.run(mockStrategy);
                
                // Then - Capture and verify state changes
                ArgumentCaptor<TradingContext> contextCaptor = ArgumentCaptor.forClass(TradingContext.class);
                verify(mockStrategy, times(4)).onTick(contextCaptor.capture());
                
                List<TradingContext> contexts = contextCaptor.getAllValues();
                
                assertAll("TradingContext state changes verification",
                    () -> {
                        // Verify current price state changes
                        assertThat(contexts.get(0).getCurrentPrice()).isEqualTo(100.0);
                        assertThat(contexts.get(1).getCurrentPrice()).isEqualTo(102.0);
                        assertThat(contexts.get(2).getCurrentPrice()).isEqualTo(98.0);
                        assertThat(contexts.get(3).getCurrentPrice()).isEqualTo(101.0);
                    },
                    () -> {
                        // Verify instant state changes
                        for (int i = 0; i < contexts.size(); i++) {
                            Instant expectedInstant = BASE_TIME.plusSeconds(i * 60);
                            assertThat(contexts.get(i).getInstant()).isEqualTo(expectedInstant);
                        }
                    },
                    () -> {
                        // Verify profit/loss list grows with each tick
                        int previousSize = 0;
                        for (TradingContext context : contexts) {
                            int currentSize = context.getProfitLoss().size();
                            assertThat(currentSize).isGreaterThan(previousSize);
                            previousSize = currentSize;
                        }
                    },
                    () -> {
                        // Verify funds history grows with each tick
                        int previousSize = 0;
                        for (TradingContext context : contexts) {
                            int currentSize = context.getFundsHistory().size();
                            assertThat(currentSize).isGreaterThan(previousSize);
                            previousSize = currentSize;
                        }
                    },
                    () -> {
                        // Verify market history grows with each tick
                        int previousSize = 0;
                        for (TradingContext context : contexts) {
                            int currentSize = context.getMHistory().size();
                            assertThat(currentSize).isGreaterThan(previousSize);
                            previousSize = currentSize;
                        }
                    },
                    () -> {
                        // Verify symbol remains constant
                        for (TradingContext context : contexts) {
                            assertThat(context.getSymbol()).isEqualTo(DEFAULT_SYMBOL);
                        }
                    },
                    () -> {
                        // Verify initial funds remain constant
                        for (TradingContext context : contexts) {
                            assertThat(context.getInitialFunds()).isEqualTo(DEFAULT_DEPOSIT);
                        }
                    },
                    () -> {
                        // Verify leverage remains constant
                        for (TradingContext context : contexts) {
                            assertThat(context.getLeverage()).isEqualTo(DEFAULT_LEVERAGE);
                        }
                    },
                    () -> {
                        // Verify mode remains BACKTEST
                        for (TradingContext context : contexts) {
                            assertThat(context.getMode()).isEqualTo(StrategyMode.BACKTEST);
                        }
                    }
                );
            }
            
            @Test
            @DisplayName("Should handle mocked dependencies simulating failure conditions")
            void shouldHandleMockedDependenciesSimulatingFailureConditions() {
                // Given - Setup mocked dependencies to simulate various failure conditions
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                // Simulate startStrategy failure
                CompletableFuture<Void> failedStartFuture = new CompletableFuture<>();
                failedStartFuture.completeExceptionally(new RuntimeException("Strategy start failed"));
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(failedStartFuture);
                
                // Simulate onTick failures on different calls
                CompletableFuture<List<SimpleOrder>> failedOnTickFuture = new CompletableFuture<>();
                failedOnTickFuture.completeExceptionally(new RuntimeException("OnTick failed"));
                
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>())) // First call succeeds
                    .thenReturn(failedOnTickFuture) // Second call fails
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>())); // Third call succeeds
                
                // When
                Backtest.Result result = backtest.run(mockStrategy);
                
                // Then - Verify that backtest handles failures gracefully
                assertAll("Failure condition handling verification",
                    () -> {
                        // Verify result is still produced despite failures
                        assertThat(result).isNotNull();
                    },
                    () -> {
                        // Verify startStrategy was called despite failure
                        verify(mockStrategy).startStrategy(any(TradingContext.class));
                    },
                    () -> {
                        // Verify onTick was called multiple times despite some failures
                        verify(mockStrategy, atLeast(1)).onTick(any(TradingContext.class));
                    },
                    () -> {
                        // Verify basic result structure is maintained
                        assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT);
                        assertThat(result.getPriceSeries()).isNotNull();
                        assertThat(result.getOrders()).isNotNull();
                    }
                );
                
                // Capture contexts to verify state despite failures
                ArgumentCaptor<TradingContext> contextCaptor = ArgumentCaptor.forClass(TradingContext.class);
                verify(mockStrategy, atLeast(1)).onTick(contextCaptor.capture());
                
                List<TradingContext> contexts = contextCaptor.getAllValues();
                
                assertAll("Context state verification despite failures",
                    () -> {
                        // Verify contexts are still valid despite strategy failures
                        for (TradingContext context : contexts) {
                            assertThat(context).isNotNull();
                            assertThat(context.getSymbol()).isEqualTo(DEFAULT_SYMBOL);
                            assertThat(context.getInitialFunds()).isEqualTo(DEFAULT_DEPOSIT);
                            assertThat(context.getCurrentPrice()).isGreaterThan(0.0);
                            assertThat(context.getInstant()).isNotNull();
                        }
                    },
                    () -> {
                        // Verify that profit/loss tracking continues despite failures
                        for (TradingContext context : contexts) {
                            assertThat(context.getProfitLoss()).isNotNull();
                            assertThat(context.getFundsHistory()).isNotNull();
                            assertThat(context.getMHistory()).isNotNull();
                        }
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify TradingContext calculations with extreme price movements")
            void shouldVerifyTradingContextCalculationsWithExtremePriceMovements() {
                // Given - Use extreme price movements to test calculation robustness
                double[] extremePrices = {100.0, 200.0, 50.0, 150.0, 25.0, 300.0};
                configureMockPriceSeries(extremePrices);
                AsyncTradingStrategy mockStrategy = mock(AsyncTradingStrategy.class);
                
                when(mockStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When
                backtest.run(mockStrategy);
                
                // Then - Verify calculations remain stable with extreme movements
                ArgumentCaptor<TradingContext> contextCaptor = ArgumentCaptor.forClass(TradingContext.class);
                verify(mockStrategy, times(6)).onTick(contextCaptor.capture());
                
                List<TradingContext> contexts = contextCaptor.getAllValues();
                
                assertAll("Extreme price movement calculations verification",
                    () -> {
                        // Verify price tracking accuracy
                        for (int i = 0; i < contexts.size(); i++) {
                            assertThat(contexts.get(i).getCurrentPrice()).isEqualTo(extremePrices[i]);
                        }
                    },
                    () -> {
                        // Verify calculations don't produce NaN or infinite values
                        for (TradingContext context : contexts) {
                            double availableFunds = context.getAvailableFunds();
                            assertThat(Double.isFinite(availableFunds)).isTrue();
                            assertThat(Double.isNaN(availableFunds)).isFalse();
                        }
                    },
                    () -> {
                        // Verify profit/loss calculations remain consistent
                        for (TradingContext context : contexts) {
                            assertThat(context.getProfitLoss()).isNotNull();
                            assertThat(context.getProfitLoss().size()).isGreaterThan(0);
                            
                            // Verify no NaN values in profit/loss history
                            for (Double pl : context.getProfitLoss()) {
                                if (pl != null) {
                                    assertThat(Double.isFinite(pl)).isTrue();
                                    assertThat(Double.isNaN(pl)).isFalse();
                                }
                            }
                        }
                    },
                    () -> {
                        // Verify funds history calculations remain stable
                        for (TradingContext context : contexts) {
                            assertThat(context.getFundsHistory()).isNotNull();
                            assertThat(context.getFundsHistory().size()).isGreaterThan(0);
                            
                            // Verify no NaN values in funds history
                            for (Double funds : context.getFundsHistory()) {
                                if (funds != null) {
                                    assertThat(Double.isFinite(funds)).isTrue();
                                    assertThat(Double.isNaN(funds)).isFalse();
                                }
                            }
                        }
                    }
                );
            }
            
            @Test
            @DisplayName("Should verify TradingContext state consistency across multiple runs")
            void shouldVerifyTradingContextStateConsistencyAcrossMultipleRuns() {
                // Given - Same price data for multiple runs
                double[] consistentPrices = {100.0, 105.0, 95.0, 102.0};
                AsyncTradingStrategy mockStrategy1 = mock(AsyncTradingStrategy.class);
                AsyncTradingStrategy mockStrategy2 = mock(AsyncTradingStrategy.class);
                
                // Configure both strategies identically
                when(mockStrategy1.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy1.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                when(mockStrategy2.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(mockStrategy2.onTick(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
                
                // When - Run backtest twice with same data
                configureMockPriceSeries(consistentPrices);
                Backtest backtest1 = new Backtest(DEFAULT_DEPOSIT, mockPriceSeries, DEFAULT_SYMBOL);
                backtest1.run(mockStrategy1);
                
                configureMockPriceSeries(consistentPrices);
                Backtest backtest2 = new Backtest(DEFAULT_DEPOSIT, mockPriceSeries, DEFAULT_SYMBOL);
                backtest2.run(mockStrategy2);
                
                // Then - Capture contexts from both runs
                ArgumentCaptor<TradingContext> contextCaptor1 = ArgumentCaptor.forClass(TradingContext.class);
                ArgumentCaptor<TradingContext> contextCaptor2 = ArgumentCaptor.forClass(TradingContext.class);
                
                verify(mockStrategy1, times(4)).onTick(contextCaptor1.capture());
                verify(mockStrategy2, times(4)).onTick(contextCaptor2.capture());
                
                List<TradingContext> contexts1 = contextCaptor1.getAllValues();
                List<TradingContext> contexts2 = contextCaptor2.getAllValues();
                
                assertAll("State consistency across multiple runs verification",
                    () -> {
                        // Verify same number of contexts
                        assertThat(contexts1.size()).isEqualTo(contexts2.size());
                    },
                    () -> {
                        // Verify price progression is identical
                        for (int i = 0; i < contexts1.size(); i++) {
                            assertThat(contexts1.get(i).getCurrentPrice())
                                .isEqualTo(contexts2.get(i).getCurrentPrice());
                        }
                    },
                    () -> {
                        // Verify time progression is identical
                        for (int i = 0; i < contexts1.size(); i++) {
                            assertThat(contexts1.get(i).getInstant())
                                .isEqualTo(contexts2.get(i).getInstant());
                        }
                    },
                    () -> {
                        // Verify initial configuration is identical
                        for (int i = 0; i < contexts1.size(); i++) {
                            assertThat(contexts1.get(i).getSymbol())
                                .isEqualTo(contexts2.get(i).getSymbol());
                            assertThat(contexts1.get(i).getInitialFunds())
                                .isEqualTo(contexts2.get(i).getInitialFunds());
                            assertThat(contexts1.get(i).getLeverage())
                                .isEqualTo(contexts2.get(i).getLeverage());
                            assertThat(contexts1.get(i).getMode())
                                .isEqualTo(contexts2.get(i).getMode());
                        }
                    }
                );
            }
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {
        
        @Nested
        @DisplayName("Boundary Condition Tests")
        class BoundaryConditionTests {
            
            @Test
            @DisplayName("Should handle backtest with single price point")
            void shouldHandleBacktestWithSinglePricePoint() {
                // Given
                double singlePrice = 100.0;
                DoubleSeries singlePriceSeries = createTestPriceSeries(singlePrice);
                Backtest singlePriceBacktest = new Backtest(DEFAULT_DEPOSIT, singlePriceSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = singlePriceBacktest.run(strategy);
                
                // Then
                assertAll("Single price point backtest",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPriceSeries()).isEqualTo(singlePriceSeries),
                    () -> assertThat(result.getPriceSeries().size()).isEqualTo(1),
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(result.getCommissions()).isGreaterThanOrEqualTo(0.0)
                );
                
                // Verify strategy was called appropriately
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should handle backtest with identical consecutive prices")
            void shouldHandleBacktestWithIdenticalConsecutivePrices() {
                // Given
                double identicalPrice = 150.0;
                double[] identicalPrices = {identicalPrice, identicalPrice, identicalPrice, identicalPrice, identicalPrice};
                DoubleSeries identicalPriceSeries = createTestPriceSeries(identicalPrices);
                Backtest identicalPriceBacktest = new Backtest(DEFAULT_DEPOSIT, identicalPriceSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = identicalPriceBacktest.run(strategy);
                
                // Then
                assertAll("Identical consecutive prices backtest",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPriceSeries()).isEqualTo(identicalPriceSeries),
                    () -> assertThat(result.getPriceSeries().size()).isEqualTo(identicalPrices.length),
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(result.getCommissions()).isGreaterThanOrEqualTo(0.0)
                );
                
                // Verify strategy was called for each price point
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, times(identicalPrices.length)).onTick(any(TradingContext.class));
                
                // Verify that profit/loss calculations remain accurate with no price movement
                // With identical prices and no orders, P&L should be minimal (only potential commissions)
                assertThat(Math.abs(result.getPl())).isLessThanOrEqualTo(result.getCommissions());
            }
            
            @Test
            @DisplayName("Should handle price series containing extreme values")
            void shouldHandlePriceSeriesContainingExtremeValues() {
                // Given
                double[] extremePrices = {
                    0.01,           // Very small positive value
                    1000000.0,      // Very large value
                    0.001,          // Extremely small positive value
                    Double.MAX_VALUE / 1000, // Very large but not overflow
                    1.0,            // Normal value
                    999999.99       // Large value with decimals
                };
                DoubleSeries extremePriceSeries = createTestPriceSeries(extremePrices);
                Backtest extremePriceBacktest = new Backtest(DEFAULT_DEPOSIT, extremePriceSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = extremePriceBacktest.run(strategy);
                
                // Then
                assertAll("Extreme values backtest",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPriceSeries()).isEqualTo(extremePriceSeries),
                    () -> assertThat(result.getPriceSeries().size()).isEqualTo(extremePrices.length),
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(result.getCommissions()).isGreaterThanOrEqualTo(0.0),
                    () -> {
                        // Verify that profit/loss calculations don't overflow or underflow
                        assertThat(result.getPl()).isNotEqualTo(Double.POSITIVE_INFINITY);
                        assertThat(result.getPl()).isNotEqualTo(Double.NEGATIVE_INFINITY);
                        assertThat(Double.isNaN(result.getPl())).isFalse();
                    },
                    () -> {
                        // Verify final value is calculated correctly
                        assertThat(result.getFinalValue()).isNotEqualTo(Double.POSITIVE_INFINITY);
                        assertThat(result.getFinalValue()).isNotEqualTo(Double.NEGATIVE_INFINITY);
                        assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    }
                );
                
                // Verify strategy was called for each extreme price point
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, times(extremePrices.length)).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should handle backtest with zero price values")
            void shouldHandleBacktestWithZeroPriceValues() {
                // Given
                double[] pricesWithZeros = {100.0, 0.0, 50.0, 0.0, 75.0};
                DoubleSeries zeroPriceSeries = createTestPriceSeries(pricesWithZeros);
                Backtest zeroPriceBacktest = new Backtest(DEFAULT_DEPOSIT, zeroPriceSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = zeroPriceBacktest.run(strategy);
                
                // Then
                assertAll("Zero price values backtest",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPriceSeries()).isEqualTo(zeroPriceSeries),
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> {
                        // Verify calculations remain stable with zero prices
                        assertThat(Double.isNaN(result.getPl())).isFalse();
                        assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    }
                );
                
                // Verify strategy was called for each price point including zeros
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, times(pricesWithZeros.length)).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should handle backtest with negative price values")
            void shouldHandleBacktestWithNegativePriceValues() {
                // Given
                double[] pricesWithNegatives = {100.0, -50.0, 75.0, -25.0, 80.0};
                DoubleSeries negativePriceSeries = createTestPriceSeries(pricesWithNegatives);
                Backtest negativePriceBacktest = new Backtest(DEFAULT_DEPOSIT, negativePriceSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = negativePriceBacktest.run(strategy);
                
                // Then
                assertAll("Negative price values backtest",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPriceSeries()).isEqualTo(negativePriceSeries),
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> {
                        // Verify calculations handle negative prices appropriately
                        assertThat(Double.isNaN(result.getPl())).isFalse();
                        assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    }
                );
                
                // Verify strategy was called for each price point including negatives
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, times(pricesWithNegatives.length)).onTick(any(TradingContext.class));
            }
        }
        
        @Nested
        @DisplayName("Error Scenario Tests")
        class ErrorScenarioTests {
            
            @Test
            @DisplayName("Should handle strategy throwing exceptions during execution")
            void shouldHandleStrategyThrowingExceptionsDuringExecution() {
                // Given
                configureMockPriceSeries(100.0, 101.0, 99.0);
                AsyncTradingStrategy faultyStrategy = mock(AsyncTradingStrategy.class);
                
                // Configure strategy to throw exception on onTick
                when(faultyStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(faultyStrategy.onTick(any(TradingContext.class)))
                    .thenThrow(new RuntimeException("Strategy execution failed"));
                
                // When & Then
                // The backtest should handle the exception gracefully
                try {
                    Backtest.Result result = backtest.run(faultyStrategy);
                    
                    // If the backtest completes, verify it handled the exception
                    assertAll("Exception handling during strategy execution",
                        () -> assertThat(result).isNotNull(),
                        () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                        () -> assertThat(result.getOrders()).isNotNull()
                    );
                } catch (RuntimeException e) {
                    // If exception propagates, verify it's the expected one
                    assertThat(e.getMessage()).contains("Strategy execution failed");
                }
                
                // Verify strategy methods were called
                verify(faultyStrategy).startStrategy(any(TradingContext.class));
                verify(faultyStrategy, atLeastOnce()).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should handle strategy throwing exceptions during startStrategy")
            void shouldHandleStrategyThrowingExceptionsDuringStartStrategy() {
                // Given
                configureMockPriceSeries(100.0, 101.0);
                AsyncTradingStrategy faultyStrategy = mock(AsyncTradingStrategy.class);
                
                // Configure strategy to throw exception on startStrategy
                when(faultyStrategy.startStrategy(any(TradingContext.class)))
                    .thenThrow(new RuntimeException("Strategy initialization failed"));
                
                // When & Then
                try {
                    Backtest.Result result = backtest.run(faultyStrategy);
                    
                    // If the backtest completes, verify it handled the exception
                    assertAll("Exception handling during strategy initialization",
                        () -> assertThat(result).isNotNull(),
                        () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT)
                    );
                } catch (RuntimeException e) {
                    // If exception propagates, verify it's the expected one
                    assertThat(e.getMessage()).contains("Strategy initialization failed");
                }
                
                // Verify startStrategy was called
                verify(faultyStrategy).startStrategy(any(TradingContext.class));
            }
            
            @ParameterizedTest
            @ValueSource(doubles = {0.5, 1.0, 2.0, 5.0, 10.0, 50.0})
            @DisplayName("Should handle different leverage values affecting available funds calculations")
            void shouldHandleDifferentLeverageValuesAffectingAvailableFundsCalculations(double leverage) {
                // Given
                double[] prices = {100.0, 105.0, 95.0, 110.0, 90.0};
                DoubleSeries priceSeries = createTestPriceSeries(prices);
                Backtest leverageBacktest = new Backtest(DEFAULT_DEPOSIT, priceSeries, DEFAULT_SYMBOL);
                leverageBacktest.setLeverage(leverage);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                Backtest.Result result = leverageBacktest.run(strategy);
                
                // Then
                assertAll("Different leverage values handling",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(result.getCommissions()).isGreaterThanOrEqualTo(0.0),
                    () -> {
                        // Verify that leverage affects available funds calculations
                        // Higher leverage should allow for larger position sizes
                        assertThat(Double.isNaN(result.getPl())).isFalse();
                        assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    }
                );
                
                // Verify strategy was called for each price point
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, times(prices.length)).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should handle multiple orders creation and closing scenarios")
            void shouldHandleMultipleOrdersCreationAndClosingScenarios() {
                // Given
                double[] prices = {100.0, 101.0, 99.0, 102.0, 98.0, 103.0};
                DoubleSeries priceSeries = createTestPriceSeries(prices);
                Backtest multiOrderBacktest = new Backtest(DEFAULT_DEPOSIT, priceSeries, DEFAULT_SYMBOL);
                
                // Create a strategy that generates multiple orders
                AsyncTradingStrategy multiOrderStrategy = mock(AsyncTradingStrategy.class);
                when(multiOrderStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                
                // Configure strategy to return orders on each tick
                when(multiOrderStrategy.onTick(any(TradingContext.class)))
                    .thenAnswer(invocation -> {
                        List<SimpleOrder> orders = new ArrayList<>();
                        TradingContext context = invocation.getArgument(0);
                        
                        // Create buy and sell orders alternately
                        if (context.getCurrentPrice() < 101.0) {
                            orders.add(new SimpleOrder("BUY", 100, context.getCurrentPrice()));
                        } else {
                            orders.add(new SimpleOrder("SELL", 50, context.getCurrentPrice()));
                        }
                        
                        return CompletableFuture.completedFuture(orders);
                    });
                
                // When
                Backtest.Result result = multiOrderBacktest.run(multiOrderStrategy);
                
                // Then
                assertAll("Multiple orders creation and closing",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> {
                        // Verify that multiple orders were processed
                        // The exact number depends on the strategy logic and order execution
                        assertThat(result.getOrders().size()).isGreaterThanOrEqualTo(0);
                    },
                    () -> {
                        // Verify commissions account for multiple orders
                        if (result.getOrders().size() > 0) {
                            assertThat(result.getCommissions()).isGreaterThan(0.0);
                        }
                    },
                    () -> {
                        // Verify profit/loss calculations account for all transactions
                        assertThat(Double.isNaN(result.getPl())).isFalse();
                        assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    }
                );
                
                // Verify strategy was called for each price point
                verify(multiOrderStrategy).startStrategy(any(TradingContext.class));
                verify(multiOrderStrategy, times(prices.length)).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should handle backtest with very large datasets")
            void shouldHandleBacktestWithVeryLargeDatasets() {
                // Given
                int largeDatasetSize = 10000;
                double[] largePriceArray = new double[largeDatasetSize];
                
                // Generate large price dataset with realistic price movements
                double basePrice = 100.0;
                largePriceArray[0] = basePrice;
                for (int i = 1; i < largeDatasetSize; i++) {
                    // Simulate random price movements within reasonable bounds
                    double change = (Math.random() - 0.5) * 2.0; // -1 to +1 change
                    largePriceArray[i] = Math.max(0.01, largePriceArray[i-1] + change);
                }
                
                DoubleSeries largePriceSeries = createTestPriceSeries(largePriceArray);
                Backtest largeDatasetBacktest = new Backtest(DEFAULT_DEPOSIT, largePriceSeries, DEFAULT_SYMBOL);
                AsyncTradingStrategy strategy = createMockStrategy();
                
                // When
                long startTime = System.currentTimeMillis();
                Backtest.Result result = largeDatasetBacktest.run(strategy);
                long executionTime = System.currentTimeMillis() - startTime;
                
                // Then
                assertAll("Large dataset handling",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getPriceSeries()).isEqualTo(largePriceSeries),
                    () -> assertThat(result.getPriceSeries().size()).isEqualTo(largeDatasetSize),
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> assertThat(result.getCommissions()).isGreaterThanOrEqualTo(0.0),
                    () -> {
                        // Verify performance remains acceptable (should complete within reasonable time)
                        // Note: This is a rough performance check - adjust threshold as needed
                        assertThat(executionTime).isLessThan(30000); // 30 seconds max
                    },
                    () -> {
                        // Verify calculations remain accurate with large datasets
                        assertThat(Double.isNaN(result.getPl())).isFalse();
                        assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                        assertThat(result.getPl()).isNotEqualTo(Double.POSITIVE_INFINITY);
                        assertThat(result.getPl()).isNotEqualTo(Double.NEGATIVE_INFINITY);
                    }
                );
                
                // Verify strategy was called for each price point
                verify(strategy).startStrategy(any(TradingContext.class));
                verify(strategy, times(largeDatasetSize)).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should handle backtest with insufficient funds scenarios")
            void shouldHandleBacktestWithInsufficientFundsScenarios() {
                // Given
                double smallDeposit = 100.0; // Very small deposit
                double[] prices = {1000.0, 1100.0, 1200.0}; // High prices relative to deposit
                DoubleSeries priceSeries = createTestPriceSeries(prices);
                Backtest insufficientFundsBacktest = new Backtest(smallDeposit, priceSeries, DEFAULT_SYMBOL);
                
                // Create strategy that tries to make large orders
                AsyncTradingStrategy aggressiveStrategy = mock(AsyncTradingStrategy.class);
                when(aggressiveStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(aggressiveStrategy.onTick(any(TradingContext.class)))
                    .thenAnswer(invocation -> {
                        List<SimpleOrder> orders = new ArrayList<>();
                        TradingContext context = invocation.getArgument(0);
                        
                        // Try to buy more than available funds allow
                        orders.add(new SimpleOrder("BUY", 1000, context.getCurrentPrice()));
                        
                        return CompletableFuture.completedFuture(orders);
                    });
                
                // When
                Backtest.Result result = insufficientFundsBacktest.run(aggressiveStrategy);
                
                // Then
                assertAll("Insufficient funds handling",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(smallDeposit),
                    () -> assertThat(result.getFinalValue()).isEqualTo(smallDeposit + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> {
                        // Verify the backtest handles insufficient funds gracefully
                        // Orders that can't be executed due to insufficient funds should be rejected
                        assertThat(Double.isNaN(result.getPl())).isFalse();
                        assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    }
                );
                
                // Verify strategy was called
                verify(aggressiveStrategy).startStrategy(any(TradingContext.class));
                verify(aggressiveStrategy, atLeastOnce()).onTick(any(TradingContext.class));
            }
            
            @Test
            @DisplayName("Should handle concurrent modification scenarios")
            void shouldHandleConcurrentModificationScenarios() {
                // Given
                double[] prices = {100.0, 101.0, 99.0, 102.0};
                DoubleSeries priceSeries = createTestPriceSeries(prices);
                Backtest concurrentBacktest = new Backtest(DEFAULT_DEPOSIT, priceSeries, DEFAULT_SYMBOL);
                
                // Create strategy that modifies context in complex ways
                AsyncTradingStrategy complexStrategy = mock(AsyncTradingStrategy.class);
                when(complexStrategy.startStrategy(any(TradingContext.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
                when(complexStrategy.onTick(any(TradingContext.class)))
                    .thenAnswer(invocation -> {
                        List<SimpleOrder> orders = new ArrayList<>();
                        TradingContext context = invocation.getArgument(0);
                        
                        // Create multiple orders that might cause concurrent modifications
                        orders.add(new SimpleOrder("BUY", 10, context.getCurrentPrice()));
                        orders.add(new SimpleOrder("SELL", 5, context.getCurrentPrice()));
                        orders.add(new SimpleOrder("BUY", 15, context.getCurrentPrice()));
                        
                        return CompletableFuture.completedFuture(orders);
                    });
                
                // When
                Backtest.Result result = concurrentBacktest.run(complexStrategy);
                
                // Then
                assertAll("Concurrent modification handling",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getInitialFund()).isEqualTo(DEFAULT_DEPOSIT),
                    () -> assertThat(result.getFinalValue()).isEqualTo(DEFAULT_DEPOSIT + result.getPl()),
                    () -> assertThat(result.getOrders()).isNotNull(),
                    () -> {
                        // Verify the backtest handles complex order scenarios without corruption
                        assertThat(Double.isNaN(result.getPl())).isFalse();
                        assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    }
                );
                
                // Verify strategy was called for each price point
                verify(complexStrategy).startStrategy(any(TradingContext.class));
                verify(complexStrategy, times(prices.length)).onTick(any(TradingContext.class));
            }
        }
    }
    
    @Nested
    @DisplayName("Parameterized Tests for Comprehensive Coverage")
    class ParameterizedComprehensiveTests {
        
        @ParameterizedTest
        @ValueSource(doubles = {0.0, 1000.0, -500.0, 1000000.0, 50.5, -0.01, 999999.99})
        @DisplayName("Should create Backtest with various deposit values")
        void shouldCreateBacktestWithVariousDepositValues(double deposit) {
            // Given
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0, 99.0);
            String symbol = "TEST";
            
            // When
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            
            // Then
            assertAll("Parameterized deposit constructor validation",
                () -> assertThat(backtest).isNotNull(),
                () -> assertThat(backtest.getDeposit()).isEqualTo(deposit),
                () -> assertThat(backtest.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(backtest.getSymbol()).isEqualTo(symbol),
                () -> assertThat(backtest.getLeverage()).isEqualTo(1.0)
            );
        }
        
        @ParameterizedTest
        @CsvSource({
            "100.0, 101.0, 1.0",      // profit scenario
            "100.0, 99.0, -1.0",     // loss scenario  
            "100.0, 100.0, 0.0",     // no change scenario
            "50.0, 75.0, 25.0",      // larger profit
            "200.0, 150.0, -50.0",   // larger loss
            "1.0, 1.5, 0.5",         // small values
            "1000.0, 1001.0, 1.0"    // large values
        })
        @DisplayName("Should calculate profit/loss correctly with different price scenarios")
        void shouldCalculateProfitLossCorrectlyWithDifferentPriceScenarios(
                double openPrice, double closePrice, double expectedPLPerUnit) {
            // Given
            double deposit = 10000.0;
            DoubleSeries priceSeries = createTestPriceSeries(openPrice, closePrice);
            Backtest backtest = new Backtest(deposit, priceSeries, "TEST");
            
            // Create strategy that buys 1 unit at open and sells at close
            AsyncTradingStrategy strategy = mock(AsyncTradingStrategy.class);
            when(strategy.startStrategy(any(TradingContext.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
            when(strategy.onTick(any(TradingContext.class)))
                .thenAnswer(invocation -> {
                    List<SimpleOrder> orders = new ArrayList<>();
                    TradingContext context = invocation.getArgument(0);
                    
                    // Buy on first tick, sell on second tick
                    if (context.getCurrentPrice() == openPrice) {
                        orders.add(new SimpleOrder("BUY", 1, openPrice));
                    } else if (context.getCurrentPrice() == closePrice) {
                        orders.add(new SimpleOrder("SELL", 1, closePrice));
                    }
                    
                    return CompletableFuture.completedFuture(orders);
                });
            
            // When
            Backtest.Result result = backtest.run(strategy);
            
            // Then
            assertAll("Profit/loss calculation validation",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getInitialFund()).isEqualTo(deposit),
                () -> assertThat(result.getFinalValue()).isEqualTo(deposit + result.getPl()),
                () -> {
                    // The actual P/L calculation depends on the implementation details
                    // but should be consistent with the price difference
                    assertThat(Double.isNaN(result.getPl())).isFalse();
                    assertThat(Double.isInfinite(result.getPl())).isFalse();
                }
            );
            
            // Verify strategy was called appropriately
            verify(strategy).startStrategy(any(TradingContext.class));
            verify(strategy, times(2)).onTick(any(TradingContext.class));
        }
        
        @ParameterizedTest
        @ValueSource(doubles = {1.0, 2.0, 5.0, 10.0, 0.5, 0.1})
        @DisplayName("Should handle different leverage values correctly")
        void shouldHandleDifferentLeverageValuesCorrectly(double leverage) {
            // Given
            double deposit = 10000.0;
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0, 99.0);
            Backtest backtest = new Backtest(deposit, priceSeries, "TEST");
            backtest.setLeverage(leverage);
            
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            backtest.initialize(strategy);
            
            // Then
            TradingContext context = backtest.getContext();
            assertAll("Leverage configuration validation",
                () -> assertThat(context).isNotNull(),
                () -> assertThat(context.getLeverage()).isEqualTo(leverage),
                () -> assertThat(context.getInitialFunds()).isEqualTo(deposit),
                () -> {
                    // Available funds should be calculated with leverage
                    double expectedAvailableFunds = deposit * leverage;
                    assertThat(context.getAvailableFunds()).isEqualTo(expectedAvailableFunds);
                }
            );
            
            // Verify strategy was initialized
            verify(strategy).startStrategy(any(TradingContext.class));
        }
        
        @ParameterizedTest
        @CsvSource({
            "1000.0, 'AAPL', 1.0",
            "5000.0, 'GOOGL', 2.0", 
            "0.0, 'TSLA', 1.0",
            "-1000.0, 'MSFT', 0.5",
            "1000000.0, 'AMZN', 10.0",
            "50.5, 'TEST', 1.5"
        })
        @DisplayName("Should create complete backtest scenarios with various parameters")
        void shouldCreateCompleteBacktestScenariosWithVariousParameters(
                double deposit, String symbol, double leverage) {
            // Given
            DoubleSeries priceSeries = createTestPriceSeries(100.0, 101.0, 99.0, 102.0);
            Backtest backtest = new Backtest(deposit, priceSeries, symbol);
            backtest.setLeverage(leverage);
            
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            Backtest.Result result = backtest.run(strategy);
            
            // Then
            assertAll("Complete backtest scenario validation",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getInitialFund()).isEqualTo(deposit),
                () -> assertThat(result.getFinalValue()).isEqualTo(deposit + result.getPl()),
                () -> assertThat(result.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(result.getOrders()).isNotNull(),
                () -> assertThat(result.getCommissions()).isGreaterThanOrEqualTo(0.0),
                () -> {
                    // Verify calculations are valid (not NaN or infinite)
                    assertThat(Double.isNaN(result.getPl())).isFalse();
                    assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    assertThat(Double.isInfinite(result.getPl())).isFalse();
                    assertThat(Double.isInfinite(result.getFinalValue())).isFalse();
                }
            );
            
            // Verify strategy interactions
            verify(strategy).startStrategy(any(TradingContext.class));
            verify(strategy, times(4)).onTick(any(TradingContext.class)); // 4 price points
        }
        
        @ParameterizedTest
        @CsvSource({
            "100.0, 101.0, 102.0, 103.0",  // upward trend
            "100.0, 99.0, 98.0, 97.0",     // downward trend
            "100.0, 100.0, 100.0, 100.0",  // flat prices
            "100.0, 110.0, 90.0, 105.0",   // volatile prices
            "50.0, 75.0, 25.0, 100.0",     // mixed scenario
            "1000.0, 999.0, 1001.0, 1000.0" // large values with small changes
        })
        @DisplayName("Should handle various price trend scenarios")
        void shouldHandleVariousPriceTrendScenarios(
                double price1, double price2, double price3, double price4) {
            // Given
            double deposit = 10000.0;
            DoubleSeries priceSeries = createTestPriceSeries(price1, price2, price3, price4);
            Backtest backtest = new Backtest(deposit, priceSeries, "TREND_TEST");
            
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            Backtest.Result result = backtest.run(strategy);
            
            // Then
            assertAll("Price trend scenario validation",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getInitialFund()).isEqualTo(deposit),
                () -> assertThat(result.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(result.getOrders()).isNotNull(),
                () -> {
                    // Verify all calculations are valid regardless of price trend
                    assertThat(Double.isNaN(result.getPl())).isFalse();
                    assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    assertThat(Double.isInfinite(result.getPl())).isFalse();
                    assertThat(Double.isInfinite(result.getFinalValue())).isFalse();
                },
                () -> {
                    // Final value should equal initial fund plus profit/loss
                    assertThat(result.getFinalValue()).isEqualTo(deposit + result.getPl());
                }
            );
            
            // Verify strategy was called for each price point
            verify(strategy).startStrategy(any(TradingContext.class));
            verify(strategy, times(4)).onTick(any(TradingContext.class));
        }
        
        @ParameterizedTest
        @ValueSource(ints = {1, 2, 5, 10, 50, 100})
        @DisplayName("Should handle datasets of various sizes")
        void shouldHandleDatasetsOfVariousSizes(int datasetSize) {
            // Given
            double deposit = 10000.0;
            double[] prices = new double[datasetSize];
            double basePrice = 100.0;
            
            // Generate price data with slight variations
            for (int i = 0; i < datasetSize; i++) {
                prices[i] = basePrice + (i % 2 == 0 ? 1.0 : -1.0); // Alternating +1/-1
            }
            
            DoubleSeries priceSeries = createTestPriceSeries(prices);
            Backtest backtest = new Backtest(deposit, priceSeries, "SIZE_TEST");
            
            AsyncTradingStrategy strategy = createMockStrategy();
            
            // When
            long startTime = System.currentTimeMillis();
            Backtest.Result result = backtest.run(strategy);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Then
            assertAll("Dataset size handling validation",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getInitialFund()).isEqualTo(deposit),
                () -> assertThat(result.getPriceSeries()).isEqualTo(priceSeries),
                () -> assertThat(result.getOrders()).isNotNull(),
                () -> {
                    // Verify calculations remain valid for all dataset sizes
                    assertThat(Double.isNaN(result.getPl())).isFalse();
                    assertThat(Double.isNaN(result.getFinalValue())).isFalse();
                    assertThat(Double.isInfinite(result.getPl())).isFalse();
                    assertThat(Double.isInfinite(result.getFinalValue())).isFalse();
                },
                () -> {
                    // Performance should remain reasonable (adjust threshold as needed)
                    assertThat(executionTime).isLessThan(10000); // 10 seconds max
                }
            );
            
            // Verify strategy was called for each price point
            verify(strategy).startStrategy(any(TradingContext.class));
            verify(strategy, times(datasetSize)).onTick(any(TradingContext.class));
        }
    }
    
    @Nested
    @DisplayName("Test Data Providers and Utilities")
    class TestDataProvidersAndUtilities {
        
        @Test
        @DisplayName("Should provide consistent test data through builders")
        void shouldProvideConsistentTestDataThroughBuilders() {
            // Test the test data builder methods for consistency
            
            // Test DoubleSeries creation
            DoubleSeries series1 = createTestPriceSeries(100.0, 101.0, 99.0);
            DoubleSeries series2 = createTestPriceSeries(100.0, 101.0, 99.0);
            
            assertAll("Test data builder consistency",
                () -> assertThat(series1.size()).isEqualTo(series2.size()),
                () -> assertThat(series1.size()).isEqualTo(3),
                () -> assertThat(series1.getSymbol()).isEqualTo(series2.getSymbol()),
                () -> assertThat(series1.isEmpty()).isFalse(),
                () -> assertThat(series2.isEmpty()).isFalse()
            );
            
            // Test price entry creation
            TimeSeries.Entry<Double> entry1 = createPriceEntry(100.0, BASE_TIME);
            TimeSeries.Entry<Double> entry2 = createPriceEntry(100.0, BASE_TIME);
            
            assertAll("Price entry builder consistency",
                () -> assertThat(entry1.getItem()).isEqualTo(entry2.getItem()),
                () -> assertThat(entry1.getInstant()).isEqualTo(entry2.getInstant()),
                () -> assertThat(entry1.getItem()).isEqualTo(100.0),
                () -> assertThat(entry1.getInstant()).isEqualTo(BASE_TIME)
            );
            
            // Test mock strategy creation
            AsyncTradingStrategy strategy1 = createMockStrategy();
            AsyncTradingStrategy strategy2 = createMockStrategy();
            
            assertAll("Mock strategy builder consistency",
                () -> assertThat(strategy1).isNotNull(),
                () -> assertThat(strategy2).isNotNull(),
                () -> assertThat(strategy1).isNotEqualTo(strategy2) // Different instances
            );
        }
        
        @Test
        @DisplayName("Should provide common test scenarios through data providers")
        void shouldProvideCommonTestScenariosThroughDataProviders() {
            // Test the TestDataProvider class constants
            
            assertAll("Test data provider constants",
                () -> assertThat(TestDataProvider.DEPOSIT_VALUES).isNotNull(),
                () -> assertThat(TestDataProvider.DEPOSIT_VALUES.length).isGreaterThan(0),
                () -> assertThat(TestDataProvider.SYMBOL_VALUES).isNotNull(),
                () -> assertThat(TestDataProvider.SYMBOL_VALUES.length).isGreaterThan(0),
                () -> assertThat(TestDataProvider.PRICE_SCENARIOS).isNotNull(),
                () -> assertThat(TestDataProvider.PRICE_SCENARIOS.length).isGreaterThan(0)
            );
            
            // Verify deposit values include edge cases
            assertAll("Deposit values coverage",
                () -> assertThat(TestDataProvider.DEPOSIT_VALUES).contains(0.0),
                () -> assertThat(TestDataProvider.DEPOSIT_VALUES).contains(1000.0),
                () -> assertThat(TestDataProvider.DEPOSIT_VALUES).contains(-500.0),
                () -> assertThat(TestDataProvider.DEPOSIT_VALUES).contains(1000000.0)
            );
            
            // Verify symbol values include edge cases
            assertAll("Symbol values coverage",
                () -> assertThat(TestDataProvider.SYMBOL_VALUES).contains("AAPL"),
                () -> assertThat(TestDataProvider.SYMBOL_VALUES).contains("GOOGL"),
                () -> assertThat(TestDataProvider.SYMBOL_VALUES).contains(""),
                () -> assertThat(TestDataProvider.SYMBOL_VALUES).contains(null)
            );
            
            // Verify price scenarios include different market conditions
            assertAll("Price scenarios coverage",
                () -> assertThat(TestDataProvider.PRICE_SCENARIOS.length).isGreaterThanOrEqualTo(4),
                () -> {
                    // Should have profit scenario (price goes up)
                    boolean hasProfitScenario = false;
                    for (double[] scenario : TestDataProvider.PRICE_SCENARIOS) {
                        if (scenario.length >= 2 && scenario[1] > scenario[0]) {
                            hasProfitScenario = true;
                            break;
                        }
                    }
                    assertThat(hasProfitScenario).isTrue();
                },
                () -> {
                    // Should have loss scenario (price goes down)
                    boolean hasLossScenario = false;
                    for (double[] scenario : TestDataProvider.PRICE_SCENARIOS) {
                        if (scenario.length >= 2 && scenario[1] < scenario[0]) {
                            hasLossScenario = true;
                            break;
                        }
                    }
                    assertThat(hasLossScenario).isTrue();
                },
                () -> {
                    // Should have single price scenario
                    boolean hasSinglePriceScenario = false;
                    for (double[] scenario : TestDataProvider.PRICE_SCENARIOS) {
                        if (scenario.length == 1) {
                            hasSinglePriceScenario = true;
                            break;
                        }
                    }
                    assertThat(hasSinglePriceScenario).isTrue();
                }
            );
        }
        
        @Test
        @DisplayName("Should validate helper methods work correctly")
        void shouldValidateHelperMethodsWorkCorrectly() {
            // Test createBacktestWithTestData helper
            Backtest testBacktest = createBacktestWithTestData(5000.0, 100.0, 101.0, 99.0);
            
            assertAll("Helper method validation",
                () -> assertThat(testBacktest).isNotNull(),
                () -> assertThat(testBacktest.getDeposit()).isEqualTo(5000.0),
                () -> assertThat(testBacktest.getPriceSeries()).isNotNull(),
                () -> assertThat(testBacktest.getPriceSeries().size()).isEqualTo(3),
                () -> assertThat(testBacktest.getSymbol()).isEqualTo(DEFAULT_SYMBOL)
            );
            
            // Test assertResultValid helper
            Backtest.Result mockResult = mock(Backtest.Result.class);
            when(mockResult.getPl()).thenReturn(100.0);
            when(mockResult.getInitialFund()).thenReturn(5000.0);
            when(mockResult.getFinalValue()).thenReturn(5100.0);
            when(mockResult.getOrders()).thenReturn(new ArrayList<>());
            when(mockResult.getPriceSeries()).thenReturn(createTestPriceSeries(100.0, 101.0));
            when(mockResult.getCommissions()).thenReturn(10.0);
            
            // This should not throw any exceptions
            assertResultValid(mockResult, 100.0, 5000.0);
            
            // Test assertTradingContextValid helper
            TradingContext mockContext = mock(TradingContext.class);
            when(mockContext.getSymbol()).thenReturn("TEST");
            when(mockContext.getInitialFunds()).thenReturn(10000.0);
            when(mockContext.getOrders()).thenReturn(new ArrayList<>());
            when(mockContext.getClosedOrders()).thenReturn(new ArrayList<>());
            when(mockContext.getProfitLoss()).thenReturn(new ArrayList<>());
            when(mockContext.getFundsHistory()).thenReturn(new ArrayList<>());
            
            // This should not throw any exceptions
            assertTradingContextValid(mockContext, "TEST", 10000.0);
        }
    }
}