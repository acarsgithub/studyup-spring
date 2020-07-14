package io.study.studyup;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.xml.transform.Result;
import java.security.Principal;
import java.sql.*;

@Controller
public class Resource {

    final String pass = "";

    @GetMapping("/")
    @ResponseBody
    public String home(){
        return "Welcome!" ;
    }

    @PostMapping("/signup")
    @ResponseBody
    public String createAccount(@RequestBody String jsonStr) throws JSONException {

        // parsing json object
        // signing up requires username, password, email, and classrank
        JSONObject json = new JSONObject(jsonStr);
        String username = json.getString("username");
        String password = json.getString("password");
        String email = json.getString("email");
        String classrank = json.getString("classrank"); // classrank should be FRESHMAN, SOPHOMORE, JUNIOR, or SENIOR

        // prepared statement used to prevent SQL injection execution
        PreparedStatement update = null;

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;

        try {
            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);


            // Determines if the user already has an account
            String checkCurrUsers = "SELECT username, email FROM user";
            ResultSet setOfCurrUsers = stmt.executeQuery(checkCurrUsers);
            while(setOfCurrUsers.next()){
                if(setOfCurrUsers.getString("username").equals(username)){
                    return("<h2><center>The username is already taken!</center></h2>");
                } else if(setOfCurrUsers.getString("email").equals(email)){
                    return("<h2><center>That email is already associated with an account!</center></h2>");
                }
            }

            // If it is a new account, add to database
            String addAccount = "INSERT INTO user(`email`, `username`, `active`, `password`, `classrank`, `roles`) " +
                    " VALUES (?, ?, TRUE, ?, ?, 'ROLE_USER')";

            // Using prepared statement as parameterized SQL query to prevent SQL Injection from normal string concatenation
            update = conn.prepareStatement(addAccount);
            update.setString(1, email);
            update.setString(2, username);
            update.setString(3, password);
            update.setString(4, classrank);
            update.executeUpdate();

        } catch (Exception se) { se.printStackTrace(); }
        // Close Resources
        try {
            if (conn != null && stmt != null)
                conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

        // Account added successfully
        return "<h2><center>Created New Account Successfully!</center></h2>";
    }



