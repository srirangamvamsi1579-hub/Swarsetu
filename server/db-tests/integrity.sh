#!/usr/bin/env bash
# Proves the MySQL schema itself enforces college separation and the data rules.
# Usage: MYSQL="mysql -uroot" ./db-tests/integrity.sh   (creates and drops database swarsetu_schemachk)
set -u
MYSQL=${MYSQL:-"mysql -uroot"}
DB=swarsetu_schemachk
cd "$(dirname "$0")/.."
$MYSQL -e "DROP DATABASE IF EXISTS $DB; CREATE DATABASE $DB CHARACTER SET utf8mb4;"
$MYSQL $DB < src/main/resources/db/migration/V1__core_schema.sql || { echo "SCHEMA FAILED"; exit 1; }
$MYSQL $DB <<'SQL'
INSERT INTO colleges(id,name,name_key) VALUES ('RAM-7K2Q','Raga Academy of Music','ragaacademyofmusic'),('SMS-T8SG','The Swara Music School','swaramusicschool');
INSERT INTO users(id,college_id,username,email,mobile,first_name,role,status,password_hash) VALUES
 ('u1','RAM-7K2Q','Admin','admin@raga.in','9876500001','A','admin','active','x'),('u2','SMS-T8SG','priya','priya@gmail.com','9876500002','P','admin','active','x');
INSERT INTO courses(id,college_id,name) VALUES ('c1','RAM-7K2Q','Hindustani Vocal'),('c2','RAM-7K2Q','Sitar'),('c1','SMS-T8SG','Hindustani Vocal');
INSERT INTO batches(id,college_id,course_id,batch_key,name,class_type,mode,weekdays,start_time) VALUES ('b1','RAM-7K2Q','c1','k','HV','Group','Online','Mon','17:00');
INSERT INTO students(id,college_id,first_name,course_id,batch_id,class_type,mode,weekdays,start_time) VALUES
 ('STU0001-HIN','RAM-7K2Q','Aarav','c1','b1','Group','Online','Mon','17:00'),('STU0002-HIN','RAM-7K2Q','Diya','c1','b1','Group','Online','Mon','17:00');
