package com.example.tradeservice.service;

import com.example.tradeservice.model.ContractHolder;
import com.example.tradeservice.model.Option;
import com.example.tradeservice.model.PositionHolder;
import com.example.tradeservice.repository.ContractRepository;
import com.ib.client.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Component
@Scope("singleton")
public class TWSConnectionManager implements EWrapper {
    private EClientSocket client;
    private final EReaderSignal readerSignal = new EJavaSignal();
    private EReader reader;
    private PositionTracker positionTracker;
    private OrderTracker orderTracker;
    private CountDownLatch connectionLatch;
    private final TwsResultHandler twsResultHandler;
    private final ContractRepository contractRepository;
    private final AtomicInteger autoIncrement = new AtomicInteger();

    // Connection parameters
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 7497; // Paper trading port (7496 for live)
    private static final int CLIENT_ID = 0;

    public TWSConnectionManager(ContractRepository contractRepository, PositionTracker positionTracker,
                                OrderTracker orderTracker) {
        this.client = new EClientSocket(this, readerSignal);
        this.positionTracker = positionTracker;
        this.orderTracker = orderTracker;
        this.connectionLatch = new CountDownLatch(1);
        this.twsResultHandler = new TwsResultHandler();
        this.contractRepository = contractRepository;
    }

    @PostConstruct
    private void connect() throws InterruptedException {
        client.eConnect(HOST, PORT, CLIENT_ID);

        // Wait for connection
        if (!connectionLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Connection timeout");
        }

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

        // Thread.sleep(2000); // avoid "Ignoring API request 'jextend.cs' since API is not accepted." error

        log.info("Connected to TWS successfully!");

        client.reqPositions(); // subscribe to positions
        client.reqAutoOpenOrders(true); // subscribe to order changes
        client.reqAllOpenOrders(); // initial request for open orders

//        orderManagerService.setClient(clientSocket);
    }

    public void requestExistingData() {
        log.info("Requesting existing positions and orders...");

        // Request account summary
        client.reqAccountSummary(9001, "All", "TotalCashValue,NetLiquidation");

        // Request all positions
        client.reqPositions();

        // Request all open orders
        client.reqAllOpenOrders();

        // Request executed trades for today
        ExecutionFilter filter = new ExecutionFilter();
        client.reqExecutions(9003, filter);
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
        log.info("Acct Summary. ReqId: " + reqId + ", Acct: " + account + ", Tag: " + tag + ", Value: " + value
                + ", Currency: " + currency);
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
//        positionTracker.updatePosition(account, contract, position, avgCost);
    }

    @Override
    public void positionEnd() {
        log.info("All Position list retrieved");
//        positionTracker.printPositions();
    }

    // Order callbacks
    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
//        orderTracker.updateOrder(orderId, contract, order, orderState);
    }

    @Override
    public void openOrderEnd() {
        log.info("Order list retrieved");
//        orderTracker.printOrders();
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
//            timeSeriesHandler.addToStream(tickerId, price, tickType);
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
        twsResultHandler.setResult(reqId, new TwsResultHolder<ContractDetails>(contractDetails));
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
        log.info("HistoricalData. " + reqId + " - Date: " + bar.time() + ", Open: " + bar.open() + ", High: "
                + bar.high() + ", Low: " + bar.low() + ", Close: " + bar.close() + ", Volume: " + bar.volume()
                + ", Count: " + bar.count() + ", WAP: " + bar.wap());
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
        ContractHolder underlyingContractHolder = contractRepository.findById(underlyingConId).orElseGet(() -> {
            TwsResultHolder<ContractHolder> holder = requestContractByConid(underlyingConId);
            return holder.getResult();
        });

        for (Types.Right right : List.of(Types.Right.Call, Types.Right.Put)) {
            for (String expiration : expirations) {
                for (Double strike : strikes) {
                    String optionSymbol = underlyingContractHolder.getContract().symbol() + " " + expiration + " " + right;
                    Option option = new Option(optionSymbol, expiration, strike, right);
                    underlyingContractHolder.getOptionChain().add(option);
                }
            }
        }

        underlyingContractHolder.setOptionChainRequestId(reqId);
        contractRepository.save(underlyingContractHolder);
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int reqId) {

        ContractHolder underlying = contractRepository.findContractHolderByOptionChainRequestId(reqId);
        if (underlying != null && !CollectionUtils.isEmpty(underlying.getOptionChain())) {
            twsResultHandler.setResult(reqId, new TwsResultHolder<>(underlying.getOptionChain()));
        }
        log.debug("Option chain retrieved: {}", underlying.getOptionChain());
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
    }

    @Override
    public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {
    }

    @Override
    public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId,
                         String headline, String extraData) {
    }

    @Override
    public void smartComponents(int reqId, java.util.Map<Integer, java.util.Map.Entry<String, Character>> theMap) {
    }

    @Override
    public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
    }

    @Override
    public void newsProviders(NewsProvider[] newsProviders) {
    }

    @Override
    public void newsArticle(int requestId, int articleType, String articleText) {
    }

    @Override
    public void historicalNews(int requestId, String time, String providerCode, String articleId,
                               String headline) {
    }

    @Override
    public void historicalNewsEnd(int requestId, boolean hasMore) {
    }

    @Override
    public void headTimestamp(int reqId, String headTimestamp) {
    }

    @Override
    public void histogramData(int reqId, java.util.List<HistogramEntry> items) {
    }

    @Override
    public void historicalDataUpdate(int reqId, Bar bar) {
    }

    @Override
    public void rerouteMktDataReq(int reqId, int conid, String exchange) {
    }

    @Override
    public void rerouteMktDepthReq(int reqId, int conid, String exchange) {
    }

    @Override
    public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {
    }

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
    }

    @Override
    public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL,
                          double realizedPnL, double value) {
    }

    @Override
    public void historicalTicks(int reqId, java.util.List<HistoricalTick> ticks, boolean done) {
    }

    @Override
    public void historicalTicksBidAsk(int reqId, java.util.List<HistoricalTickBidAsk> ticks, boolean done) {
    }

    @Override
    public void historicalTicksLast(int reqId, java.util.List<HistoricalTickLast> ticks, boolean done) {
    }

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size,
                                  TickAttribLast tickAttribLast, String exchange, String specialConditions) {
    }

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice,
                                 Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {
    }

    @Override
    public void tickByTickMidPoint(int reqId, long time, double midPoint) {
    }

    @Override
    public void orderBound(long orderId, int apiClientId, int apiOrderId) {
    }

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {
    }

    @Override
    public void completedOrdersEnd() {
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

}


