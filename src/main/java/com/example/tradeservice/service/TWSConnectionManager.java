package com.example.tradeservice.service;

import com.example.tradeservice.entity.DataRequest;
import com.example.tradeservice.entity.HistoricalData;
import com.example.tradeservice.model.ContractHolder;
import com.example.tradeservice.model.PositionHolder;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.repository.ContractRepository;
import com.example.tradeservice.repository.DataRequestRepository;
import com.example.tradeservice.repository.HistoricalDataRepository;
import com.example.tradeservice.service.impl.OrderTrackerImpl;
import com.example.tradeservice.service.impl.PositionTracker;
import com.example.tradeservice.service.impl.TimeSeriesHandler;
import com.ib.client.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.exceptions.JedisDataException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.tradeservice.service.TimeUtils.parseIbTime;


@Slf4j
@Component
@Scope("singleton")
public class TWSConnectionManager implements EWrapper {
    private EClientSocket client;
    private final EReaderSignal readerSignal = new EJavaSignal();
    private EReader reader;
    private PositionTracker positionTracker;
    private OrderTrackerImpl orderTracker;
    private StrategyService strategyService;
    private CountDownLatch connectionLatch;
    private final TwsResultHandler twsResultHandler;
    private final AtomicInteger autoIncrement = new AtomicInteger();
    private String managedAccount;
    private final HistoricalDataRepository historicalDataRepository;
    private final DataRequestRepository dataRequestRepository;
    private final AccountService accountService;
    private final TimeSeriesHandler timeSeriesHandler;

    // Connection parameters
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 7497; // Paper trading port (7496 for live)
    private static final int CLIENT_ID = 0;
    private final ContractRepository contractRepository;

    public TWSConnectionManager(PositionTracker positionTracker,
                                AccountService accountService,
                                OrderTrackerImpl orderTracker,
                                StrategyService strategyService,
                                HistoricalDataRepository historicalDataRepository,
                                ContractRepository contractRepository,
                                DataRequestRepository dataRequestRepository, TimeSeriesHandler timeSeriesHandler) {
        this.dataRequestRepository = dataRequestRepository;
        this.timeSeriesHandler = timeSeriesHandler;
        this.client = new EClientSocket(this, readerSignal);
        this.positionTracker = positionTracker;
        this.orderTracker = orderTracker;
        this.strategyService = strategyService;
        this.accountService = accountService;
        this.connectionLatch = new CountDownLatch(1);
        this.twsResultHandler = new TwsResultHandler();
        this.historicalDataRepository = historicalDataRepository;
        this.contractRepository = contractRepository;
    }