    @PostMapping("/request-group")
    @ResponseBody
    public String requestSend(@RequestBody String jsonStr, Principal principal) throws JSONException {

        // parsing json object
        JSONObject json = new JSONObject(jsonStr);
        String groupname = json.getString("groupname");

        // Using principal to get currently logged in user
        String username = principal.getName();

        // Variables to store data later
        int requestID = -1;
        int adminID = -1;
        int groupID = -1;

        // Using prepared statements to help fight against SQL Injection
        PreparedStatement request = null;
        PreparedStatement groupInfoPS = null;
        PreparedStatement requestsEntry = null;

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;

        try {
            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            // Obtains user id from database using prepared statement to prevent SQL Injection
            String requesterIDSQL = "SELECT id FROM user WHERE username = ?";
            request = conn.prepareStatement(requesterIDSQL);
            request.setString(1, username);

            // Obtaining id of user wanting to request
            ResultSet requestIDSet = request.executeQuery();
            while (requestIDSet.next()){
                requestID = requestIDSet.getInt("id");
            }

            // extracting information about group that user wants to join from database
            String groupInfoSQL = "SELECT groupid, groupadmin_id FROM studygroups WHERE groupname = ?";

            // Using prepared statement to prevent SQL Injection
            groupInfoPS = conn.prepareStatement(groupInfoSQL);
            groupInfoPS.setString(1, groupname);
            ResultSet groupSet = groupInfoPS.executeQuery();

            // Getting groupID and group admin id from database
            while (groupSet.next()){
                groupID = groupSet.getInt("groupid");
                adminID = groupSet.getInt("groupadmin_id");
            }

            // Add user request into requests table
            String updateRequests = "INSERT INTO requests(`groupid`, `groupname`, `groupadmin_id`, `requestuserid`) " +
                    "VALUES (?, ?, ?, ?)";

            // Using prepared statement to prevent SQL Injection and to add information to requests table in database
            requestsEntry = conn.prepareStatement(updateRequests);
            requestsEntry.setInt(1, groupID);
            requestsEntry.setString(2, groupname);
            requestsEntry.setInt(3, adminID);
            requestsEntry.setInt(4, requestID);
            stmt.executeUpdate(updateRequests);


        } catch (Exception se) { se.printStackTrace(); }
        // Close Resources
        try {
            if (conn != null && stmt != null)
                conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

        // Account added successfully
        return "<h2><center>Requested Group Successfully!</center></h2>";
    }



    @PostMapping("/create-group")
    @ResponseBody
    public String createNewGroup(@RequestBody String jsonStr, Principal principal) throws JSONException {

        // parsing json object
        JSONObject json = new JSONObject(jsonStr);
        String groupname = json.getString("groupname");
        String subject = json.getString("subject");
        String description = json.getString("description");

        // Gets the username of the logged in individual
        String curUser = principal.getName();

        // Variables for storing data later
        int realAdminID = -1;
        int groupID = -1;

        PreparedStatement userIDPS = null;
        PreparedStatement createGroupPS = null;
        PreparedStatement groupIDPS = null;
        PreparedStatement associationPS = null;

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;

        try {
            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);


            // Determines if the groupname already exists
            String checkCurrUserNames = "SELECT groupname FROM studygroups";
            ResultSet setOfCurrUserNames = stmt.executeQuery(checkCurrUserNames);
            while(setOfCurrUserNames.next()){
                if(setOfCurrUserNames.getString("groupname").equals(groupname)){
                    return("<h2><center>The group name is already taken!</center></h2>");
                }
            }

            // Get id of user with prepared statement to prevent SQL Injection
            String adminID = "SELECT id FROM user WHERE username = ?";
            userIDPS = conn.prepareStatement(adminID);
            userIDPS.setString(1, curUser);
            ResultSet adminIDSet = userIDPS.executeQuery();

            // Storing user id to add to studygroups table
            while (adminIDSet.next()){
                realAdminID = adminIDSet.getInt("id");
            }


            // Add study group to database if it is not taken already
            String insertGroup = "INSERT INTO studygroups(`groupname`, `groupadmin_username`, `groupadmin_id`, `numusers`, `subject`, `description`) " +
                    " VALUES (?, ?, ?, '1', ?, ?)";

            // using prepared statement ot add studygroup to database to prevent SQL Injection
            createGroupPS = conn.prepareStatement(insertGroup);
            createGroupPS.setString(1, groupname);
            createGroupPS.setString(2, curUser);
            createGroupPS.setInt(3, realAdminID);
            createGroupPS.setString(4, subject);
            createGroupPS.setString(5, description);
            createGroupPS.executeUpdate();


            // Getting group ID of group just created and using prepared statement to obtain data
            String obtainGroupID = "SELECT groupid FROM studygroups WHERE groupname = ?";
            groupIDPS = conn.prepareStatement(obtainGroupID);
            groupIDPS.setString(1, groupname);
            ResultSet getGroupID = groupIDPS.executeQuery();

            // actually obtaining groupID of group just created and storing info
            while(getGroupID.next()){
                groupID = getGroupID.getInt("groupid");
            }

            // Adding admin to associations using prepared statement to prevent SQL Injection
            String adminAssoc = "INSERT INTO associations(`groupid`, `userid`, `roles`) VALUES (?, ?, 'ROLE_ADMIN')";
            associationPS = conn.prepareStatement(adminAssoc);
            associationPS.setInt(1, groupID);
            associationPS.setInt(2, realAdminID);
            associationPS.executeUpdate();


        } catch (Exception se) { se.printStackTrace(); }
        // Close Resources
        try {
            if (conn != null && stmt != null)
                conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

        // Account added successfully
        return "<h2><center>Created New StudyUp Group Successfully!</center></h2>";
    }



    // HERE KEEP GOING
    @GetMapping("/groups")
    @ResponseBody
    public String allGroupsHome(){

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;
        String groupInfoData = "";

        try {
            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            // Get group information
            String extractGroupInfo = "SELECT groupname, groupadmin_username, numusers, subject, description FROM studygroups";

            // Formatting data
            if(stmt.execute(extractGroupInfo)) {
                // Obtaining study group data from SQL database
                ResultSet groupInfo = stmt.executeQuery(extractGroupInfo);

                // Accessing helper method to create table
                groupInfoData = viewTable(groupInfo, "<h2><center>Study Groups</center></h2>");
                groupInfo.close();
            }


        } catch (Exception se) { se.printStackTrace(); }
        // Close Resources
        try {
            if (conn != null && stmt != null)
                conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

        // Account added successfully
        return groupInfoData;
    }


    @GetMapping("/{username}")
    @ResponseBody
    public String userHome(@PathVariable("username") String username, Principal principal){

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;
        String loggedInUser = principal.getName();
        int id = -1;

        StringBuilder data = new StringBuilder();

        if(!loggedInUser.equals(username)){
            return "<h2><center>You do not have access to this individual's page!</center></h2>";
        }

        try {
            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            // Get group information
            String getUserID = "SELECT id FROM user WHERE username = '" + loggedInUser + "'";
            ResultSet userIDSet = stmt.executeQuery(getUserID);
            while(userIDSet.next()){
                id = userIDSet.getInt("id");
            }

            String findGroupsForID = "SELECT groupid FROM associations WHERE userid = '" + id + "'";
            ResultSet groups = stmt.executeQuery(findGroupsForID);
            while(groups.next()){
                int groupID = groups.getInt("groupid");
                String findGroupInfo = "SELECT * FROM studygroups WHERE groupid = '" + groupID + "'";
                ResultSet groupInfoSet = stmt.executeQuery(findGroupInfo);
                data.append(viewTable(groupInfoSet, "<h2><center>Group ID: " + groupID + "</center</h2>")).append("<br><br>");
                groupInfoSet.close();
            }


        } catch (Exception se) { se.printStackTrace(); }
        // Close Resources
        try {
            if (conn != null && stmt != null)
                conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return data.toString();
    }


    @GetMapping("/{username}/requests")
    @ResponseBody
    public String userRequests(@PathVariable("username") String username, Principal principal,
                               @RequestParam(defaultValue = "-1", value = "requestID", required = false) int requestUserID,
                               @RequestParam(value= "groupID", required=false) String groupID,
                               @RequestParam(value= "decision", required=false) boolean decision){

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;
        String loggedInUser = principal.getName();

        int id = -1;
        StringBuilder data = new StringBuilder();

        if(!loggedInUser.equals(username)){
            return "<h2><center>You do not have access to this individual's page!</center></h2>";
        }

        try {
            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            if(requestUserID == -1){

                // Get user id
                String getUserID = "SELECT id FROM user WHERE username = '" + loggedInUser + "'";
                ResultSet userIDSet = stmt.executeQuery(getUserID);
                while(userIDSet.next()){
                    id = userIDSet.getInt("id");
                }

                String findGroupsForID = "SELECT requestuserid, groupname FROM requests WHERE groupadmin_id = '" + id + "'";
                ResultSet groups = stmt.executeQuery(findGroupsForID);
                return viewTable(groups, "<h2><center>All Requests For " + username + "</center></h2>");
            } else {
                if (decision){
                    String requestDeny = "DELETE FROM requests WHERE requestuserid = '" + requestUserID + "' AND groupid = '" + groupID + "'";
                    stmt.executeUpdate(requestDeny);

                    String numUsersIncrease = "UPDATE studygroups SET numusers = numusers + 1 WHERE groupID = '" + groupID + "'";
                    stmt.executeUpdate(numUsersIncrease);

                    String insertUser = "INSERT INTO associations(`groupid`, `userid`, `roles`) VALUES ('" + groupID + "', " + requestUserID + "', ROLE_USER)";
                    stmt.executeUpdate(insertUser);

                    return "<h2><center>Successfully approved user request!</center></h2>";
                } else {
                    String requestDeny = "DELETE FROM requests WHERE requestuserid = '" + requestUserID + "' AND groupid = '" + groupID + "'";
                    stmt.executeUpdate(requestDeny);
                    return "<h2></center>You successfully denied the request!</center></h2>";
                }
            }


        } catch (Exception se) { se.printStackTrace(); }
        // Close Resources
        try {
            if (conn != null && stmt != null)
                conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return "";
    }



    /*
        Method: viewTable
        Purpose: This method will take in a result set and modify the data in a nice table format
        Parameter: result set which holds the data
        Parameter: title of the table that will store the user information
        Returns: a nice format for the information in the SQL table
     */
    public String viewTable(ResultSet rs, String title) throws SQLException {

        // Set up for creating table
        StringBuilder result = new StringBuilder();
        result.append("<table border=\"1\" \nalign=\"center\"> \n<caption>").append(title).append("</caption>");
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();

        // Getting column names from database as headers of table
        result.append("<tr>");
        for (int i = 1; i <= columnsNumber; i++) {
            result.append("<th>").append(rsmd.getColumnName(i)).append("</th>");
        }
        result.append("</tr>");

        // Obtaining database information stored in columns
        while (rs.next()) {
            result.append("<tr>");
            for (int i = 1; i <= columnsNumber; i++) {
                result.append("<td><center>").append(rs.getString(i)).append("</center</td>");
            }
            result.append("</tr>");
        }
        result.append("</table>");

        // Return filled table
        return result.toString();
    }

}
