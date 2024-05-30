package jm.preversion.biblewith.login;

public class LoginDto {
    private int user_no;
    private String user_email;
    private String user_pwd;
    private boolean user_autologin;
    private String user_nick;
    private String user_name;
    private String user_image;

    public LoginDto(String user_email, String user_pwd, boolean user_autologin) {
        this.user_email = user_email;
        this.user_pwd = user_pwd;
        this.user_autologin = user_autologin;
    }

    public LoginDto(int user_no, String user_email, String user_pwd, boolean user_autologin, String user_nick, String user_name, String user_image) {
        this.user_no = user_no;
        this.user_email = user_email;
        this.user_pwd = user_pwd;
        this.user_autologin = user_autologin;
        this.user_nick = user_nick;
        this.user_name = user_name;
        this.user_image = user_image;
    }

    @Override
    public String toString() {
        return "LoginDto{" +
                "user_no=" + user_no +
                ", user_email='" + user_email + '\'' +
                ", user_pwd='" + user_pwd + '\'' +
                ", user_autologin=" + user_autologin +
                ", user_nick='" + user_nick + '\'' +
                ", user_name='" + user_name + '\'' +
                ", user_image='" + user_image + '\'' +
                '}';
    }

    public int getUser_no() {
        return user_no;
    }

    public void setUser_no(int user_no) {
        this.user_no = user_no;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getUser_pwd() {
        return user_pwd;
    }

    public void setUser_pwd(String user_pwd) {
        this.user_pwd = user_pwd;
    }

    public boolean isUser_autologin() {
        return user_autologin;
    }

    public void setUser_autologin(boolean user_autologin) {
        this.user_autologin = user_autologin;
    }

    public String getUser_nick() {
        return user_nick;
    }

    public void setUser_nick(String user_nick) {
        this.user_nick = user_nick;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_image() {
        return user_image;
    }

    public void setUser_image(String user_image) {
        this.user_image = user_image;
    }
}
