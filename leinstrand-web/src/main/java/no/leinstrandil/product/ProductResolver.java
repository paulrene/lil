package no.leinstrandil.product;

import no.leinstrandil.database.model.club.Event;
import no.leinstrandil.database.model.club.Sport;
import no.leinstrandil.database.model.club.Team;
import no.leinstrandil.database.model.person.Principal;
import no.leinstrandil.service.ClubService;
import no.leinstrandil.service.UserService;

public class ProductResolver {

    private ProductResolver() {
    }

    public static Product getPrincipalClubMembershipProductByAge(Principal principal, int productYear) {
        int age = UserService.getAgeAtEndOfYear(principal, productYear);
        if (age >= 18) {
            return new Product(ProductCode.ADULT_MEMBERSHIP.getCode(), "Klubbmedlemskap voksen " + productYear + " for " + principal.getName(), 150, 0);
        } else {
            return new Product(ProductCode.YOUTH_MEMBERSHIP.getCode(), "Klubbmedlemskap barn/ungdom " + productYear + " for " + principal.getName(), 50, 0);
        }
    }

    public static Product getFamilyClubMembershipProduct(int productYear) {
        return new Product(ProductCode.FAMILY_MEMBERSHIP.getCode(), "Familiemedlemskap " + productYear, 200, 0);
    }

    public static Product getTeamMembershipProduct(Principal principal, Team team, int feeCount, int productYear) {
        int age = UserService.getAgeAtEndOfYear(principal, productYear);
        int price = 0;
        int discount = 0;
        if (age <= 7) {
            if (feeCount >= 1) {
                discount = 100;
            }
            price = 300;
        } else if (age >= 8 && age <= 12 ) {
            if (feeCount >= 2) {
                discount = 100;
            }
            price = 300;
        } else if (age >= 13 && age <= 17) {
            if (feeCount >= 2) {
                discount = 100;
            }
            price = 950;
        } else { // age >= 18
            price = 2200;
        }
        Sport sport = team.getSport();
        String description = sport.getName() + " " + team.getName() + " trenings-/aktivitetsavgift " + productYear + " for " + principal.getName();
        return new Product(ProductCode.TEAM_FEE.getCode(), description, price, discount);
    }

    public static Product getEventParticipationProduct(Principal principal, Event event) {
        Integer price = event.getPriceMember();
        String priceStatus = "medlem";
        if (!event.requireMembership()) {
            if (!ClubService.isEnrolledAsClubMember(principal.getFamily())) {
                price = event.getPriceNonMember();
                priceStatus = "ikke medlem";
            }
        }
        String description = "Deltageravgift " + event.getName() + " for " + priceStatus + " "  + principal.getName();
        return new Product(ProductCode.EVENT_FEE.getCode(), description, price, 0);
    }

}
