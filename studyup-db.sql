drop database if exists studyup;
CREATE DATABASE IF NOT EXISTS studyup;
USE studyup;


DROP TABLE If EXISTS user;
CREATE TABLE user (
	  id          	int 	    		auto_increment			PRIMARY KEY,
      email			VARCHAR(255)		NOT NULL,
	  username      VARCHAR(255)        NOT NULL,
	  active    	BOOLEAN    			NOT NULL,
	  password     	VARCHAR(255)   		NOT NULL,
      classrank		VARCHAR(255)		NOT NULL				CHECK(classrank IN('FRESHMAN', 'SOPHOMORE', 'JUNIOR', 'SENIOR')),
      roles    		VARCHAR(255)   		NOT NULL				CHECK(roles IN('ROLE_USER', 'ROLE_ADMIN'))
);


DROP TABLE If EXISTS studygroups;
CREATE TABLE studygroups (
	  groupid       			int 	    		auto_increment			PRIMARY KEY,
      groupname					VARCHAR(255)		NOT NULL,
      groupadmin_username		VARCHAR(255)		NOT NULL,
      groupadmin_id				int					NOT NULL,
	  numusers     				int        			NOT NULL,
	  subject    				VARCHAR(255)    	NOT NULL,
      description    			VARCHAR(255)   		NOT NULL
);


DROP TABLE If EXISTS requests;
CREATE TABLE requests (
	  groupname					VARCHAR(255)		NOT NULL,
	  groupid       			int 	    		NOT NULL,
      groupadmin_id				int					NOT NULL,
      groupadmin_username		VARCHAR(255) 		NOT NULL,
	  requestuserid				int 				NOT NULL,
      requestusername			VARCHAR(255) 		NOT NULL
);


DROP TABLE If EXISTS associations;
CREATE TABLE associations (
	  groupname					VARCHAR(255)		NOT NULL,
	  groupid       			int 	    		NOT NULL,
      userid					int					NOT NULL,
      username					VARCHAR(255) 		NOT NULL,
	  roles						VARCHAR(255)		CHECK(roles IN('ROLE_USER', 'ROLE_ADMIN'))
);

