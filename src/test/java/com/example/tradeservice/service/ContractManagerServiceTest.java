package com.example.tradeservice.service;

import com.example.tradeservice.mapper.ContractMapper;
import com.example.tradeservice.model.ContractHolder;
import com.example.tradeservice.model.ContractModel;
import com.example.tradeservice.repository.ContractRepository;
import com.example.tradeservice.repository.PositionRepository;
import com.example.tradeservice.service.TwsResultHolder;
import com.example.tradeservice.service.impl.ContractManagerServiceImpl;
import com.ib.client.Contract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractManagerServiceTest {

    @Mock
    private TWSConnectionManager tws;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractManagerServiceImpl contractManagerService;

    private Contract mockContract;
    private ContractHolder mockContractHolder;
    private ContractModel mockContractModel;
    private TwsResultHolder<ContractHolder> mockTwsResult;

    @BeforeEach
    void setUp() {
        mockContract = new Contract();
        mockContract.conid(12345);
        mockContract.symbol("AAPL");
        mockContract.secType("STK");
        mockContract.exchange("SMART");
        mockContract.currency("USD");

        mockContractHolder = new ContractHolder();
        mockContractHolder.setContract(mockContract);

        mockContractModel = new ContractModel(12345, "AAPL", "SMART", "USD", "Apple Inc.");

        mockTwsResult = new TwsResultHolder<>(mockContractHolder);
    }

    @Test
    void searchContract_ShouldReturnContractModels_WhenSearchIsSuccessful() {
        // Given
        String searchTerm = "AAPL";
        List<Contract> contracts = Arrays.asList(mockContract);
        TwsResultHolder<List<Contract>> searchResult = new TwsResultHolder<>(contracts);

        when(tws.searchContract(searchTerm)).thenReturn(searchResult);
        when(contractMapper.convertToContract(mockContract)).thenReturn(mockContractModel);

        // When
        List<ContractModel> result = contractManagerService.searchContract(searchTerm);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockContractModel, result.get(0));
        verify(tws).searchContract(searchTerm);
        verify(contractMapper).convertToContract(mockContract);
    }

    @Test
    void searchContract_ShouldThrowRuntimeException_WhenTwsReturnsError() {
        // Given
        String searchTerm = "INVALID";
        TwsResultHolder<List<Contract>> searchResult = new TwsResultHolder<>("Contract not found");

        when(tws.searchContract(searchTerm)).thenReturn(searchResult);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            contractManagerService.searchContract(searchTerm);
        });
        verify(tws).searchContract(searchTerm);
        verify(contractMapper, never()).convertToContract(any());
    }

    @Test
    void searchContract_ShouldReturnEmptyList_WhenNoContractsFound() {
        // Given
        String searchTerm = "NONEXISTENT";
        TwsResultHolder<List<Contract>> searchResult = new TwsResultHolder<>(Arrays.asList());

        when(tws.searchContract(searchTerm)).thenReturn(searchResult);

        // When
        List<ContractModel> result = contractManagerService.searchContract(searchTerm);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tws).searchContract(searchTerm);
        verify(contractMapper, never()).convertToContract(any());
    }

    @Test
    void getMarketData_ShouldSaveContractAndSubscribe_WhenContractFound() {
        // Given
        int conid = 12345;
        mockTwsResult = new TwsResultHolder<>(mockContractHolder);

        when(tws.requestContractByConid(conid)).thenReturn(mockTwsResult);

        // When
        contractManagerService.getMarketData(conid);

        // Then
        verify(tws).requestContractByConid(conid);
        verify(contractRepository).save(mockContractHolder);
        // Note: The actual market data subscription is commented out in the implementation
        // so we don't verify that call
    }

    @Test
    void getMarketData_ShouldNotSaveContract_WhenTwsReturnsError() {
        // Given
        int conid = 99999;
        TwsResultHolder<ContractHolder> errorResult = new TwsResultHolder<>("Contract not found");

        when(tws.requestContractByConid(conid)).thenReturn(errorResult);

        // When
        contractManagerService.getMarketData(conid);

        // Then
        verify(tws).requestContractByConid(conid);
        verify(contractRepository, never()).save(any());
    }

    @Test
    void searchContract_ShouldHandleMultipleContracts() {
        // Given
        String searchTerm = "TECH";
        
        Contract contract2 = new Contract();
        contract2.conid(67890);
        contract2.symbol("GOOGL");
        contract2.secType("STK");
        contract2.exchange("SMART");
        contract2.currency("USD");

        ContractModel contractModel2 = new ContractModel(67890, "GOOGL", "SMART", "USD", "Alphabet Inc.");

        List<Contract> contracts = Arrays.asList(mockContract, contract2);
        TwsResultHolder<List<Contract>> searchResult = new TwsResultHolder<>(contracts);

        when(tws.searchContract(searchTerm)).thenReturn(searchResult);
        when(contractMapper.convertToContract(mockContract)).thenReturn(mockContractModel);
        when(contractMapper.convertToContract(contract2)).thenReturn(contractModel2);

        // When
        List<ContractModel> result = contractManagerService.searchContract(searchTerm);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(mockContractModel, result.get(0));
        assertEquals(contractModel2, result.get(1));
        verify(tws).searchContract(searchTerm);
        verify(contractMapper).convertToContract(mockContract);
        verify(contractMapper).convertToContract(contract2);
    }
}
