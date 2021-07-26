INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('Root', 'WEBSTUDIO');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('designGit', 'REPOSITORY');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('designDB', 'REPOSITORY');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('designGit/DESIGN/rules/1234Project', 'PROJECT');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('designGit/DESIGN/rules/abcdeProject', 'PROJECT');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('designDB/DESIGN/rules/testProject', 'PROJECT');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('designDB/DESIGN/rules/checkProject', 'PROJECT');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('DESIGN/rules/1234Project/Models.xlsx', 'MODULE');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('DESIGN/rules/1234Project/Algorithms.xlsx', 'MODULE');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('DESIGN/rules/testProject/Models.xlsx', 'MODULE');

INSERT INTO Openl_Security_Objects(objectName, objectType)
VALUES ('DESIGN/rules/checkProject/AutoPolicyCalculation.xlsx', 'MODULE');

INSERT INTO OpenL_Groups(groupName)
VALUES ('ClassicGroup');

INSERT INTO OpenL_Groups(groupName)
VALUES ('AdminGroup');

INSERT INTO OpenL_Groups(groupName)
VALUES ('CustomGroup');

INSERT INTO Openl_Groups(groupName)
VALUES ('ParentGroup');

INSERT INTO Openl_Groups(groupName)
VALUES ('SubGroup');

INSERT INTO Openl_Group2Group(groupId, includedGroupId, level)
SELECT p.id, n.id, 1
FROM (SELECT id FROM OpenL_Groups WHERE groupName = 'ParentGroup') p,
     (SELECT id FROM OpenL_Groups WHERE groupName = 'SubGroup') n;

INSERT
INTO OpenL_Users (loginName, password)
VALUES ('SimpleAdmin', '$2a$10$Z4jC5mRqzw/1XlrW/LhLM.Os9Cd6eupg0GMJuMd7OzVqRUX26aaMW');

INSERT
INTO OpenL_Users (loginName, password)
VALUES ('John', '$2a$10$Z4jC5mRqzw/1XlrW/LhLM.Os9Cd6eupg0GMJuMd7OzVqRUX26aaMW');

INSERT INTO OpenL_Users(loginName, password)
VALUES ('Michael', '$2a$10$Z4jC5mRqzw/1XlrW/LhLM.Os9Cd6eupg0GMJuMd7OzVqRUX26aaMW');

INSERT INTO OpenL_Users(loginName, password)
VALUES ('Ann', '$2a$10$Z4jC5mRqzw/1XlrW/LhLM.Os9Cd6eupg0GMJuMd7OzVqRUX26aaMW');


INSERT INTO OpenL_User2Group(loginName, groupId)
SELECT 'SimpleAdmin', p.id
FROM (SELECT id FROM OpenL_Groups WHERE groupName = 'AdminGroup') p;


INSERT INTO Openl_User2Group(loginName, groupId)
SELECT 'John', g.id
FROM (SELECT id FROM OpenL_Groups WHERE groupName = 'ClassicGroup') g;

INSERT INTO openl_group_access_entry(groupid, accesslevel, objectid)
SELECT g.id, 'MANAGER', o.id
FROM (SELECT id from openl_groups WHERE groupname = 'ClassicGroup') g,
     (SELECT id from openl_security_objects WHERE objecttype = 'WEBSTUDIO') o;

INSERT INTO openl_user_access_entry(loginname, accesslevel, objectid)
SELECT 'John', 'FORBIDDEN', o.id
FROM (SELECT id from openl_security_objects WHERE objectname = 'DESIGN/rules/testProject/Models.xlsx') o;


INSERT INTO Openl_User2Group(loginName, groupId)
SELECT 'Michael', g.id
FROM (SELECT id FROM OpenL_Groups WHERE groupName = 'CustomGroup') g;

INSERT INTO openl_group_access_entry(groupid, accesslevel, objectid)
SELECT g.id, 'MANAGER', o.id
FROM (SELECT id from openl_groups WHERE groupname = 'CustomGroup') g,
     (SELECT id from openl_security_objects WHERE objecttype = 'WEBSTUDIO') o;

INSERT INTO openl_group_access_entry(groupid, accesslevel, objectid)
SELECT g.id, 'EDITOR', o.id
FROM (SELECT id from openl_groups WHERE groupname = 'CustomGroup') g,
     (SELECT id from openl_security_objects WHERE objectname = 'designGit') o;

INSERT INTO openl_user_access_entry(loginname, accesslevel, objectid)
SELECT 'Michael', 'DEPLOYER', o.id
FROM (SELECT id from openl_security_objects WHERE objectname = 'designGit/DESIGN/rules/1234Project') o;

INSERT INTO openl_user_access_entry(loginname, accesslevel, objectid)
SELECT 'Michael', 'VIEWER', o.id
FROM (SELECT id from openl_security_objects WHERE objectname = 'DESIGN/rules/1234Project/models') o;

INSERT INTO openl_user_access_entry(loginname, accesslevel, objectid)
SELECT 'Michael', 'FORBIDDEN', o.id
FROM (SELECT id from openl_security_objects WHERE objectname = 'DESIGN/rules/1234Project/algorithms') o;


INSERT INTO Openl_User2Group(loginName, groupId)
SELECT 'Ann', g.id
FROM (SELECT id FROM OpenL_Groups WHERE groupName = 'ParentGroup') g;

INSERT INTO Openl_User2Group(loginName, groupId)
SELECT 'Ann', g.id
FROM (SELECT id FROM OpenL_Groups WHERE groupName = 'SubGroup') g;

INSERT INTO openl_group_access_entry(groupid, accesslevel, objectid)
SELECT g.id, 'EDITOR', o.id
FROM (SELECT id from openl_groups WHERE groupname = 'ParentGroup') g,
     (SELECT id from openl_security_objects WHERE objecttype = 'WEBSTUDIO') o;

INSERT INTO openl_group_access_entry(groupid, accesslevel, objectid)
SELECT g.id, 'VIEWER', o.id
FROM (SELECT id from openl_groups WHERE groupname = 'SubGroup') g,
     (SELECT id from openl_security_objects WHERE objecttype = 'WEBSTUDIO') o;
