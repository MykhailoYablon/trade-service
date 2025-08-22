package com.example.tradeservice.service;

import com.example.tradeservice.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private com.example.tradeservice.repository.AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void getAccount_ShouldReturnEmptyMap_WhenNoAccountInfoSet() {
        // When
        Map<String, String> result = accountService.getAccount();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void setAccount_ShouldStoreAccountInfo() {
        // Given
        String tag = "NetLiquidation";
        String value = "100000.00";

        // When
        accountService.setAccount(tag, value);

        // Then
        Map<String, String> accountInfo = accountService.getAccount();
        assertEquals(value, accountInfo.get(tag));
        assertEquals(1, accountInfo.size());
    }

    @Test
    void setAccount_ShouldUpdateExistingTag() {
        // Given
        String tag = "NetLiquidation";
        String initialValue = "100000.00";
        String updatedValue = "95000.00";

        // When
        accountService.setAccount(tag, initialValue);
        accountService.setAccount(tag, updatedValue);

        // Then
        Map<String, String> accountInfo = accountService.getAccount();
        assertEquals(updatedValue, accountInfo.get(tag));
        assertEquals(1, accountInfo.size());
    }

    @Test
    void setAccount_ShouldHandleMultipleTags() {
        // Given
        String tag1 = "NetLiquidation";
        String value1 = "100000.00";
        String tag2 = "AvailableFunds";
        String value2 = "50000.00";

        // When
        accountService.setAccount(tag1, value1);
        accountService.setAccount(tag2, value2);

        // Then
        Map<String, String> accountInfo = accountService.getAccount();
        assertEquals(value1, accountInfo.get(tag1));
        assertEquals(value2, accountInfo.get(tag2));
        assertEquals(2, accountInfo.size());
    }

    @Test
    void setAccount_ShouldHandleNullValues() {
        // Given
        String tag = "NetLiquidation";
        String value = null;

        // When
        accountService.setAccount(tag, value);

        // Then
        Map<String, String> accountInfo = accountService.getAccount();
        assertNull(accountInfo.get(tag));
        assertEquals(1, accountInfo.size());
    }

    @Test
    void setAccount_ShouldHandleEmptyString() {
        // Given
        String tag = "NetLiquidation";
        String value = "";

        // When
        accountService.setAccount(tag, value);

        // Then
        Map<String, String> accountInfo = accountService.getAccount();
        assertEquals(value, accountInfo.get(tag));
        assertEquals(1, accountInfo.size());
    }
}
