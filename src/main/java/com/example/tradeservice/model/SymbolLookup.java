package com.example.tradeservice.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class SymbolLookup {
    @SerializedName("result")
    private List<SymbolLookupInfo> result = null;

    @SerializedName("count")
    private Long count = null;

    public SymbolLookup result(List<SymbolLookupInfo> result) {
        this.result = result;
        return this;
    }

    public SymbolLookup addResultItem(SymbolLookupInfo resultItem) {
        if (this.result == null) {
            this.result = new ArrayList<SymbolLookupInfo>();
        }
        this.result.add(resultItem);
        return this;
    }

    public SymbolLookup count(Long count) {
        this.count = count;
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
        SymbolLookup symbolLookup = (SymbolLookup) o;
        return Objects.equals(this.result, symbolLookup.result) &&
                Objects.equals(this.count, symbolLookup.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, count);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SymbolLookup {\n");

        sb.append("    result: ").append(toIndentedString(result)).append("\n");
        sb.append("    count: ").append(toIndentedString(count)).append("\n");
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
