package ru.mail.jira.plugins.myteam.exceptions;

public class MyteamServerErrorException extends Exception{
    public int getStatus() {
        return status;
    }

    public int status;
    public MyteamServerErrorException(int status,String message){
        super(message);
        this.status = status;
    }
}