    @PostConstruct
    private void connect() throws InterruptedException {
        try {
            client.eConnect(HOST, PORT, CLIENT_ID);

            log.info("Client is connected " + client.isConnected());

            final EReader reader = new EReader(client, readerSignal);
            reader.start();

            // An additional thread is created in this program design to empty the messaging
            // queue
            new Thread(() -> {
                while (client.isConnected()) {
                    readerSignal.waitForSignal();
                    try {
                        reader.processMsgs();
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
            }).start();

            Thread.sleep(2000); // avoid "Ignoring API request 'jextend.cs' since API is not accepted." error

            log.info("Connected to TWS successfully!");

            client.reqPositions(); // subscribe to positions
            client.reqAutoOpenOrders(true); // subscribe to order changes
            client.reqAllOpenOrders(); // initial request for open orders

            // Request account summary
            client.reqAccountSummary(CLIENT_ID, "All", "TotalCashValue,NetLiquidation,TotalCashBalance,NetDividend,CashBalance");

            // Request all positions
            client.reqPositions();

            // Request all open orders
            client.reqOpenOrders();

            //request all P&L for current positions
                client.reqPnLSingle(autoIncrement.getAndIncrement(), this.managedAccount, "", 265598);

            client.getTwsConnectionTime();

            orderTracker.setClient(client);

            strategyService.setClient(client);

            //test subscribe to market data



        } catch (Exception e) {
            log.error("Error connecting to TWS: {}", e.getMessage());
//            throw new RuntimeException(e);
        }
    }

    public void requestExistingData() {
        log.info("Requesting existing positions and orders...");

        // Request account summary
        client.reqAccountSummary(9001, "All", "TotalCashValue,NetLiquidation");

        // Request all positions
        client.reqPositions();

        // Request all open orders
        client.reqAllOpenOrders();

        //request all P&L for current positions
        client.reqPnLSingle(autoIncrement.getAndIncrement(), this.managedAccount, "", 265598);

        // Request executed trades for today
//        ExecutionFilter filter = new ExecutionFilter();
//        client.reqExecutions(9003, filter);
    }

    public void disconnect() {
        if (client != null && client.isConnected()) {
            log.info("Disconnecting from TWS...");
            client.eDisconnect();
        }
    }

    // EWrapper implementation - Connection events
    @Override
    public void connectAck() {
        if (client.isAsyncEConnect()) {
            log.info("Acknowledging connection");
            connectionLatch.countDown();
            client.startAPI();
        }
    }

    @Override
    public void connectionClosed() {
        log.info("Connection closed");
    }

    @Override
    public void error(Exception e) {
        System.err.println("Error: " + e.getMessage());
    }

    @Override
    public void error(String str) {
        System.err.println("Error: " + str);
    }

    @Override
    public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        log.error("Error id: {}; Code: {}: {}", id, errorCode, errorMsg);
        twsResultHandler.setResult(id, new TwsResultHolder("Error code: " + errorCode + "; " + errorMsg));
    }

    // Account and Portfolio callbacks
    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        log.info("accountSummaryEndReqId: " + reqId + ", Acct: " + account + ", Tag: " + tag + ", Value: " + value
                + ", Currency: " + currency);
        accountService.setAccount("Acct", account);
        accountService.setAccount(tag, value);
    }

    @Override
    public void accountSummaryEnd(int reqId) {
        log.info("AccountSummaryEnd. Req Id: " + reqId + "\n");
    }

    @Override
    public void verifyMessageAPI(String s) {
        log.info("verifyMessageAPI");
    }

    @Override
    public void verifyCompleted(boolean b, String s) {
        log.info("verifyCompleted");
    }

    @Override
    public void verifyAndAuthMessageAPI(String s, String s1) {
        log.info("verifyAndAuthMessageAPI");
    }

    @Override
    public void verifyAndAuthCompleted(boolean b, String s) {
        log.info("verifyAndAuthCompleted");
    }

    @Override
    public void displayGroupList(int reqId, String groups) {
        log.info("Display Group List. ReqId: " + reqId + ", Groups: " + groups + "\n");
    }

    @Override
    public void displayGroupUpdated(int reqId, String contractInfo) {
        log.info("Display Group Updated. ReqId: " + reqId + ", Contract info: " + contractInfo + "\n");
    }

    @Override
    public void position(String account, Contract contract, Decimal position, double avgCost) {
        positionTracker.addPosition(new PositionHolder(contract, position, avgCost));
        int reqId = autoIncrement.getAndIncrement();
        positionTracker.createDataRequest(reqId, contract, "3 D", "5 mins");
        client.reqHistoricalData(reqId, contract, "", "3 D", "5 mins",
                "TRADES", 1, 1, false, null);
    }

    @Override
    public void positionEnd() {
        log.info("All Position list retrieved");
        positionTracker.getAllPositions();
    }

