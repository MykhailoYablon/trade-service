package com.strategy.trade.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Quote {

    @SerializedName("o")
    private Float o = null;

    @SerializedName("h")
    private Float h = null;

    @SerializedName("l")
    private Float l = null;

    @SerializedName("c")
    private Float c = null;

    @SerializedName("pc")
    private Float pc = null;

    @SerializedName("d")
    private Float d = null;

    @SerializedName("dp")
    private Float dp = null;

    public Quote o(Float o) {
        this.o = o;
        return this;
    }

    public Quote h(Float h) {
        this.h = h;
        return this;
    }

    public Quote l(Float l) {
        this.l = l;
        return this;
    }

    public Quote c(Float c) {
        this.c = c;
        return this;
    }

    public Quote pc(Float pc) {
        this.pc = pc;
        return this;
    }

    public Quote d(Float d) {
        this.d = d;
        return this;
    }

    public Quote dp(Float dp) {
        this.dp = dp;
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
        Quote quote = (Quote) o;
        return Objects.equals(this.o, quote.o) &&
                Objects.equals(this.h, quote.h) &&
                Objects.equals(this.l, quote.l) &&
                Objects.equals(this.c, quote.c) &&
                Objects.equals(this.pc, quote.pc) &&
                Objects.equals(this.d, quote.d) &&
                Objects.equals(this.dp, quote.dp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(o, h, l, c, pc, d, dp);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Quote {\n");

        sb.append("    o: ").append(toIndentedString(o)).append("\n");
        sb.append("    h: ").append(toIndentedString(h)).append("\n");
        sb.append("    l: ").append(toIndentedString(l)).append("\n");
        sb.append("    c: ").append(toIndentedString(c)).append("\n");
        sb.append("    pc: ").append(toIndentedString(pc)).append("\n");
        sb.append("    d: ").append(toIndentedString(d)).append("\n");
        sb.append("    dp: ").append(toIndentedString(dp)).append("\n");
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
