package sample.model;

import sample.model.enums.ClientState;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// slouží k přenášení dat mezi serverem a klientem a zároven i jako samotná zpráva v chatu
public class Message implements Serializable {

    private String message;
    private String name;
    private String password;
    private String email;
    private int protocol;
    private long id;
    private long senderId;
    private String superUserName;
    private long roomId;
    private List<Message> messages;
    // unikátní ID zprávy
    private String uniqueID;
    private List<Long> ids;
    private ClientState state;
    private LocalDateTime timeStamp;


    public Message(int protocol, String name, String password, String email){
        this.protocol = protocol;
        this.name = name;
        this.password = password;
        this.email = email;
    }

    public Message(int protocol, long superUserid){
        this.protocol = protocol;
        this.id = superUserid;
    }

    public Message(int protocol){
        this.protocol = protocol;
    }

    // CHAT
    public Message(int protocol, long senderId, long id, String name, String message){
        this.protocol = protocol;
        this.senderId = senderId;
        this.id = id;
        this.name = name;
        this.message = message;
        uniqueID = UUID.randomUUID().toString();
        timeStamp = LocalDateTime.now();
    }

    public Message(int protocol, long roomId, long id){
        this.protocol = protocol;
        this.roomId = roomId;
        this.id = id;
    }

    public Message(long roomId, String name, long id, String superUserName){
        this.roomId = roomId;
        this.name = name;
        this.id = id;
        this.superUserName = superUserName;
    }

    public Message(int protocol, List<Message> messages){
        this.protocol = protocol;
        this.messages = messages;
    }

    // FOR CHAT MESSAGES
    public Message(int protocol, long roomId, String name, long superUserid, String superUserName){
        this.protocol = protocol;
        this.id = superUserid;
        this.name = name;
        this.roomId = roomId;
        this.superUserName = superUserName;
    }

    public Message(int protocol, long id, String name, ClientState state){
        this.protocol = protocol;
        this.id = id;
        this.state = state;
        this.name = name;
    }

    public Message(int protocol, long id, String name, ClientState state, long roomId){
        this.protocol = protocol;
        this.id = id;
        this.state = state;
        this.name = name;
        this.roomId = roomId;
    }


    public Message(int protocol, String name){
        this.protocol = protocol;
        this.name = name;
    }

    public Message(int protocol, String name, String password){
        this.name = name;
        this.password = password;
        this.protocol = protocol;
    }

    public Message(int protocol, long roomId, List<Long> ids){
        this.protocol = protocol;
        this.roomId = roomId;
        this.ids = ids;
    }

    public Message(){ }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public int getProtocol(){
        return this.protocol;
    }

    public long getId(){
        return id;
    }

    public long getRoomId(){
        return this.roomId;
    }

    public List<Message> getMessages(){
        return this.messages;
    }


    public String getSuperUserName(){
        return this.superUserName;
    }

    public long getSenderId(){
        return this.senderId;
    }

    public String getUniqueID(){
        return uniqueID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(uniqueID, message.uniqueID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueID);
    }

    public void setSenderId(long id){
        this.senderId = id;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public void setProtocol(int protocol){
        this.protocol = protocol;
    }

    public void setId(long roomId) {
        this.id = roomId;
    }

    @Override
    public String toString() {
        return getCurrentLocalDateTimeStamp() + ": " + name + ": " + message;
    }

    public String getEmail() {
        return email;
    }

    // vrátí LocalDateTime v formatu String
    public String getCurrentLocalDateTimeStamp() {
        if(timeStamp == null){
            timeStamp = LocalDateTime.now();
        }
        return timeStamp
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    public ClientState getState() {
        return state;
    }

    public void setState(ClientState state) {
        this.state = state;
    }

    public void setLocalTimeStamp(LocalDateTime localTimeStamp){
        this.timeStamp = localTimeStamp;
    }




}


