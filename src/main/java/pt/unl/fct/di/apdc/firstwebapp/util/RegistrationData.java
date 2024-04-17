package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegistrationData {

    public String username;
    public String password;
    public String email;
    public String confirm_password;
    public String name;
    public boolean profile; //private or not
    public String profession;
    public String workplace;
    public String address;
    public String postalCode;
    public String NIF;
    public boolean hasPhoto = false;
    public String tel_number;

    public RegistrationData() {}

    public RegistrationData(String username, String password, String confirm_password, String email, String name,
                            Boolean profile, String profession, String workplace, String address, String postalCode,
                            String NIF, Boolean hasPhoto, String tel_number){
        this.username = username;
        this.password = password;
        this.confirm_password = confirm_password;
        this.email = email;
        this.name = name;
        this.profile = profile;
        this.profession = profession;
        this.workplace = workplace;
        this.address = address;
        this.postalCode = postalCode;
        this.NIF = NIF;
        this.hasPhoto = hasPhoto;
        this.tel_number = tel_number;
    }

    public boolean validRegistration(){
        return username != null && password != null && password.equals(confirm_password) && email != null && name != null;
    }

}
