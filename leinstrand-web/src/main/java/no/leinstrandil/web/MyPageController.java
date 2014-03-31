package no.leinstrandil.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import no.leinstrandil.database.model.person.Address;
import no.leinstrandil.database.model.person.EmailAddress;
import no.leinstrandil.database.model.person.Family;
import no.leinstrandil.database.model.person.MobileNumber;
import no.leinstrandil.database.model.web.User;
import no.leinstrandil.service.ServiceResponse;
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
        } else if (tab.equals("epost")) {
            List<EmailAddress> emailList = user.getPrincipal().getEmailAddressList();
            if (!emailList.isEmpty()) {
                EmailAddress email = emailList.get(0);
                JSONObject data = new JSONObject();
                data.put("email", email.getEmail());
                context.put("data", data);
            }
        } else if (tab.equals("mobil")) {
            List<MobileNumber> mobileList = user.getPrincipal().getMobileNumberList();
            if (!mobileList.isEmpty()) {
                MobileNumber mobile = mobileList.get(0);
                JSONObject data = new JSONObject();
                data.put("mobile", mobile.getNumber());
                context.put("data", data);
            }
        } else if (tab.equals("familie")) {
            Family family = userService.ensureFamilyForUser(user);
            context.put("family", family);
        }

    }

    @Override
    public String handlePost(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        if (user == null) {
            return null;
        }
        String action = request.queryParams("action");
        if ("save-profile".equals(action)) {
            saveProfile(user, request, errorMap, infoList);
        } else if ("save-address".equals(action)) {
            saveAddress(user, request, errorMap, infoList);
        } else if ("save-email".equals(action)) {
            saveEmail(user, request, errorMap, infoList);
        } else if ("save-mobile".equals(action)) {
            saveMobile(user, request, errorMap, infoList);
        } else if ("add-family-member".equals(action)) {
            addFamilyMember(user, request, errorMap, infoList);
        }

        return null;
    }

    private void addFamilyMember(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String name = request.queryParams("name");
        String birthDateStr = request.queryParams("birthdate");
        String gender = request.queryParams("gender");

        if (name == null || name.isEmpty()) {
            errorMap.put("name", "Du må oppgi navn på familiemedlemmet.");
        }
        if (name.split(" ").length <=1 ) {
            errorMap.put("name", "Du må oppgi både fornavn og etternavn.");
        }

        if (birthDateStr == null || birthDateStr.isEmpty()) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen for å kunne beregne riktig treningsavgift.");
        }
        Date birthDate = null;
        try {
            birthDate = new SimpleDateFormat("dd.MM.yyyy").parse(birthDateStr);
        } catch (ParseException e) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen for å kunne beregne riktig treningsavgift.");
        }

        if (gender == null || gender.isEmpty()) {
            errorMap.put("gender", "Vi trenger å vite kjønn for å vise de mest relevante treningstilbudene først.");
        }

        if (errorMap.isEmpty()) {
            popuplateResponse(errorMap, infoList, userService.addFamilyMember(user, name, birthDate, gender));
        }


    }

    private void saveMobile(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String mobile = request.queryParams("mobile");
        if (mobile == null || mobile.isEmpty()) {
            String newMobile = mobile.replace("+", "");
            try { Long.parseLong(newMobile); }  catch (NumberFormatException e) {
                errorMap.put("mobile", "Mobilnummeret kan kun bestå av tall.");
            }
            if (newMobile.length()<8) {
                errorMap.put("mobile", "Mobilnummeret må bestå av minst 8 siffer.");
            }
        }

        if (errorMap.isEmpty()) {
            popuplateResponse(errorMap, infoList, userService.updateMobile(user, mobile));
        }
    }

    private void saveEmail(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String email = request.queryParams("email");
        if (email == null || email.isEmpty()) {
            if (!email.contains("@")) {
                errorMap.put("email", "E-postadressen må inneholde en krøllalfa.");
            }
        }

        if (errorMap.isEmpty()) {
            popuplateResponse(errorMap, infoList, userService.updateEmail(user, email));
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
            popuplateResponse(errorMap, infoList, userService.updateAddress(user, address1, "", zip, city, country));
        }
    }

    private void saveProfile(User user, Request request, Map<String, String> errorMap, List<String> infoList) {
        String name = request.queryParams("name");
        String birthDateStr = request.queryParams("birthdate");
        String gender = request.queryParams("gender");

        if (name == null || name.isEmpty()) {
            errorMap.put("name", "Du må oppgi navnet ditt. Vi trenger det for å holde medlemsregistret oppdatert.");
        }
        if (name.split(" ").length <=1 ) {
            errorMap.put("name", "Du må oppgi både fornavn og etternavn.");
        }

        if (birthDateStr == null || birthDateStr.isEmpty()) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen din for å kunne beregne riktig treningsavgift.");
        }
        Date birthDate = null;
        try {
            birthDate = new SimpleDateFormat("yyyy-MM-dd").parse(birthDateStr);
        } catch (ParseException e) {
            errorMap.put("birthdate", "Vi trenger fødselsdatoen din for å kunne beregne riktig treningsavgift.");
        }

        if (gender == null || gender.isEmpty()) {
            errorMap.put("gender", "Vi trenger å vite ditt kjønn for å vise de mest relevante treningstilbudene først.");
        }

        if (errorMap.isEmpty()) {
            popuplateResponse(errorMap, infoList, userService.updateProfile(user, name, birthDate, gender));
        }
    }

    private void popuplateResponse(Map<String, String> errorMap, List<String> infoList, ServiceResponse response) {
        if (response.isSuccess()) {
            infoList.add(response.getMessage());
        } else {
            errorMap.put("save", response.getMessage());
        }
    }

}
