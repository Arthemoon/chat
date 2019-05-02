package sample.server;

import sample.model.Client;
import sample.model.Message;


import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// obsluha databáze
public class DatabaseManager {
    // // jdbc:mysql://localhost:3306/chatapp?user=springstudent&password=springstudent
    private static final String CONNECTION_STRING = "jdbc:mysql://sql7.freemysqlhosting.net:3306/sql7273485?user=sql7273485&password=VsBK3uTZq3";

    // jdbc:mysql://sql7.freemysqlhosting.net:3306/sql17273485?userésqk17273485&password=VsBK3uTZq3

    public DatabaseManager() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.exit(1);
        }
    }

    // registrace klienta
    public boolean registerClient(String name, String password, String email) {

        boolean registered;

        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {

            PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO client " +
                    "(name, password, email) VALUES(?,?,?)");
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, email);

            registered = preparedStatement.executeUpdate() != -1;

        } catch (SQLException e) {
            registered = false;
        }

        return registered;
    }


    // klientský login podle emailu
    public Client clientLogin(String email){
        Client client = null;
        try(Connection conn = DriverManager.getConnection(CONNECTION_STRING)){
            PreparedStatement preparedStatement = conn.prepareStatement("SELECT c.id, c.name, c.password FROM client c WHERE" +
                    " c.email=?");
            preparedStatement.setString(1, email);

            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.next()){
                int id = resultSet.getInt("id");
                String userName = resultSet.getString("name");
                String password = resultSet.getString("password");
                client = new Client(id, userName, password);
            }

        } catch (SQLException e){
            e.printStackTrace();
        }

        return client;
    }

    // vrátí klienta na základě id
    public Client getClient(long id) {
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING);
             PreparedStatement statement = conn.prepareStatement("SELECT * FROM client WHERE ID = ?")) {
            statement.setLong(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int id2 = resultSet.getInt("id");
                String name = resultSet.getString("name");

                return new Client(id2, name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    // vrátí všechny kamarády daného uživatele, můžou být dva typy vyledávání: requested = neschválené žádosti o přátelství
    // a accepted = což jsou schválené žádosti o přátelství
    public List<Client> getAllFriends(long id, String type) {
        List<Client> clients = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {
            PreparedStatement preparedStatement;

            if (type == "accepted") {
                preparedStatement = conn.prepareStatement("SELECT * FROM " + "client" + " C LEFT JOIN " +
                        "friends" + " F " + " ON C.id = F.friendId WHERE F.clientId = ? AND F.type=?" +
                        " UNION SELECT * FROM client C LEFT JOIN " +
                        " friends F ON C.id = F.clientId WHERE F.friendId = ? AND F.type=?");

                preparedStatement.setLong(3, id);
                preparedStatement.setString(4, type);

            } else {
                preparedStatement = conn.prepareStatement("SELECT * FROM client INNER JOIN friends " +
                        "on client.id = friends.clientId WHERE friends.friendId = ? AND friends.type=?");

            }

            preparedStatement.setLong(1, id);
            preparedStatement.setString(2, type);

            ResultSet allFriends = preparedStatement.executeQuery();

            clients = createListOfClients(allFriends);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return clients;
    }


    // vyhledávání klienta podle emailu
    public List<Client> query(String name) {
        List<Client> clients = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {
            String query = "SELECT id, name FROM " + "client" + " WHERE email=? ";
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            preparedStatement.setString(1, name);


            ResultSet resultSet = preparedStatement.executeQuery();
            clients = createListOfClients(resultSet);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return clients;
    }

    // vrátí list klientů z resultSetu
    private List<Client> createListOfClients(ResultSet resultSet) {
        List<Client> clients = new ArrayList<>();
        try {
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                String name = resultSet.getString("name");
                clients.add(new Client(id, name));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return clients;
    }

    // přidá žádost o přátelství
    public int addRequest(long sender, long receiver) {

        int result = -1;
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {
            String query = "INSERT INTO friends VALUES(?,?,?)";

            PreparedStatement preparedStatement = conn.prepareStatement(query);
            preparedStatement.setLong(1, sender);
            preparedStatement.setLong(2, receiver);
            preparedStatement.setString(3, "requested");

            result = preparedStatement.executeUpdate();


        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // updatuje žádost o přátelství
    public int updateRequest(long id, long senderId) {
        int result = -1;
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {

            String sql = "UPDATE friends SET type=? WHERE clientId = ? AND friendId = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, "accepted");
            preparedStatement.setLong(2, id);
            preparedStatement.setLong(3, senderId);

            result = preparedStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // odstranuje žádost o přátelství
    public int removeRequest(long id, long senderId) {
        int result = -1;
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {

            String sql = "DELETE FROM friends WHERE clientId=? AND friendId=? OR clientId=? AND friendId=?";

            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setLong(1, senderId);
            preparedStatement.setLong(2, id);
            preparedStatement.setLong(3, id);
            preparedStatement.setLong(4, senderId);

            result = preparedStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    // ukládání privátní zprávy
    public int saveMessage(String uniqueID, long senderId, long id, String message) {
        int updated = 0;
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {

            java.sql.Timestamp date = new java.sql.Timestamp(new java.util.Date().getTime());
            String sql = "INSERT INTO message VALUES (?,?,?,?,?,?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, uniqueID);
            preparedStatement.setLong(2, senderId);
            preparedStatement.setLong(3, id);
            if(message.length() > 500){
                message = message.substring(0, 499);
            }
            preparedStatement.setString(4, message);
            preparedStatement.setTimestamp(5, date);
            preparedStatement.setLong(6, countChatId(senderId, id));
            updated = preparedStatement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return updated;
    }

    // získá všechny zprávy v daném chatu seřazené podle datumu
    public List<Message> getMessages(long id, long id2) {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(CONNECTION_STRING)) {

            PreparedStatement preparedStatement = conn.prepareStatement("SELECT (SELECT name FROM client WHERE id=m.sender_id), m.date, m.chatId, m.message FROM client c " +
                    "INNER JOIN message m ON c.id = ? WHERE chatId=? ORDER BY date");

            preparedStatement.setLong(1, id);
            preparedStatement.setLong(2, countChatId(id, id2));

            ResultSet resultSet = preparedStatement.executeQuery();


            while (resultSet.next()) {
                Message m = new Message();
                m.setName(resultSet.getString(1));
                m.setMessage(resultSet.getString("message"));
                Timestamp timeStap = resultSet.getTimestamp("date");
                LocalDateTime ldt = timeStap.toLocalDateTime();
                m.setLocalTimeStamp(ldt);
                m.setRoomId(resultSet.getInt("chatId"));

                messages.add(m);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    // spočítá ID chatu
    private long countChatId(long id1, long id2){
        String value = "";
        if (id1 > id2) {
            value = id2 + "" + id1;
            return  Integer.parseInt(value);
        } else if(id1 < id2){
            value = id1 + "" + id2;
            return  Integer.parseInt(value);
        } else if(id1 == id2) {
            value = id1 + "" + id1;
            return Integer.parseInt(value);
        }
        return -1;
    }

}



