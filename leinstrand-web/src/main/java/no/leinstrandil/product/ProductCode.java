package no.leinstrandil.product;

public enum ProductCode {

    FAMILY_MEMBERSHIP("FAMMEM", ProductType.CLUB_MEMBERSHIP),
    ADULT_MEMBERSHIP("ADUMEM", ProductType.CLUB_MEMBERSHIP),
    YOUTH_MEMBERSHIP("YOUMEM", ProductType.CLUB_MEMBERSHIP),
    TEAM_FEE("TEAMFEE", ProductType.TEAM_FEE);

    private String code;
    private ProductType type;

    private ProductCode(String code, ProductType type) {
        this.code = code;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public ProductType getType() {
        return type;
    }

    public static ProductCode resolve(String code) {
        for(ProductCode pc : values()) {
            if (pc.getCode().equals(code)) {
                return pc;
            }
        }
        return null;
    }

    public static boolean isCodeBelongingToProductOfType(String code, ProductType type) {
        ProductCode pc = resolve(code);
        if (pc == null) {
            return false;
        }
        return pc.getType() == type;
    }


}
