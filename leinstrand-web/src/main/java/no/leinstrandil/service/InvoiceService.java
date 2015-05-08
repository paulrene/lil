package no.leinstrandil.service;

import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.club.Team;

public class InvoiceService {

    private SendRegningService sendRegningService;
    private UserService userService;
    private Storage storage;

    public InvoiceService(Storage storage, UserService userService, SendRegningService sendRegningService) {
        this.storage = storage;
        this.userService = userService;
        this.sendRegningService = sendRegningService;
    }

    public void createInvoiceForTeam(Team team) {
        // TODO Auto-generated method stub

    }

    public void createClubMembershipInvoice() {





        // TODO Auto-generated method stub

    }

}
