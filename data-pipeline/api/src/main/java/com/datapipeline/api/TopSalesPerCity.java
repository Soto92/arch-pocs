package com.datapipeline.api;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
public class TopSalesPerCity {

    @Id
    private String city;
    private String salesmanName;
    private BigDecimal saleValue;
    private Timestamp windowEnd;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getSalesmanName() {
        return salesmanName;
    }

    public void setSalesmanName(String salesmanName) {
        this.salesmanName = salesmanName;
    }

    public BigDecimal getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(BigDecimal saleValue) {
        this.saleValue = saleValue;
    }

    public Timestamp getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Timestamp windowEnd) {
        this.windowEnd = windowEnd;
    }
}
