package sample.model;


// místnost v aplikaci
public class Room extends Folder {
    private Client superUser;

    // superUser = uživatel s vyššími právy
    public Room(Client superUser, long id, String name){
        super(id, name);
        this.superUser = superUser;
    }

    public Client getSuperUser(){
        return superUser;
    }

    public String toString(){
        return getName();
    }

}
