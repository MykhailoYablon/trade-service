# Unit Tests for Trade Service

This directory contains comprehensive unit tests for the Trade Service application. The tests are designed to provide good coverage of the service layer functionality while maintaining isolation through proper mocking.

## Test Structure

### Service Tests

1. **AccountServiceTest** - Tests for account management functionality
   - Tests account info storage and retrieval
   - Handles edge cases like null values and empty strings
   - Verifies proper state management

2. **PositionServiceTest** - Tests for position management
   - Tests position retrieval and creation
   - Mocks dependencies like PositionTracker and PositionMapper
   - Handles both successful and error scenarios

3. **ContractManagerServiceTest** - Tests for contract management
   - Tests contract search functionality
   - Tests market data subscription
   - Mocks TWS connection and repository dependencies

4. **TradeDataServiceTest** - Tests for real-time trade data processing
   - Tests Redis operations for trade storage
   - Tests event publishing for trade updates
   - Tests trade history retrieval

5. **TimeUtilsTest** - Tests for time parsing utilities
   - Tests IB time format parsing
   - Tests timezone conversion
   - Tests error handling for invalid formats

6. **OrderTrackerTest** - Tests for order management
   - Tests order placement and tracking
   - Tests order status updates
   - Tests limit order creation

7. **FinnhubClientTest** - Tests for external API client
   - Tests symbol lookup functionality
   - Tests quote retrieval
   - Tests market status checking

## Running the Tests

### Prerequisites
- Java 17 or higher
- Gradle build system
- H2 database (for in-memory testing)

### Command Line
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests AccountServiceTest

# Run tests with coverage
./gradlew test jacocoTestReport

# Run tests with detailed output
./gradlew test --info
```

### IDE Integration
- Import the project into your IDE
- Run tests using the IDE's test runner
- Tests use JUnit 5 and Mockito for mocking

## Test Configuration

The tests use a separate configuration file (`application-test.yml`) that includes:
- In-memory H2 database configuration
- Test-specific Redis settings
- Mock API tokens
- Debug logging for test troubleshooting

## Mocking Strategy

The tests use Mockito for mocking dependencies:
- **External APIs**: FinnhubClient, TWSConnectionManager
- **Databases**: Repository interfaces
- **Redis**: RedisTemplate operations
- **Event Publishing**: ApplicationEventPublisher

## Test Coverage

The tests cover:
- **Happy Path**: Normal operation scenarios
- **Edge Cases**: Null values, empty strings, invalid inputs
- **Error Handling**: Exception scenarios and error responses
- **State Management**: Proper state transitions and updates
- **Integration Points**: API calls and external service interactions

## Best Practices

1. **Test Isolation**: Each test is independent and doesn't rely on other tests
2. **Descriptive Names**: Test method names clearly describe what is being tested
3. **Given-When-Then**: Tests follow the AAA (Arrange-Act-Assert) pattern
4. **Mock Verification**: Tests verify that mocked dependencies are called correctly
5. **Edge Case Coverage**: Tests include boundary conditions and error scenarios

## Adding New Tests

When adding new service classes:
1. Create a test class in the same package structure under `src/test/java`
2. Use `@ExtendWith(MockitoExtension.class)` for Mockito integration
3. Mock all external dependencies
4. Test both success and failure scenarios
5. Include edge cases and boundary conditions
6. Follow the existing naming conventions

## Troubleshooting

### Common Issues
1. **Mockito Errors**: Ensure all dependencies are properly mocked
2. **Redis Connection**: Tests use in-memory Redis or mocked RedisTemplate
3. **Database Issues**: Tests use H2 in-memory database
4. **API Token**: Tests use mock tokens, not real API credentials

### Debug Mode
Enable debug logging in `application-test.yml` to see detailed test execution:
```yaml
logging:
  level:
    com.example.tradeservice: DEBUG
```

