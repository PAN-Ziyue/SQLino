CREATE TABLE student (iden_number CHAR(18) UNIQUE, PRIMARY KEY(stuid),  stuid INT, score FLOAT, name CHAR(20));

DROP TABLE student;


CREATE INDEX studentid ON student ( stuid);

CREATE INDEX studentid ON student (name);

CREATE INDEX student_iden ON student (iden_number);


INSERT INTO student_iden  VALUES ('a', 178.6);  

INSERT INTO student  VALUES ('a', 178.6, 20, '7');

INSERT INTO student VALUES('3203021999', 385, 88.7, 'hey');

INSERT INTO student VALUES('3203181999', 473, 88.7, 'ziyue');
