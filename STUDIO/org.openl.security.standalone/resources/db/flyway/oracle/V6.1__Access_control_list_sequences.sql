create SEQUENCE OpenL_Group_Access_Entry_ID_SEQ;
create or replace trigger OpenL_Group_Access_Entry_ID_TRG
before insert on OpenL_Group_Access_Entry
for each row
begin
  if :new.id is null then
select OpenL_Group_Access_Entry_ID_SEQ.nextval into :new.id from dual;
end if;
end;
/

create SEQUENCE OpenL_User_Access_Entry_ID_SEQ;
create or replace trigger OpenL_User_Access_Entry_ID_TRG
before insert on OpenL_User_Access_Entry
for each row
begin
  if :new.id is null then
select OpenL_User_Access_Entry_ID_SEQ.nextval into :new.id from dual;
end if;
end;
/

create SEQUENCE OpenL_Security_Objects_ID_SEQ;
create or replace trigger OpenL_Security_Objects_ID_TRG
before insert on OpenL_Security_Objects
for each row
begin
  if :new.id is null then
select OpenL_Security_Objects_ID_SEQ.nextval into :new.id from dual;
end if;
end;
/
