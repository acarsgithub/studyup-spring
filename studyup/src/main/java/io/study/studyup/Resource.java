package io.study.studyup;

import io.study.studyup.models.Group;
import io.study.studyup.models.Request;
import io.study.studyup.models.User;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

// TODO ALLOW USER TO ACCEPT OR DECLINE REQUESTS FROM MAIN PROFILE PAGE

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
        Method: login
        Purpose: Get method that redirects to custom login form
     */
    @GetMapping("/login")
    public String loginGet(){
        return "login";
    }


    /*
        Method: createAccountGet
        Purpose: Get method that redirects to the create account file
     */
    @GetMapping("/signup")
    public String createAccountGet(){
        System.out.println("Here!");
        return "createacc";
    }

    /*
        Method: signup
        Purpose: this method will allow for an individual to create an account with studyup
        HttpServletRequest request - this object should store 4 parameters
                     --- username of user creating account
                     --- password of user creating account
                     --- email of user creating account
                     --- classrank of user creating account (FRESHMAN, SOPHOMORE, JUNIOR, SENIOR)
        Conditionals: the username and email cannot already be taken

        TODO: forcing user to input secure password, must actually hash the password, validating classrank
     */
    @PostMapping("/signup")
    @ResponseBody
    public String createAccount(HttpServletRequest request) {

        // TODO make sure all fields are included!! from the post form
        System.out.println("Here2!");
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

            System.out.println("Here3!");

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
        Purpose: this method will do validation of request made to group and store request into hashmap list and database
        Principal - the currently logged in user
        PathVariable groupname - the nme of the groupname the currently logged in user is requesting to join

        TODO: must ensure groupname is properly typed in and a groupname that actually exists
     */
    @PostMapping("/request-group/{groupname}")
    @ResponseBody
    public String requestSend(@PathVariable("groupname") String groupname, Principal principal) {

        // prepared statement used to prevent SQL injection execution
        PreparedStatement obtainGroupAdminPS = null;
        PreparedStatement obtainUsersOfGroupPS = null;
        PreparedStatement obtainGroupInfoPS = null;
        PreparedStatement obtainRequestIDPS = null;
        PreparedStatement requestDatabasePS = null;
        PreparedStatement requesterExtractionPS = null;

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;

        // Variables to store information on group admin and requester
        String groupAdminUsername = "";
        String requesterUsername = principal.getName();
        int group_id = -1;
        int groupadmin_id = -1;
        int requester_id = -1;

        try {

            // Open connection and execute query
            conn = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/studyup", "root", pass);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            // obtaining the group admin username of the studygroup being requested to
            String obtainGroupAdmin = "SELECT groupadmin_username FROM studygroups WHERE groupname = ?";
            obtainGroupAdminPS = conn.prepareStatement(obtainGroupAdmin);
            obtainGroupAdminPS.setString(1, groupname);
            ResultSet adminIDRetrieval = obtainGroupAdminPS.executeQuery();

            while(adminIDRetrieval.next()){
                groupAdminUsername = adminIDRetrieval.getString("groupadmin_username");
            }


            // obtaining group requests and storing all requests into a list

            String obtainRequesters = "SELECT requestusername FROM requests WHERE groupname = ?";
            requesterExtractionPS = conn.prepareStatement(obtainRequesters);
            requesterExtractionPS.setString(1, groupname);
            ResultSet requesters = requesterExtractionPS.executeQuery();

            // checking all requests for group to ensure the user has not already requested this group
            while(requesters.next()){
                if(requesters.getString("requestusername").equals(requesterUsername)){
                    return "<h1>You have already sent a request to this group!</h1>";
                }
            }

            // checking if user is already a member of group they want to request to
            String allUserOfGroup = "SELECT username FROM associations WHERE groupname = ?";
            obtainUsersOfGroupPS = conn.prepareStatement(allUserOfGroup);
            obtainUsersOfGroupPS.setString(1, groupname);
            ResultSet users = obtainUsersOfGroupPS.executeQuery();

            while(users.next()){
                if(users.getString("username").equals(requesterUsername)){
                    return "<h1>You are already a member of this group!</h1>";
                }
            }

            // User is clear to request!
            // Getting groupId and groupAdminID for database entry
            String getGroupID = "SELECT groupid, groupadmin_id FROM studygroups WHERE groupname = ?";
            obtainGroupInfoPS = conn.prepareStatement(getGroupID);
            obtainGroupInfoPS.setString(1, groupname);
            ResultSet groupInfoRS = obtainGroupInfoPS.executeQuery();
            while(groupInfoRS.next()){
                group_id = groupInfoRS.getInt("groupid");
                groupadmin_id = groupInfoRS.getInt("groupadmin_id");
            }

            // getting request user id
            String requestId = "SELECT id FROM user WHERE username = ?";
            obtainRequestIDPS = conn.prepareStatement(requestId);
            obtainRequestIDPS.setString(1, requesterUsername);
            ResultSet requestIDRS = obtainRequestIDPS.executeQuery();
            while(requestIDRS.next()){
                requester_id = requestIDRS.getInt("id");
            }

            // Inserting info into requests table
            String requestAdditionSQL = "INSERT INTO requests(`groupname`, `groupid`, `groupadmin_username`, `groupadmin_id`, `requestuserid`, `requestusername`) " +
                    " VALUES (?, ?, ?, ?, ?, ?)";
            requestDatabasePS = conn.prepareStatement(requestAdditionSQL);
            requestDatabasePS.setString(1, groupname);
            requestDatabasePS.setInt(2, group_id);
            requestDatabasePS.setString(3, groupAdminUsername);
            requestDatabasePS.setInt(4, groupadmin_id);
            requestDatabasePS.setInt(5, requester_id);
            requestDatabasePS.setString(6, requesterUsername);
            requestDatabasePS.executeUpdate();


        } catch (Exception se) { se.printStackTrace(); }

        return "<h1>Request was successful!</h1>";
    }


    /*
        Method: createNewGroupGet
        Purpose: the get request triggers the creategroup HTML
     */
    @GetMapping("/create-group")
    public String createNewGroupGet(){
        return "creategroup";
    }


    /*
        Method: createNewGroup
        Purpose: this will allow the logged in user to create a new group and will place him as the admin of the group
        HttpServletRequest request - this object will store 3 parameters from post form
                    --- groupname will be the name of the group the user wants to create (should be unique)
                    --- subject will be the subject the studygroup will associate with
                    --- description should contain date and time of meetings and extra stuff the user mentions
        Principal -  this will be the currently logged in user

        TODO: ensure subject is a valid subject of classes at each university
     */
    @PostMapping("/create-group")
    @ResponseBody
    public String createNewGroup(HttpServletRequest request, Principal principal) {

        // parsing parameters from request object
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
            String adminAssoc = "INSERT INTO associations(`groupname`, `groupid`, `userid`, `username`, `roles`) VALUES (?, ?, ?, ?, 'ROLE_ADMIN')";
            associationPS = conn.prepareStatement(adminAssoc);
            associationPS.setString(1, groupname);
            associationPS.setInt(2, groupID);
            associationPS.setInt(3, realAdminID);
            associationPS.setString(4, principal.getName());
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

                groupInfo.close();
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
    public String userHome(@PathVariable("username") String username, Principal principal, Model model){

        // Connection and statement for SQL database
        Connection conn = null;
        Statement stmt = null;

        // stores users groups
        ArrayList<Group> userActiveGroups = new ArrayList<>();

        // stores all requests made to admin's groups
        ArrayList<Request> requestsMadeToAdmin = new ArrayList<>();

        // Getting logged in username
        String loggedInUser = principal.getName();

        // Prepared statements to prevent SQL Injection
        PreparedStatement getIDPS = null;
        PreparedStatement getGroupIDPS = null;
        PreparedStatement getAdminRequests = null;

        // Variable for data storage later
        int id = -1;

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

            // obtaining all requests from SQL database for principal
            // creating request objects out of those requests and adding them to a list
            String obtainRequests = "SELECT groupname, requestusername FROM requests WHERE groupadmin_username = ?";
            getAdminRequests = conn.prepareStatement(obtainRequests);
            getAdminRequests.setString(1, username);
            ResultSet allRequests = getAdminRequests.executeQuery();
            while(allRequests.next()){
                Request req = new Request();
                req.setRequester_username(allRequests.getString("requestusername"));
                req.setGroupname(allRequests.getString("groupname"));
                req.setGroupadmin_username(username);
                requestsMadeToAdmin.add(req);
            }

            // Getting groupID's of all groups associated with the user using prepared statement to prevent injection
            String findGroupsForID = "SELECT groupid FROM associations WHERE userid = ?";
            getGroupIDPS = conn.prepareStatement(findGroupsForID);
            getGroupIDPS.setInt(1, id);
            ResultSet groups = getGroupIDPS.executeQuery();

            // Putting every group user is in to list
            while(groups.next()){

                // Security measure against SQL Injection already set in place, groupid is auto incremented!
                // I believe I don't need to worry about this issue, but may need to come back for it later
                int groupID = groups.getInt("groupid");
                String extractGroupInfo = "SELECT * FROM studygroups WHERE groupid = '" + groupID + "'";

                // Formatting the data extarcted from the database
                if(stmt.execute(extractGroupInfo)) {

                    // Obtaining study groups of user from SQL database
                    ResultSet groupInfo = stmt.executeQuery(extractGroupInfo);
                    while(groupInfo.next()) {
                        Group group = new Group();
                        group.setGroupname(groupInfo.getString("groupname"));
                        group.setGroupadmin_id(groupInfo.getInt("groupadmin_id"));
                        group.setGroupadmin_username(groupInfo.getString("groupadmin_username"));
                        group.setNumusers(groupInfo.getInt("numusers"));
                        group.setSubject(groupInfo.getString("subject"));
                        group.setDescription(groupInfo.getString("description"));
                        userActiveGroups.add(group);
                    }

                    groupInfo.close();
                    model.addAttribute("allGroups", userActiveGroups);
                    model.addAttribute("allRequests", requestsMadeToAdmin);
                    return "usergroups";
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

        return "usergroups";
    }

}
