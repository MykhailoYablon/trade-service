package com.example.tradeservice.service.impl;

import com.example.tradeservice.entity.Account;
import com.example.tradeservice.repository.AccountRepository;
import com.example.tradeservice.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    AccountRepository accountRepository;

    Map<String, String> accountInfo = new HashMap<>();

    @Override
    public Map<String, String> getAccount() {
        return accountInfo;
    }

    @Override
    public void setAccount(String tag, String value) {
        accountInfo.put(tag, value);
    }
}
