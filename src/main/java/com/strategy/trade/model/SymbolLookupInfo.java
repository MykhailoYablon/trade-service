package com.strategy.trade.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class SymbolLookupInfo {
    @SerializedName("description")
    private String description = null;

    @SerializedName("displaySymbol")
    private String displaySymbol = null;

    @SerializedName("symbol")
    private String symbol = null;

    @SerializedName("type")
    private String type = null;

    public SymbolLookupInfo description(String description) {
        this.description = description;
        return this;
    }

    public SymbolLookupInfo displaySymbol(String displaySymbol) {
        this.displaySymbol = displaySymbol;
        return this;
    }

    public SymbolLookupInfo symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public SymbolLookupInfo type(String type) {
        this.type = type;
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
        SymbolLookupInfo symbolLookupInfo = (SymbolLookupInfo) o;
        return Objects.equals(this.description, symbolLookupInfo.description) &&
                Objects.equals(this.displaySymbol, symbolLookupInfo.displaySymbol) &&
                Objects.equals(this.symbol, symbolLookupInfo.symbol) &&
                Objects.equals(this.type, symbolLookupInfo.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, displaySymbol, symbol, type);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SymbolLookupInfo {\n");

        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    displaySymbol: ").append(toIndentedString(displaySymbol)).append("\n");
        sb.append("    symbol: ").append(toIndentedString(symbol)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
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
