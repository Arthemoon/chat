package sample.server;

// slouží pro komunikaci mezi serverem a klientem - jak zpracovat data atd...
public class ServerProtocols {

    public static final int LOGIN = 1;
    public static final int REGISTER = 2;
    public static final int GET_ALL_ONLINE_FRIENDS = 3;
    public static final int CREATE_ROOM = 4;
    public static final int ADD_CLIENT = 25;
    public static final int GET_ALL_ROOMS = 6;
    public static final int GET_ALL_CLIENTS_IN_ROOMS = 8;

    public static final int LOGIN_SUCCESS = 100;
    public static final int LOGIN_FAILED = 101;

    public static final int REGISTRATION_SUCCESS = 200;
    public static final int REGISTRATION_FAILED = 201;

    public static final int PRIVATE = 500;
    public static final int GROUP = 501;

    public static final int ADD_TO_GROUP = 502;
    public static final int LEAVEGROUP = 503;
    public static final int DESTROY = 504;

    public static final int GET_ALL_FRIENDS = 600;
    public static final int ADD_FRIEND = 601;
    public static final int REMOVE_FRIEND = 602;
    public static final int DISCONNECTED = 700;
    public static final int QUERY = 800;
    public static final int REQUEST_ACCEPTED = 900;
    public static final int REQUEST_DECLINED = 901;
    public static final int ADD_FRIENDS = 603;
    public static final int LOADED_MESSAGES = 950;
    public static final int LOAD_MESSAGE = 951;
    public static final int CHANGE_ROOM_NAME = 980;
}
