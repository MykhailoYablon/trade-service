package com.example.tradeservice.service;

import com.example.tradeservice.entity.Account;

import java.util.Map;

public interface AccountService {

    Map<String, String> getAccount();

    void setAccount(String tag, String value);
}
