package no.leinstrandil.service;

import java.io.IOException;
import no.leinstrandil.Config;
import no.leinstrandil.database.Storage;
import no.sws.client.SwsClient;
import org.apache.commons.httpclient.HttpException;

public class SendRegningService {

    private Storage storage;
    private SwsClient swsClient;

    public SendRegningService(Storage storage, Config config) throws HttpException, IOException {
        this.storage = storage;
        swsClient = new SwsClient(config.getSrsUsername(), config.getSrsPassword());

        swsClient.setTest(true);
    }

    public ServiceResponse sendInvoice(no.leinstrandil.database.model.accounting.Invoice invoice) {





        return null;
    }


}