INSERT INTO payments(id,college_id,student_id,batch_id,fee_month,total_fee,paid) VALUES ('p1','RAM-7K2Q','STU0001-HIN','b1','2026-11',1800,500);
INSERT INTO attendance_marks(college_id,student_id,class_date,code,saved_by) VALUES ('RAM-7K2Q','STU0002-HIN','2026-11-02','P','u1');
INSERT INTO payment_claims(id,college_id,student_id,fee_month,amount,method,reference,paid_on,submitted_by) VALUES ('k1','RAM-7K2Q','STU0001-HIN','2026-11',500,'UPI','123456789012','2026-11-20','u1');
SQL
pass=0; fail=0
expect() { # expect BLOCKED|ALLOWED "label" "sql"
  out=$($MYSQL $DB -e "$3" 2>&1); if echo "$out" | grep -q ERROR; then got=BLOCKED; else got=ALLOWED; fi
  if [ "$got" = "$1" ]; then pass=$((pass+1)); echo "ok    $1 $2"; else fail=$((fail+1)); echo "FAIL  expected $1, got $got: $2 $out"; fi
}
expect BLOCKED "student in college B using college A's course" "INSERT INTO students(id,college_id,first_name,course_id,class_type,mode,weekdays,start_time) VALUES ('STU0009-X','SMS-T8SG','H','c2','Group','Online','Mon','17:00');"
expect BLOCKED "payment in college B for college A's student" "INSERT INTO payments(id,college_id,student_id,fee_month,total_fee) VALUES ('p9','SMS-T8SG','STU0001-HIN','2026-11',1);"
expect BLOCKED "attendance in A saved by B's admin" "INSERT INTO attendance_marks(college_id,student_id,class_date,code,saved_by) VALUES ('RAM-7K2Q','STU0001-HIN','2026-11-09','P','u2');"
expect BLOCKED "user in B linked to A's student" "UPDATE users SET student_id='STU0001-HIN' WHERE id='u2';"
expect BLOCKED "duplicate college name (case/punctuation)" "INSERT INTO colleges(id,name,name_key) VALUES ('RAX-1111','RAGA academy of music!','ragaacademyofmusic');"
expect BLOCKED "username reused in another college (case differs)" "INSERT INTO users(id,college_id,username,email,first_name,role,password_hash) VALUES ('u3','SMS-T8SG','ADMIN','o@x.in','X','faculty','x');"
expect BLOCKED "mobile reused" "INSERT INTO users(id,college_id,username,email,mobile,first_name,role,password_hash) VALUES ('u4','SMS-T8SG','kiran','k@x.in','9876500001','K','faculty','x');"
expect BLOCKED "bad college code format" "INSERT INTO colleges(id,name,name_key) VALUES ('bad','Bad','bad');"
expect BLOCKED "paid more than total" "UPDATE payments SET paid=2000 WHERE id='p1';"
expect BLOCKED "delete course in use" "DELETE FROM courses WHERE college_id='RAM-7K2Q' AND id='c1';"
expect ALLOWED "delete unused course with same id in other college" "DELETE FROM courses WHERE college_id='SMS-T8SG' AND id='c1';"
expect BLOCKED "delete student with payments" "DELETE FROM students WHERE college_id='RAM-7K2Q' AND id='STU0001-HIN';"
expect BLOCKED "same UPI reference again" "INSERT INTO payment_claims(id,college_id,student_id,fee_month,amount,method,reference,paid_on,submitted_by) VALUES ('k2','RAM-7K2Q','STU0002-HIN','2026-11',500,'UPI','123456789012','2026-11-20','u1');"
expect BLOCKED "malformed UPI reference" "INSERT INTO payment_claims(id,college_id,student_id,fee_month,amount,method,reference,paid_on,submitted_by) VALUES ('k3','RAM-7K2Q','STU0002-HIN','2026-11',500,'UPI','12345','2026-11-20','u1');"
expect ALLOWED "reference reusable after rejection" "UPDATE payment_claims SET status='rejected' WHERE id='k1'; INSERT INTO payment_claims(id,college_id,student_id,fee_month,amount,method,reference,paid_on,submitted_by) VALUES ('k4','RAM-7K2Q','STU0002-HIN','2026-11',500,'UPI','123456789012','2026-11-21','u1');"
expect ALLOWED "delete student without payments cascades" "DELETE FROM payment_claims WHERE student_id='STU0002-HIN'; DELETE FROM students WHERE college_id='RAM-7K2Q' AND id='STU0002-HIN';"
left=$($MYSQL -N $DB -e "SELECT COUNT(*) FROM attendance_marks WHERE student_id='STU0002-HIN'")
[ "$left" = "0" ] && { pass=$((pass+1)); echo "ok    attendance removed with student"; } || { fail=$((fail+1)); echo "FAIL  attendance left: $left"; }
expect BLOCKED "uploaded scanner paying another UPI ID" "INSERT INTO college_payment_details(college_id,payee,upi_id,qr_mode,qr_image,qr_vpa,changed_by) VALUES ('RAM-7K2Q','Raga','raga@okicici','upload',x'00','other@ybl','u1');"
expect BLOCKED "calendar To before From" "INSERT INTO calendar_events(id,college_id,title,type,start_date,end_date,created_by) VALUES ('e1','RAM-7K2Q','X','Holiday','2026-12-10','2026-12-01','u1');"
expect BLOCKED "http meeting link" "UPDATE batches SET meet_link='http://meet.x' WHERE id='b1';"
st=$($MYSQL -N $DB -e "SELECT CONCAT(status,'/',dues) FROM payments WHERE id='p1'")
[ "$st" = "Partially paid/1300.00" ] && { pass=$((pass+1)); echo "ok    computed status and dues ($st)"; } || { fail=$((fail+1)); echo "FAIL  status/dues $st"; }
$MYSQL -e "DROP DATABASE $DB;"
echo "---- $pass passed, $fail failed"
[ $fail -eq 0 ]
