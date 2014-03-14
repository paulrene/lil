package no.leinstrandil.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.person.Address;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.UserService;
import org.apache.velocity.VelocityContext;
import org.json.JSONObject;
import spark.Request;

public class MyPageController implements Controller {

    private UserService userService;

    public MyPageController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void handleGet(User user, Request request, VelocityContext context) {
        String tab = request.queryParams("tab");
        if (tab == null) {
            tab = "profil";
        }
        context.put("tab", tab);

        if (tab.equals("adresse")) {
            List<Address> addressList = user.getPrincipal().getAddressList();
            if (!addressList.isEmpty()) {
                Address address = addressList.get(0);
                JSONObject data = new JSONObject();
                data.put("address1", address.getAddress1());
                data.put("zip", address.getZip());
                data.put("city", address.getCity());
                data.put("country", address.getCountry());
                context.put("data", data);
            }
        }

    }

    @Override
    public void handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        if (user == null) {
            return;
        }

        String action = request.queryParams("action");
        if ("save-profile".equals(action)) {
            saveProfile(user, request, errorMap, infoList);
        } else if ("save-address".equals(action)) {
            saveAddress(user, request, errorMap, infoList);
        }

    }

    private void saveAddress(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String address1 = request.queryParams("address1");
        String city = request.queryParams("city");
        String zip = request.queryParams("zip");
        String country = request.queryParams("country");

        if (address1 == null || address1.isEmpty()) {
            // We allow empty street addresses.
        }
        if (zip == null || zip.isEmpty()) {
            errorMap.put("zip", "Postnummer må fylles ut.");
        }
        if (city == null || city.isEmpty()) {
            errorMap.put("city", "Poststed må fylles ut.");
        }
        if (country == null || country.isEmpty()) {
            country = "Norge";
        }

        if (errorMap.isEmpty()) {
            userService.updateAddress(user, address1, "", zip, city, country);
            infoList.add("Din adresse ble lagret.");
        }
    }

    private void saveProfile(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String name = request.queryParams("name");
        if (name == null || name.isEmpty()) {
            errorMap.put("name", "Du må oppgi navnet ditt. Vi trenger det for å holde medlemsregistret oppdatert.");
        }
        if (name.split(" ").length <=1 ) {
            errorMap.put("name", "Du må oppgi både fornavn og etternavn.");
        }

        String birthDateStr = request.queryParams("birthdate");
        if (birthDateStr == null || birthDateStr.isEmpty()) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen din for å kunne beregne riktig treningsavgift.");
        }
        Date birthDate = null;
        try {
            birthDate = new SimpleDateFormat("yyyy-MM-dd").parse(birthDateStr);
        } catch (ParseException e) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen din for å kunne beregne riktig treningsavgift.");
        }

        String gender = request.queryParams("gender");
        if (gender == null || gender.isEmpty()) {
            errorMap.put("gender", "Vi trenger å vite ditt kjønn for å vise de mest relevante treningstilbudene først.");
        }

        if (errorMap.isEmpty()) {
            userService.updateProfile(user, name, birthDate, gender);
            infoList.add("Din profil ble lagret.");
        }
    }

}
