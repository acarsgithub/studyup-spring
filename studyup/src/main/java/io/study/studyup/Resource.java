package io.study.studyup;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.sql.*;

@Controller
public class Resource {

    final String pass = "";

    @GetMapping("/")
    @ResponseBody
    public String home(){
        return "<h2><center>Welcome!</center></h2>";
    }

    @PostMapping("/signup")
    @ResponseBody
    public String createAccount(@RequestBody String jsonStr) throws JSONException {

        // parsing json object
        JSONObject json = new JSONObject(jsonStr);
        String username = json.getString("username");
        String password = json.getString("password");
        String email = json.getString("email");
        String classrank = json.getString("classrank"); // classrank should be FRESHMAN, SOPHOMORE, JUNIOR, or SENIOR

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
                    " VALUES ('" + email + "','" + username + "', TRUE, '" + password + "', '" + classrank + "', 'ROLE_USER')";
            stmt.executeUpdate(addAccount);

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


    @PostMapping("/create-group")
    @ResponseBody
    public String createNewGroup(@RequestBody String jsonStr, Principal principal) throws JSONException {
        System.out.println(principal.getName());

        // parsing json object
        JSONObject json = new JSONObject(jsonStr);
        String groupname = json.getString("groupname");
        String subject = json.getString("subject");
        String description = json.getString("description");

        // Gets the username of the logged in individual
        String curUser = principal.getName();
        int realAdminID = -1;

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

            // Get id of user
            String adminID = "SELECT id FROM user WHERE username= '" + curUser + "'";
            ResultSet adminIDSet = stmt.executeQuery(adminID);
            while (adminIDSet.next()){
                realAdminID = adminIDSet.getInt("id");
            }


            // Add study group to database if it is not taken already
            String properId = "INSERT INTO studygroups(`groupname`, `groupadmin_username`, `groupadmin_id`, `numusers`, `subject`, `description`) " +
                    " VALUES ('" + groupname + "','" + curUser + "', '" + realAdminID + "', '" + 1 + "', '" + subject + "', '" + description + "')";
            stmt.executeUpdate(properId);

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
                groupInfoData = viewTable(groupInfo, "<h2><center>Study Groups/center></h2>");
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
