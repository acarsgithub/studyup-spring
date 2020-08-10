package io.study.studyup;

import io.study.studyup.models.Group;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import java.security.Principal;
import java.sql.*;
import java.util.ArrayList;

@Controller
public class Resource {

    final String pass = "";

    /*
        Method: home
        Purpose: temporary homepage for everyone to access when introduced to app
     */
    @GetMapping("/")
    public String home() {
        return "landing" ;
    }

    /*
        Method: createAccountGet
     */
    @GetMapping("/signup")
    public String createAccountGet(){
        return "createacc";
    }

    /*
        Method: signup
        Purpose: this method will allow for an individual to create an account with studyup
        RequestBody jsonStr - this parameter is a json datatype that should store 4 key-value pairs
                     --- username of user creating account
                     --- password of user creating account
                     --- email of user creating account
                     --- classrank of user creating account (has to be FRESHMAN, SOPHOMORE, JUNIOR, SENIOR)
        Conditionals: the username and email cannot already be taken

        NEEDED: forcing user to input secure password, must actually hash the password, validating classrank
     */
    @PostMapping("/signup")
    @ResponseBody
    public String createAccount(HttpServletRequest request) throws JSONException {

        // parsing json object
        // signing up requires username, password, email, and classrank
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String classrank = request.getParameter("classrank"); // classrank should be FRESHMAN, SOPHOMORE, JUNIOR, or SENIOR

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


    /*
        Method: requestSend
        Purpose: this method will allow users to request joining a studygroup
        RequestBody jsonStr - this json will store one key-value pair
                    --- groupname - the group the user is requesting to join

        NEEDED: must ensure groupname is properly typed in and a groupname that actually exists
     */
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


    /*
        Method: createNewGroupGet
     */
    @GetMapping("/create-group")
    public String createNewGroupGet(){
        return "creategroup";
    }


    /*
        Method: createNewGroup
        Purpose: this will allow the logged in user to create a new group and will place him as the admin of the group
        RequestBody jsonStr - this json will store 3 key-value pairs
                    --- groupname will be the name of the group the user wants to create (should be unique)
                    --- subject will be the subject the studygroup will associate with
                    --- description should contain date and time of meetings and extra stuff the user mentions
        Principal -  this will be the currently logged in user

        NEEDED: ensure subject is a valid subject of classes at each university
     */
    @PostMapping("/create-group")
    @ResponseBody
    public String createNewGroup(HttpServletRequest request, HttpServletResponse response/*@RequestBody String jsonStr*/, Principal principal) throws JSONException {

        // parsing json object
        // JSONObject json = new JSONObject(jsonStr);
        String groupname = request.getParameter("groupname");
        String subject = request.getParameter("subject");
        String description = request.getParameter("description");

        // Gets the username of the logged in individual
        String curUser = principal.getName();

        // Variables for storing data later
        int realAdminID = -1;
        int groupID = -1;

        // Prepared statements for protection against SQL Injection
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


    /*
        Method: allGroupsHome
        Purpose: this method will display all groups in existence currently and will display the group admin, the num users,
            the groupname, the subject of each group, and the description of each group
     */
    @GetMapping("/groups")
    public String allGroupsHome(Model model){

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;
        ArrayList<Group> groups = new ArrayList<>();


        try {
            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            // Get group information
            String extractGroupInfo = "SELECT * FROM studygroups";

            // Formatting data
            if(stmt.execute(extractGroupInfo)) {
                // Obtaining study group data from SQL database
                ResultSet groupInfo = stmt.executeQuery(extractGroupInfo);
                while(groupInfo.next()) {
                    Group group = new Group();
                    group.setGroupname(groupInfo.getString("groupname"));
                    group.setGroupadmin_id(groupInfo.getInt("groupadmin_id"));
                    group.setGroupadmin_username(groupInfo.getString("groupadmin_username"));
                    group.setNumusers(groupInfo.getInt("numusers"));
                    group.setSubject(groupInfo.getString("subject"));
                    group.setDescription(groupInfo.getString("description"));
                    groups.add(group);
                }
                // Accessing helper method to create table
                // groupInfoData = viewTable(groupInfo, "<h2><center>Study Groups</center></h2>");
                // groupInfo.close();
                model.addAttribute("allGroups", groups);
                return "groups";
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
        return "groups";
    }


    /*
        Method: userHome
        Purpose: this method will return  all groups associated with the user to show the user all the active groups they are in
        Principal - the currently logged in user
        PathVariable username - the username of the person's groups to display... MUST BE currently logged in user or access is restricted
     */
    @GetMapping("/{username}")
    @ResponseBody
    public String userHome(@PathVariable("username") String username, Principal principal){

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;

        // Getting logged in username
        // String loggedInUser = principal.getName();
        String loggedInUser = username;

        // Prepared statements to prevent SQL Injection
        PreparedStatement getIDPS = null;
        PreparedStatement getGroupIDPS = null;

        // Variable for data storage later
        int id = -1;
        StringBuilder data = new StringBuilder();

        // Determining if logged in user matches username path variable give
        if(!loggedInUser.equals(username)){
            return "<h2><center>You do not have access to this individual's page!</center></h2>";
        }

        try {
            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            // Get id of user using prepared statement to prevent SQL Injection
            String getUserID = "SELECT id FROM user WHERE username = ?";
            getIDPS = conn.prepareStatement(getUserID);
            getIDPS.setString(1, username);
            ResultSet userIDSet = getIDPS.executeQuery();

            // Getting actual id and storing it
            while(userIDSet.next()){
                id = userIDSet.getInt("id");
            }

            // Getting groupID's of all groups associated with the user using prepared statement to prevetn injection
            String findGroupsForID = "SELECT groupid FROM associations WHERE userid = ?";
            getGroupIDPS = conn.prepareStatement(findGroupsForID);
            getGroupIDPS.setInt(1, id);
            ResultSet groups = getGroupIDPS.executeQuery();

            // Putting every group user is in in its own table to display
            while(groups.next()){

                // Security measure against SQL Injection already set in place, groupid is auto incremented!
                // I believe I don't need to worry about this issue, but may need to come back for it later
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

        // Printing out data
        return data.toString();
    }


    /*
        Method: userRequests
        Purpose: this method will display all requests a user has (if they are an admin of any groups) and will allow them
            to accept of decline requests depending on if optional parameters are passed in
        PathVariable username - the username of the person's requests to see.. MUST BE logged in user or access forbidden
        Principal - the currently logged in user
        OptionalRequestParam requestUserID - this is the requesterID the user will be accepting/rejecting to group
        OptionalRequestParam groupID - this is the groupID associated with the requester
        OptionalRequestParam decision - this is the decision of the user for the requester and the group


        NEEDED: check all 3 optional parameters, ensure groupID and requesterID are assoicated in requests table,
            possibly convert to POST method

     */
    @GetMapping("/{username}/requests")
    @ResponseBody
    public String userRequests(@PathVariable("username") String username, Principal principal,
                               @RequestParam(defaultValue = "-1", value = "requestID", required = false) int requestUserID,
                               @RequestParam(value= "groupID", required=false) String groupID,
                               @RequestParam(value= "decision", required=false) boolean decision){

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;

        // Using principal to get logged in username
        String loggedInUser = principal.getName();

        // Prepared Statements for protection against SQL Injection
        PreparedStatement getIDPS = null;
        PreparedStatement getRequests = null;
        PreparedStatement deleteRequest1 = null;
        PreparedStatement updateStudyGroupPS = null;
        PreparedStatement updateAssociationsPS = null;

        // Variables to store data later
        int id = -1;
        StringBuilder data = new StringBuilder();

        // Determining if logged in user has access to path variable username given
        if(!loggedInUser.equals(username)){
            return "<h2><center>You do not have access to this individual's page!</center></h2>";
        }

        try {
            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            // If optional parameter request is not given, continue forward here
            if(requestUserID == -1){

                // Get user id SQL statement
                String getUserID = "SELECT id FROM user WHERE username = ?";

                // Using prepared statement to prevent SQL injection
                getIDPS = conn.prepareStatement(getUserID);
                getIDPS.setString(1, loggedInUser);
                ResultSet userIDSet = getIDPS.executeQuery();

                // Saving id to variable
                while(userIDSet.next()){
                    id = userIDSet.getInt("id");
                }

                // SQL statement to show all requests of logged in user
                String findGroupsForID = "SELECT requestuserid, groupname FROM requests WHERE groupadmin_id = ?";

                // Using prepared statement to prevent SQL Injection
                getRequests = conn.prepareStatement(findGroupsForID);
                getRequests.setInt(1, id);
                ResultSet groups = getRequests.executeQuery();

                // Passing in result set and returning table of requests to user
                return viewTable(groups, "<h2><center>All Requests For " + username + "</center></h2>");

            } else {

                // Deleting request from requests table
                String requestDelete = "DELETE FROM requests WHERE requestuserid = ? AND groupid = ?";

                // Using prepared statement to prevent SQL Injection
                deleteRequest1 = conn.prepareStatement(requestDelete);
                deleteRequest1.setInt(1, requestUserID);
                deleteRequest1.setString(2, groupID);
                deleteRequest1.executeUpdate();

                // If optional parameters given, determine if decision is yes or no
                if (decision){

                    // Updating numusers in studygroup since request is accepted
                    String numUsersIncrease = "UPDATE studygroups SET numusers = numusers + 1 WHERE groupID = ?";
                    updateStudyGroupPS = conn.prepareStatement(numUsersIncrease);
                    updateStudyGroupPS.setString(1, groupID);
                    updateStudyGroupPS.executeUpdate();

                    // Adding accepted user as an association in the table
                    String insertUser = "INSERT INTO associations(`groupid`, `userid`, `roles`) VALUES (?, ?, ROLE_USER)";
                    updateAssociationsPS = conn.prepareStatement(insertUser);
                    updateAssociationsPS.setString(1, groupID);
                    updateAssociationsPS.setInt(2, requestUserID);
                    updateAssociationsPS.executeUpdate();

                    // Successfully accepting user to studygroup
                    return "<h2><center>Successfully approved user request!</center></h2>";

                } else {

                    // Say you've successfully deleted the request
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

        // Just needed for function to not cause error
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
