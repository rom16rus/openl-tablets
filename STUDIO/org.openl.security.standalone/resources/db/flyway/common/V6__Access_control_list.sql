DROP TABLE OpenL_Group_Authorities;

ALTER TABLE OpenL_Groups
    ADD COLUMN isAdmin ${char}(1) NOT NULL DEFAULT 'N';

UPDATE OpenL_Groups
SET isAdmin = 'Y'
WHERE groupname = 'Administrators';

ALTER TABLE OpenL_Groups
    ADD COLUMN isExternal ${char}(1) NOT NULL DEFAULT 'N';

CREATE TABLE OpenL_Security_Objects
(
    id         ${identity},
    objectName ${varchar}(100) NOT NULL,
    objectType ${varchar}(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (objectName, objectType)
);

CREATE TABLE OpenL_User_Access_Entry
(
    id          ${identity},
    loginName   ${varchar}(50),
    accessLevel ${varchar}(50) not null,
    objectId    ${bigint},
    PRIMARY KEY (id),
    UNIQUE (loginName, objectId),
    CONSTRAINT fk_OpenL_User_Access_Entry_OpenL_Security_Objects FOREIGN KEY (objectId) REFERENCES OpenL_Security_Objects (id) ON DELETE CASCADE,
    CONSTRAINT fk_OpenL_User_Access_Entry_OpenL_Users FOREIGN KEY (loginName) REFERENCES OpenL_Users (loginName) ON DELETE CASCADE
);

CREATE TABLE OpenL_Group_Access_Entry
(
    id          ${identity},
    groupId     ${bigint},
    accessLevel ${varchar}(50) not null,
    objectId    ${bigint},
    PRIMARY KEY (id),
    UNIQUE (groupId, objectId),
    CONSTRAINT fk_OpenL_Group_Access_Entry_OpenL_Security_Objects FOREIGN KEY (objectId) REFERENCES OpenL_Security_Objects (id) ON DELETE CASCADE,
    CONSTRAINT fk_OpenL_Group_Access_Entry_OpenL_Groups FOREIGN KEY (groupId) REFERENCES OpenL_Groups (id) ON DELETE CASCADE
);