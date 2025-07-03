package com.example.tradeservice.service;

import com.example.tradeservice.model.PositionHolder;
import com.ib.client.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
@Scope("singleton")
public class TWSConnectionManager implements EWrapper {
    private EClientSocket client;
    private EReaderSignal readerSignal;
    private EReader reader;
    private PositionTracker positionTracker;
    private OrderTracker orderTracker;
    private CountDownLatch connectionLatch;

    // Connection parameters
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 7497; // Paper trading port (7496 for live)
    private static final int CLIENT_ID = 0;

    public TWSConnectionManager() {
        this.readerSignal = new EJavaSignal();
        this.client = new EClientSocket(this, readerSignal);
        this.positionTracker = new PositionTracker();
        this.orderTracker = new OrderTracker();
        this.connectionLatch = new CountDownLatch(1);
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

        System.out.println("Connected to TWS successfully!");

        client.reqPositions(); // subscribe to positions
        client.reqAutoOpenOrders(true); // subscribe to order changes
        client.reqAllOpenOrders(); // initial request for open orders

//        orderManagerService.setClient(clientSocket);
    }

    public void requestExistingData() {
        System.out.println("Requesting existing positions and orders...");

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
            System.out.println("Disconnecting from TWS...");
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
        System.out.println("Connection closed");
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
//        twsResultHandler.setResult(id, new TwsResultHolder("Error code: " + errorCode + "; " + errorMsg));
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
        System.out.println("All open orders received");
//        orderTracker.printOrders();
    }

