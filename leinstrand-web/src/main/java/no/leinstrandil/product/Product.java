package no.leinstrandil.product;

public class Product {

    private String productCode;
    private String description;
    private int unitPrice;
    private int discountInPercent;

    public Product(String productCode, String description, int unitPrice, int discountInPercent) {
        this.discountInPercent = discountInPercent;
        this.productCode = productCode;
        this.description = description;
        this.unitPrice = unitPrice;
    }

    public String getDescription() {
        return description;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getDiscountInPercent() {
        return discountInPercent;
    }

}
