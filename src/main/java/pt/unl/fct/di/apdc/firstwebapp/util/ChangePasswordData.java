package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangePasswordData{

    public String confirmPassword;
    public String oldPassword;
    public String password;


    public ChangePasswordData() {

    }

    public ChangePasswordData(String oldPassword, String password, String confirmPassword) {
        this.oldPassword = oldPassword;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public boolean validate(){
        return oldPassword != null && password != null && password.equals(confirmPassword);
    }
}