    // Order callbacks
    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        orderTracker.setOrder(contract, order, orderState);
    }

    @Override
    public void openOrderEnd() {
        log.info("Order list retrieved");
//        orderTracker.getAllOrders();
    }

    @Override
    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining,
                            double avgFillPrice, int permId, int parentId, double lastFillPrice,
                            int clientId, String whyHeld, double mktCapPrice) {
        orderTracker.updateOrderStatus(orderId, status, filled, remaining, avgFillPrice);
    }

    @Override
    public void execDetails(int reqId, Contract contract, Execution execution) {
        log.info("Execution: " + execution.side() + " " + execution.shares() +
                " " + contract.symbol() + " @ " + execution.price());
    }

    @Override
    public void execDetailsEnd(int reqId) {
        log.info("All executions received");
    }

    // Required empty implementations
    @Override
    public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {
        TickType tickType = TickType.get(field);
        if (Set.of(TickType.ASK, TickType.BID).contains(tickType)) {
//          some Redis stuff
            timeSeriesHandler.addToStream(tickerId, price, tickType);
            log.debug("Tick added to stream {}: {}", tickType, price);
        } else {
            log.debug("Skip tick type {}", tickType);
        }
    }

    @Override
    public void tickSize(int tickerId, int field, Decimal size) {
        TickType tickType = TickType.get(field);
    }

    @Override
    public void tickOptionComputation(int tickerId, int field, int tickAttrib, double impliedVol,
                                      double delta, double optPrice, double pvDividend,
                                      double gamma, double vega, double theta, double undPrice) {
        log.info("TickOptionComputation. TickerId: " + tickerId + ", field: " + field + ", ImpliedVolatility: "
                + impliedVol + ", Delta: " + delta
                + ", OptionPrice: " + optPrice + ", pvDividend: " + pvDividend + ", Gamma: " + gamma + ", Vega: " + vega
                + ", Theta: " + theta + ", UnderlyingPrice: " + undPrice);
    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        log.info("Tick Generic. Ticker Id:" + tickerId + ", Field: " + TickType.getField(tickType) + ", Value: "
                + value);
    }

    @Override
    public void tickString(int tickerId, int tickType, String value) {
        TickType type = TickType.get(tickType);
    }

    @Override
    public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints,
                        double impliedFuture, int holdDays, String futureLastTradeDate,
                        double dividendImpact, double dividendsToLastTradeDate) {
        log.info("TickEFP. " + tickerId + ", Type: " + tickType + ", BasisPoints: " + basisPoints
                + ", FormattedBasisPoints: " +
                formattedBasisPoints + ", ImpliedFuture: " + impliedFuture + ", HoldDays: " + holdDays
                + ", FutureLastTradeDate: " + futureLastTradeDate +
                ", DividendImpact: " + dividendImpact + ", DividendsToLastTradeDate: " + dividendsToLastTradeDate);
    }

    @Override
    public void tickSnapshotEnd(int reqId) {
        log.info("TickSnapshotEnd: " + reqId);
    }

    @Override
    public void marketDataType(int reqId, int marketDataType) {
        log.info("MarketDataType. [" + reqId + "], Type: [" + marketDataType + "]\n");
    }

    @Override
    public void nextValidId(int orderId) {
        log.info("Next valid order ID: " + orderId);
        this.orderTracker.setOrderId(orderId);
    }

    @Override
    public void managedAccounts(String accountsList) {
        this.managedAccount = accountsList;
        log.info("Managed accounts: " + accountsList);
    }

    @Override
    public void updateAccountValue(String key, String value, String currency, String accountName) {
        log.info("UpdateAccountValue. Key: " + key + ", Value: " + value + ", Currency: " + currency + ", AccountName: "
                + accountName);
    }

    @Override
    public void updatePortfolio(Contract contract, Decimal position, double marketPrice,
                                double marketValue, double averageCost, double unrealizedPNL,
                                double realizedPNL, String accountName) {
        log.info("UpdatePortfolio. " + contract.symbol() + ", " + contract.secType() + " @ " + contract.exchange()
                + ": Position: " + position + ", MarketPrice: " + marketPrice + ", MarketValue: " + marketValue
                + ", AverageCost: " + averageCost
                + ", UnrealizedPNL: " + unrealizedPNL + ", RealizedPNL: " + realizedPNL + ", AccountName: "
                + accountName);
    }

    @Override
    public void updateAccountTime(String timeStamp) {
    }

    @Override
    public void accountDownloadEnd(String accountName) {
    }

    @Override
    public void contractDetails(int reqId, ContractDetails contractDetails) {
        twsResultHandler.setResult(reqId, new TwsResultHolder<>(contractDetails));
    }

    @Override
    public void bondContractDetails(int reqId, ContractDetails contractDetails) {
        log.info(EWrapperMsgGenerator.bondContractDetails(reqId, contractDetails));
    }

    @Override
    public void contractDetailsEnd(int reqId) {
        log.info("ContractDetailsEnd. " + reqId + "\n");
    }

    @Override
    public void updateMktDepth(int tickerId, int position, int operation, int side,
                               double price, Decimal size) {
        log.info("UpdateMarketDepth. " + tickerId + " - Position: " + position + ", Operation: " + operation
                + ", Side: " + side + ", Price: " + price + ", Size: " + size + "");
    }

    @Override
    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation,
                                 int side, double price, Decimal size, boolean isSmartDepth) {
    }

    @Override
    public void updateNewsBulletin(int msgId, int msgType, String newsMessage, String originExch) {
    }

    @Override
    public void receiveFA(int faData, String cxml) {
    }

    @Override
    public void historicalData(int reqId, Bar bar) {
//        log.info("HistoricalData. " + reqId + " - Date: " + bar.time() + ", Open: " + bar.open() + ", High: "
//                + bar.high() + ", Low: " + bar.low() + ", Close: " + bar.close() + ", Volume: " + bar.volume()
//                + ", Count: " + bar.count() + ", WAP: " + bar.wap());

        //Move this to corresponding service
        DataRequest request = dataRequestRepository.findByReqId(reqId)
                .orElseThrow(() -> new RuntimeException("DataRequest not found for reqId: " + reqId));


        // Convert IB bar to entity
        HistoricalData data = HistoricalData.builder()
                .position(request.getPosition())
                .timestamp(parseIbTime(bar.time()))
                .timeframe(TimeFrame.FIVE_MIN)
                .open(BigDecimal.valueOf(bar.open()))
                .high(BigDecimal.valueOf(bar.high()))
                .low(BigDecimal.valueOf(bar.low()))
                .close(BigDecimal.valueOf(bar.close()))
                .volume(bar.volume().longValue())
                .count(bar.count())
                .wap(bar.wap().value())
                .build();

        // Save historical data (ON DUPLICATE KEY UPDATE if using MySQL)
        try {
            historicalDataRepository.save(data);
        } catch (DataIntegrityViolationException e) {
            // Handle duplicate - update existing record
            log.debug("Duplicate bar data for reqId={}, timestamp={}", reqId, data.getTimestamp());
        }

        log.debug("Historical data saved: reqId={}, timestamp={}, close={}",
                reqId, data.getTimestamp(), data.getClose());

    }

    @Override
    public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
        log.info("HistoricalDataEnd. " + reqId + " - Start Date: " + startDateStr + ", End Date: " + endDateStr);
    }

    @Override
    public void scannerParameters(String xml) {
        log.info("ScannerParameters. " + xml + "\n");
    }

    @Override
    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance,
                            String benchmark, String projection, String legsStr) {
        log.info("ScannerData. " + reqId + " - Rank: " + rank + ", Symbol: " + contractDetails.contract().symbol()
                + ", SecType: " + contractDetails.contract().secType() + ", Currency: "
                + contractDetails.contract().currency()
                + ", Distance: " + distance + ", Benchmark: " + benchmark + ", Projection: " + projection
                + ", Legs String: " + legsStr);
    }

    @Override
    public void scannerDataEnd(int reqId) {
        log.info("ScannerDataEnd. " + reqId);
    }

    @Override
    public void realtimeBar(int reqId, long time, double open, double high, double low, double close,
                            Decimal volume, Decimal wap, int count) {
        log.info("RealTimeBars. " + reqId + " - Time: " + time + ", Open: " + open + ", High: " + high + ", Low: " + low
                + ", Close: " + close + ", Volume: " + volume + ", Count: " + count + ", WAP: " + wap);
    }

    @Override
    public void currentTime(long time) {
        log.info("currentTime");
    }

    @Override
    public void fundamentalData(int reqId, String data) {
        log.info("FundamentalData. ReqId: [" + reqId + "] - Data: [" + data + "]");
    }

    @Override
    public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {
        log.info("deltaNeutralValidation");
    }

    @Override
    public void commissionReport(CommissionReport commissionReport) {
        log.info("CommissionReport. [" + commissionReport.execId() + "] - [" + commissionReport.commission() + "] ["
                + commissionReport.currency() + "] RPNL [" + commissionReport.realizedPNL() + "]");
    }

    @Override
    public void positionMulti(int reqId, String account, String modelCode, Contract contract,
                              Decimal pos, double avgCost) {
        log.info("Position Multi. Request: " + reqId + ", Account: " + account + ", ModelCode: " + modelCode
                + ", Symbol: " + contract.symbol() + ", SecType: " + contract.secType() + ", Currency: "
                + contract.currency() + ", Position: " + pos + ", Avg cost: " + avgCost + "\n");
    }

    @Override
    public void positionMultiEnd(int reqId) {
        log.info("Position Multi End. Request: " + reqId + "\n");
    }

    @Override
    public void accountUpdateMulti(int reqId, String account, String modelCode, String key,
                                   String value, String currency) {
        log.info("Account Update Multi. Request: " + reqId + ", Account: " + account + ", ModelCode: " + modelCode
                + ", Key: " + key + ", Value: " + value + ", Currency: " + currency + "\n");
    }

    @Override
    public void accountUpdateMultiEnd(int reqId) {
        log.info("Account Update Multi End. Request: " + reqId + "\n");
    }

    @Override
    public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId,
                                                    String tradingClass, String multiplier,
                                                    java.util.Set<String> expirations,
                                                    java.util.Set<Double> strikes) {
//        ContractHolder underlyingContractHolder = contractRepository.findById(underlyingConId).orElseGet(() -> {
//            TwsResultHolder<ContractHolder> holder = requestContractByConid(underlyingConId);
//            return holder.getResult();
//        });
//
//        for (Types.Right right : List.of(Types.Right.Call, Types.Right.Put)) {
//            for (String expiration : expirations) {
//                for (Double strike : strikes) {
//                    String optionSymbol = underlyingContractHolder.getContract().symbol() + " " + expiration + " " + right;
//                    Option option = new Option(optionSymbol, expiration, strike, right);
//                    underlyingContractHolder.getOptionChain().add(option);
//                }
//            }
//        }
//
//        underlyingContractHolder.setOptionChainRequestId(reqId);
//        contractRepository.save(underlyingContractHolder);
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int reqId) {

//        ContractHolder underlying = contractRepository.findContractHolderByOptionChainRequestId(reqId);
//        if (underlying != null && !CollectionUtils.isEmpty(underlying.getOptionChain())) {
//            twsResultHandler.setResult(reqId, new TwsResultHolder<>(underlying.getOptionChain()));
//        }
//        log.debug("Option chain retrieved: {}", underlying.getOptionChain());
    }

    @Override
    public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {
        for (SoftDollarTier tier : tiers) {
            log.info("tier: " + tier.toString() + ", ");
        }
    }

    @Override
    public void familyCodes(FamilyCode[] familyCodes) {
        for (FamilyCode fc : familyCodes) {
            log.info("Family Code. AccountID: " + fc.accountID() + ", FamilyCode: " + fc.familyCodeStr());
        }
    }

    @Override
    public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {
        List<Contract> resultList = new ArrayList<>();
        for (ContractDescription cd : contractDescriptions) {
            resultList.add(cd.contract());
        }
        twsResultHandler.setResult(reqId, new TwsResultHolder(resultList));
    }

    @Override
    public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {
        for (DepthMktDataDescription depthMktDataDescription : depthMktDataDescriptions) {
            log.info("Depth Mkt Data Description. Exchange: " + depthMktDataDescription.exchange() +
                    ", ListingExch: " + depthMktDataDescription.listingExch() +
                    ", SecType: " + depthMktDataDescription.secType() +
                    ", ServiceDataType: " + depthMktDataDescription.serviceDataType() +
                    ", AggGroup: " + depthMktDataDescription.aggGroup());
        }
    }

    @Override
    public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId,
                         String headline, String extraData) {
        log.info("Tick News. TickerId: " + tickerId + ", TimeStamp: " + timeStamp + ", ProviderCode: " + providerCode
                + ", ArticleId: " + articleId + ", Headline: " + headline + ", ExtraData: " + extraData + "\n");
    }

    @Override
    public void smartComponents(int reqId, java.util.Map<Integer, java.util.Map.Entry<String, Character>> theMap) {
        log.info("smart components req id:" + reqId);

        for (Map.Entry<Integer, Map.Entry<String, Character>> item : theMap.entrySet()) {
            log.info("bit number: " + item.getKey() +
                    ", exchange: " + item.getValue().getKey() + ", exchange letter: " + item.getValue().getValue());
        }
    }

    @Override
    public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
        log.info("Tick req params. Ticker Id:" + tickerId + ", Min tick: " + minTick + ", bbo exchange: " + bboExchange
                + ", Snapshot permissions: " + snapshotPermissions);
    }

    @Override
    public void newsProviders(NewsProvider[] newsProviders) {
        for (NewsProvider np : newsProviders) {
            log.info("News Provider. Provider Code: " + np.providerCode() + ", Provider Name: " + np.providerName()
                    + "\n");
        }
    }

    @Override
    public void newsArticle(int requestId, int articleType, String articleText) {
        log.info("News Article. Request Id: " + requestId + ", Article Type: " + articleType + ", Article Text: "
                + articleText + "\n");
    }

    @Override
    public void historicalNews(int requestId, String time, String providerCode, String articleId,
                               String headline) {
        log.info("Historical News. Request Id: " + requestId + ", Time: " + time + ", Provider Code: " + providerCode
                + ", Article Id: " + articleId + ", Headline: " + headline + "\n");
    }

    @Override
    public void historicalNewsEnd(int requestId, boolean hasMore) {
        log.info("Historical News End. Request Id: " + requestId + ", Has more: " + hasMore + "\n");
    }

    @Override
    public void headTimestamp(int reqId, String headTimestamp) {
        log.info("Head Timestamp. Request Id: " + reqId + ", Head Timestamp: " + headTimestamp + "\n");
    }

    @Override
    public void histogramData(int reqId, java.util.List<HistogramEntry> items) {
        log.info("Histogram Data. Request Id: " + reqId + ", Items: " + items + "\n");
    }

    @Override
    public void historicalDataUpdate(int reqId, Bar bar) {
        log.info("HistoricalDataUpdate. " + reqId + " - Date: " + bar.time() + ", Open: " + bar.open() + ", High: "
                + bar.high() + ", Low: " + bar.low() + ", Close: " + bar.close() + ", Volume: " + bar.volume()
                + ", Count: " + bar.count() + ", WAP: " + bar.wap());
    }

    @Override
    public void rerouteMktDataReq(int reqId, int conId, String exchange) {
        log.info(EWrapperMsgGenerator.rerouteMktDataReq(reqId, conId, exchange));
    }

    @Override
    public void rerouteMktDepthReq(int reqId, int conId, String exchange) {
        log.info(EWrapperMsgGenerator.rerouteMktDepthReq(reqId, conId, exchange));
    }

    @Override
    public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {
        DecimalFormat df = new DecimalFormat("#.#");
        df.setMaximumFractionDigits(340);
        log.info("Market Rule Id: " + marketRuleId);
        for (PriceIncrement pi : priceIncrements) {
            log.info("Price Increment. Low Edge: " + df.format(pi.lowEdge()) + ", Increment: "
                    + df.format(pi.increment()));
        }
    }

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
        log.info(EWrapperMsgGenerator.pnl(reqId, dailyPnL, unrealizedPnL, realizedPnL));
    }

    @Override
    public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL,
                          double realizedPnL, double value) {
        log.info(EWrapperMsgGenerator.pnlSingle(reqId, pos, dailyPnL, unrealizedPnL, realizedPnL, value));
        accountService.setAccount("pos", pos.toString());
        accountService.setAccount("dailyPnL", String.valueOf(dailyPnL));
        accountService.setAccount("unrealizedPnL", String.valueOf(unrealizedPnL));
        accountService.setAccount("realizedPnL", String.valueOf(realizedPnL));
        accountService.setAccount("value", String.valueOf(value));
    }

    @Override
    public void historicalTicks(int reqId, java.util.List<HistoricalTick> ticks, boolean done) {
        for (HistoricalTick tick : ticks) {
            log.info(EWrapperMsgGenerator.historicalTick(reqId, tick.time(), tick.price(), tick.size()));
        }
    }

    @Override
    public void historicalTicksBidAsk(int reqId, java.util.List<HistoricalTickBidAsk> ticks, boolean done) {
        for (HistoricalTickBidAsk tick : ticks) {
            log.info(EWrapperMsgGenerator.historicalTickBidAsk(reqId, tick.time(), tick.tickAttribBidAsk(),
                    tick.priceBid(), tick.priceAsk(), tick.sizeBid(),
                    tick.sizeAsk()));
        }
    }

    @Override
    public void historicalTicksLast(int reqId, java.util.List<HistoricalTickLast> ticks, boolean done) {
        for (HistoricalTickLast tick : ticks) {
            log.info(EWrapperMsgGenerator.historicalTickLast(reqId, tick.time(), tick.tickAttribLast(), tick.price(),
                    tick.size(), tick.exchange(),
                    tick.specialConditions()));
        }
    }

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size,
                                  TickAttribLast tickAttribLast, String exchange, String specialConditions) {
        log.info(EWrapperMsgGenerator.tickByTickAllLast(reqId, tickType, time, price, size, tickAttribLast, exchange,
                specialConditions));
    }

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice,
                                 Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {
        timeSeriesHandler.addToStream(reqId, bidPrice, TickType.BID);
        timeSeriesHandler.addToStream(reqId, askPrice, TickType.ASK);
    }

    @Override
    public void tickByTickMidPoint(int reqId, long time, double midPoint) {
        log.info(EWrapperMsgGenerator.tickByTickMidPoint(reqId, time, midPoint));
    }

    @Override
    public void orderBound(long orderId, int apiClientId, int apiOrderId) {
        log.info(EWrapperMsgGenerator.orderBound(orderId, apiClientId, apiOrderId));
    }

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {
        log.info(EWrapperMsgGenerator.completedOrder(contract, order, orderState));
    }

    @Override
    public void completedOrdersEnd() {
        log.info(EWrapperMsgGenerator.completedOrdersEnd());
    }

    @Override
    public void replaceFAEnd(int reqId, String text) {
    }

    @Override
    public void wshMetaData(int reqId, String dataJson) {
    }

    @Override
    public void wshEventData(int reqId, String dataJson) {
    }

    @Override
    public void historicalSchedule(int reqId, String startDateTime, String endDateTime,
                                   String timeZone, java.util.List<HistoricalSession> sessions) {
    }

    @Override
    public void userInfo(int reqId, String whiteBrandingId) {
    }

    public TwsResultHolder<ContractHolder> requestContractByConid(int conid) {
        Contract contract = new Contract();
        contract.conid(conid);
        TwsResultHolder<ContractDetails> contractDetails = requestContractDetails(contract);
        ContractHolder contractHolder = new ContractHolder(contractDetails.getResult().contract());
        contractHolder.setDetails(contractDetails.getResult());
        return new TwsResultHolder<>(contractHolder);
    }

    public TwsResultHolder<ContractDetails> requestContractDetails(Contract contract) {
        final int currentId = autoIncrement.getAndIncrement();
        client.reqContractDetails(currentId, contract);
        TwsResultHolder<ContractDetails> details = twsResultHandler.getResult(currentId);
        Optional<ContractHolder> contractHolder = contractRepository.findById(details.getResult().conid());
        contractHolder.ifPresent(holder -> {
            holder.setDetails(details.getResult());
            // TODO save from ContractManager
            contractRepository.save(holder);
        });
        return details;
    }

    public TwsResultHolder<List<Contract>> searchContract(String search) {
        if (StringUtils.hasLength(search)) {
            final int currentId = autoIncrement.getAndIncrement();
            client.reqMatchingSymbols(currentId, search);
            return twsResultHandler.getResult(currentId);
        }
        return new TwsResultHolder("Search parameter cannot be empty");
    }

    /**
     * Subscribe to market data stream, returns with the stream id.
     * @param contract
     * @param tickData if true, we get tick-by-tick data
     */
    public int subscribeMarketData(Contract contract, boolean tickData) {
        final int currentId = autoIncrement.getAndIncrement();
        Optional<ContractHolder> contractHolderOptional = contractRepository.findById(contract.conid());
        ContractHolder contractHolder = contractHolderOptional.orElse(new ContractHolder(contract));
        contractHolder.setStreamRequestId(currentId);
        contractRepository.save(contractHolder);
        try {
            timeSeriesHandler.createStream(currentId, contract);
        } catch (JedisDataException e) {
            log.error(e.getMessage());
        }
        if (tickData) {
            client.reqTickByTickData(currentId, contract, "BidAsk", 1, false);
        } else {
            client.reqMktData(currentId, contract, "", false, false, null);
        }
        return currentId;
    }

}


