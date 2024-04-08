package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangePasswordData{

    public String confirmPassword;
    public String username;
    public String password;


    public ChangePasswordData() {

    }

    public ChangePasswordData(String username, String password, String confirmPassword) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public boolean validate(){
        return username != null && password != null && password.equals(confirmPassword);
    }
}
