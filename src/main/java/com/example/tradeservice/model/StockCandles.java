package com.example.tradeservice.model;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/**
 * StockCandles
 */

public class StockCandles {
    @Getter
    @SerializedName("o")
    private List<Float> o = null;

    @Getter
    @SerializedName("h")
    private List<Float> h = null;

    @Getter
    @SerializedName("l")
    private List<Float> l = null;

    @Getter
    @SerializedName("c")
    private List<Float> c = null;

    @Getter
    @SerializedName("v")
    private List<Float> v = null;

    @Getter
    @SerializedName("t")
    private List<Long> t = null;

    @Getter
    @SerializedName("s")
    private String s = null;

    public StockCandles o(List<Float> o) {
        this.o = o;
        return this;
    }

    public StockCandles addOItem(Float oItem) {
        if (this.o == null) {
            this.o = new ArrayList<Float>();
        }
        this.o.add(oItem);
        return this;
    }

    public void setO(List<Float> o) {
        this.o = o;
    }

    public StockCandles h(List<Float> h) {
        this.h = h;
        return this;
    }

    public StockCandles addHItem(Float hItem) {
        if (this.h == null) {
            this.h = new ArrayList<Float>();
        }
        this.h.add(hItem);
        return this;
    }

    public void setH(List<Float> h) {
        this.h = h;
    }

    public StockCandles l(List<Float> l) {
        this.l = l;
        return this;
    }

    public StockCandles addLItem(Float lItem) {
        if (this.l == null) {
            this.l = new ArrayList<Float>();
        }
        this.l.add(lItem);
        return this;
    }

    public void setL(List<Float> l) {
        this.l = l;
    }

    public StockCandles c(List<Float> c) {
        this.c = c;
        return this;
    }

    public StockCandles addCItem(Float cItem) {
        if (this.c == null) {
            this.c = new ArrayList<Float>();
        }
        this.c.add(cItem);
        return this;
    }

    public void setC(List<Float> c) {
        this.c = c;
    }

    public StockCandles v(List<Float> v) {
        this.v = v;
        return this;
    }

    public StockCandles addVItem(Float vItem) {
        if (this.v == null) {
            this.v = new ArrayList<Float>();
        }
        this.v.add(vItem);
        return this;
    }

    public void setV(List<Float> v) {
        this.v = v;
    }

    public StockCandles t(List<Long> t) {
        this.t = t;
        return this;
    }

    public StockCandles addTItem(Long tItem) {
        if (this.t == null) {
            this.t = new ArrayList<Long>();
        }
        this.t.add(tItem);
        return this;
    }

    public void setT(List<Long> t) {
        this.t = t;
    }

    public StockCandles s(String s) {
        this.s = s;
        return this;
    }

    /**
     * Status of the response. This field can either be ok or no_data.
     * @return s
     **/
    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StockCandles stockCandles = (StockCandles) o;
        return Objects.equals(this.o, stockCandles.o) &&
                Objects.equals(this.h, stockCandles.h) &&
                Objects.equals(this.l, stockCandles.l) &&
                Objects.equals(this.c, stockCandles.c) &&
                Objects.equals(this.v, stockCandles.v) &&
                Objects.equals(this.t, stockCandles.t) &&
                Objects.equals(this.s, stockCandles.s);
    }

    @Override
    public int hashCode() {
        return Objects.hash(o, h, l, c, v, t, s);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class StockCandles {\n");

        sb.append("    o: ").append(toIndentedString(o)).append("\n");
        sb.append("    h: ").append(toIndentedString(h)).append("\n");
        sb.append("    l: ").append(toIndentedString(l)).append("\n");
        sb.append("    c: ").append(toIndentedString(c)).append("\n");
        sb.append("    v: ").append(toIndentedString(v)).append("\n");
        sb.append("    t: ").append(toIndentedString(t)).append("\n");
        sb.append("    s: ").append(toIndentedString(s)).append("\n");
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
