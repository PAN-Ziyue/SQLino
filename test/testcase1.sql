CREATE TABLE student (iden_number CHAR(18) UNIQUE, PRIMARY KEY(stuid),  stuid INT, score FLOAT, name CHAR(20));

DROP TABLE student;


CREATE INDEX studentid ON student ( stuid);

CREATE INDEX studentid ON student (name);

CREATE INDEX student_iden ON student (iden_number);


INSERT INTO student_iden  VALUES ('a', 178.6);  

INSERT INTO student  VALUES ('a', 178.6, 20, '7');

INSERT INTO student VALUES('3203021999', 385, 88.7, 'hey');

INSERT INTO student VALUES('3203181999', 473, 88.7, 'ziyue');

SELECT * FROM student WHERE iden_number != '3203181999' AND name = 'hey';

SELECT * FROM student WHERE score = 88.7;

SELECT * FROM student WHERE stuid = 385 AND name = 'ziyue' ;