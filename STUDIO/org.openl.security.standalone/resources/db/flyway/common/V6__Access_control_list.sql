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

ALTER TABLE OpenL_Group2Group
    ADD COLUMN level ${bigint};

UPDATE OpenL_Group2Group
SET level = 1
WHERE groupid = (SELECT id
                 from openl_groups
                 where groupname = 'Developers')
    and includedgroupid = (SELECT id
                           from openl_groups
                           where groupname = 'Viewers')
   OR groupid = (SELECT id
                 from openl_groups
                 where groupname = 'Testers')
    and includedgroupid = (SELECT id
                           from openl_groups
                           where groupname = 'Viewers')
   OR groupid = (SELECT id
                 from openl_groups
                 where groupname = 'Deployers')
    and includedgroupid = (SELECT id
                           from openl_groups
                           where groupname = 'Viewers')
   OR groupid = (SELECT id
                 from openl_groups
                 where groupname = 'Analysts')
    and includedgroupid = (SELECT id
                           from openl_groups
                           where groupname = 'Developers')
   OR groupid = (SELECT id
                 from openl_groups
                 where groupname = 'Analysts')
    and includedgroupid = (SELECT id
                           from openl_groups
                           where groupname = 'Testers');

INSERT INTO openl_group2group(groupid, includedgroupid, level)
VALUES ((select id from openl_groups where groupname = 'Analysts'),
        (select id from openl_groups where groupname = 'Viewers'),
        2);

ALTER TABLE OpenL_Group2Group
    ALTER COLUMN level SET not null;
