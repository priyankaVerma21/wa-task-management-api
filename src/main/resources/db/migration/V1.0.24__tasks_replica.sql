-- drops replication slot if exists and creates it
SELECT pg_drop_replication_slot(slot_name) FROM pg_replication_slots WHERE slot_name = 'test_slot_v1';
--SELECT pg_create_logical_replication_slot('test_slot_v1', 'pgoutput');

DROP PUBLICATION IF EXISTS task_publication;
CREATE PUBLICATION task_publication WITH (publish = 'insert,update,delete');
ALTER PUBLICATION task_publication ADD TABLE cft_task_db.tasks;

-- GRANT CONNECT ON DATABASE cft_task_db TO repl_user;
--GRANT USAGE ON SCHEMA cft_task_db TO repl_user;
--GRANT SELECT ON cft_task_db.tasks TO repl_user;
