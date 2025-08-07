package com.example.tradeservice.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
public class MarketStatus {

    @Getter
    @SerializedName("exchange")
    private String exchange = null;

    @Getter
    @SerializedName("timezone")
    private String timezone = null;

    @Getter
    @SerializedName("session")
    private String session = null;

    @Getter
    @SerializedName("holiday")
    private String holiday = null;

    @SerializedName("isOpen")
    private Boolean isOpen = null;

    @Getter
    @SerializedName("t")
    private Long t = null;

    public MarketStatus exchange(String exchange) {
        this.exchange = exchange;
        return this;
    }

    public MarketStatus timezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public MarketStatus session(String session) {
        this.session = session;
        return this;
    }

    public MarketStatus holiday(String holiday) {
        this.holiday = holiday;
        return this;
    }

    public MarketStatus isOpen(Boolean isOpen) {
        this.isOpen = isOpen;
        return this;
    }

    public Boolean isIsOpen() {
        return isOpen;
    }

    public MarketStatus t(Long t) {
        this.t = t;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MarketStatus marketStatus = (MarketStatus) o;
        return Objects.equals(this.exchange, marketStatus.exchange) &&
                Objects.equals(this.timezone, marketStatus.timezone) &&
                Objects.equals(this.session, marketStatus.session) &&
                Objects.equals(this.holiday, marketStatus.holiday) &&
                Objects.equals(this.isOpen, marketStatus.isOpen) &&
                Objects.equals(this.t, marketStatus.t);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchange, timezone, session, holiday, isOpen, t);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MarketStatus {\n");

        sb.append("    exchange: ").append(toIndentedString(exchange)).append("\n");
        sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
        sb.append("    session: ").append(toIndentedString(session)).append("\n");
        sb.append("    holiday: ").append(toIndentedString(holiday)).append("\n");
        sb.append("    isOpen: ").append(toIndentedString(isOpen)).append("\n");
        sb.append("    t: ").append(toIndentedString(t)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}
