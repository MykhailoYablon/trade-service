package com.example.tradeservice.model.enums;

public enum Endpoint {

    METRIC("https://finnhub.io/api/v1/stock/metric"),
    CANDLE("https://finnhub.io/api/v1/stock/candle"),
    COMPANY_NEWS("https://finnhub.io/api/v1/company-news"),
    COMPANY_PROFILE("https://finnhub.io/api/v1/stock/profile2"),
    DIVIDEND("https://finnhub.io/api/v1/stock/dividend"),
    MARKET_HOLIDAY("https://finnhub.io/api/v1/stock/market-holiday"),
    MARKET_STATUS("https://finnhub.io/api/v1/stock/market-status"),
    MARKET_NEWS("https://finnhub.io/api/v1/news"),
    SYMBOL("https://finnhub.io/api/v1/stock/symbol"),
    SYMBOL_LOOKUP("https://finnhub.io/api/v1/search"),
    QUOTE("https://finnhub.io/api/v1/quote"),
    INSIDER_TRANSACTIONS("https://finnhub.io/api/v1/stock/insider-transactions");


    private String url;

    Endpoint(String url) {
        this.url = url;
    }

    public String url() {
        return url;
    }

}