    @Override
    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining,
                            double avgFillPrice, int permId, int parentId, double lastFillPrice,
                            int clientId, String whyHeld, double mktCapPrice) {
//        orderTracker.updateOrderStatus(orderId, status, filled, remaining, avgFillPrice);
    }

    @Override
    public void execDetails(int reqId, Contract contract, Execution execution) {
        System.out.println("Execution: " + execution.side() + " " + execution.shares() +
                " " + contract.symbol() + " @ " + execution.price());
    }

    @Override
    public void execDetailsEnd(int reqId) {
        System.out.println("All executions received");
    }

    // Required empty implementations
    @Override
    public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {}

    @Override
    public void tickSize(int tickerId, int field, Decimal size) {}

    @Override
    public void tickOptionComputation(int tickerId, int field, int tickAttrib, double impliedVol,
                                      double delta, double optPrice, double pvDividend,
                                      double gamma, double vega, double theta, double undPrice) {}

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {}

    @Override
    public void tickString(int tickerId, int tickType, String value) {}

    @Override
    public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints,
                        double impliedFuture, int holdDays, String futureLastTradeDate,
                        double dividendImpact, double dividendsToLastTradeDate) {}

    @Override
    public void tickSnapshotEnd(int reqId) {}

    @Override
    public void marketDataType(int i, int i1) {

    }

    @Override
    public void nextValidId(int orderId) {
        System.out.println("Next valid order ID: " + orderId);
    }

    @Override
    public void managedAccounts(String accountsList) {
        System.out.println("Managed accounts: " + accountsList);
    }

    @Override
    public void updateAccountValue(String key, String val, String currency, String accountName) {}

    @Override
    public void updatePortfolio(Contract contract, Decimal position, double marketPrice,
                                double marketValue, double averageCost, double unrealizedPNL,
                                double realizedPNL, String accountName) {}

    @Override
    public void updateAccountTime(String timeStamp) {}

    @Override
    public void accountDownloadEnd(String accountName) {}

    @Override
    public void contractDetails(int reqId, ContractDetails contractDetails) {}

    @Override
    public void bondContractDetails(int reqId, ContractDetails contractDetails) {}

    @Override
    public void contractDetailsEnd(int reqId) {}

    @Override
    public void updateMktDepth(int tickerId, int position, int operation, int side,
                               double price, Decimal size) {}

    @Override
    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation,
                                 int side, double price, Decimal size, boolean isSmartDepth) {}

    @Override
    public void updateNewsBulletin(int msgId, int msgType, String newsMessage, String originExch) {}

    @Override
    public void receiveFA(int faData, String cxml) {}

    @Override
    public void historicalData(int reqId, Bar bar) {}

    @Override
    public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {}

    @Override
    public void scannerParameters(String xml) {}

    @Override
    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance,
                            String benchmark, String projection, String legsStr) {}

    @Override
    public void scannerDataEnd(int reqId) {}

    @Override
    public void realtimeBar(int reqId, long time, double open, double high, double low, double close,
                            Decimal volume, Decimal wap, int count) {}

    @Override
    public void currentTime(long time) {}

    @Override
    public void fundamentalData(int reqId, String data) {}

    @Override
    public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {}

    @Override
    public void commissionReport(CommissionReport commissionReport) {}

    @Override
    public void positionMulti(int reqId, String account, String modelCode, Contract contract,
                              Decimal pos, double avgCost) {}

    @Override
    public void positionMultiEnd(int reqId) {}

    @Override
    public void accountUpdateMulti(int reqId, String account, String modelCode, String key,
                                   String val, String currency) {}

    @Override
    public void accountUpdateMultiEnd(int reqId) {}

    @Override
    public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId,
                                                    String tradingClass, String multiplier,
                                                    java.util.Set<String> expirations,
                                                    java.util.Set<Double> strikes) {}

    @Override
    public void securityDefinitionOptionalParameterEnd(int reqId) {}

    @Override
    public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {}

    @Override
    public void familyCodes(FamilyCode[] familyCodes) {}

    @Override
    public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {}

    @Override
    public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {}

    @Override
    public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId,
                         String headline, String extraData) {}

    @Override
    public void smartComponents(int reqId, java.util.Map<Integer, java.util.Map.Entry<String, Character>> theMap) {}

    @Override
    public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {}

    @Override
    public void newsProviders(NewsProvider[] newsProviders) {}

    @Override
    public void newsArticle(int requestId, int articleType, String articleText) {}

    @Override
    public void historicalNews(int requestId, String time, String providerCode, String articleId,
                               String headline) {}

    @Override
    public void historicalNewsEnd(int requestId, boolean hasMore) {}

    @Override
    public void headTimestamp(int reqId, String headTimestamp) {}

    @Override
    public void histogramData(int reqId, java.util.List<HistogramEntry> items) {}

    @Override
    public void historicalDataUpdate(int reqId, Bar bar) {}

    @Override
    public void rerouteMktDataReq(int reqId, int conid, String exchange) {}

    @Override
    public void rerouteMktDepthReq(int reqId, int conid, String exchange) {}

    @Override
    public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {}

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {}

    @Override
    public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL,
                          double realizedPnL, double value) {}

    @Override
    public void historicalTicks(int reqId, java.util.List<HistoricalTick> ticks, boolean done) {}

    @Override
    public void historicalTicksBidAsk(int reqId, java.util.List<HistoricalTickBidAsk> ticks, boolean done) {}

    @Override
    public void historicalTicksLast(int reqId, java.util.List<HistoricalTickLast> ticks, boolean done) {}

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size,
                                  TickAttribLast tickAttribLast, String exchange, String specialConditions) {}

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice,
                                 Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {}

    @Override
    public void tickByTickMidPoint(int reqId, long time, double midPoint) {}

    @Override
    public void orderBound(long orderId, int apiClientId, int apiOrderId) {}

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {}

    @Override
    public void completedOrdersEnd() {}

    @Override
    public void replaceFAEnd(int reqId, String text) {}

    @Override
    public void wshMetaData(int reqId, String dataJson) {}

    @Override
    public void wshEventData(int reqId, String dataJson) {}

    @Override
    public void historicalSchedule(int reqId, String startDateTime, String endDateTime,
                                   String timeZone, java.util.List<HistoricalSession> sessions) {}

    @Override
    public void userInfo(int reqId, String whiteBrandingId) {}
}
