package com.example.tradeservice.service;


import java.util.Map;

public interface AccountService {

    Map<String, String> getAccount();

    void setAccount(String tag, String value);
}
