insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111017', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431017', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'hearing_work', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115019', 'tribunal-caseworker', true, false , false , true, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111017', '2021-05-09T20:15:45.345875+01:00');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115021', 'senior-tribunal-caseworker', true, true, true, false , false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111017', '2021-05-09T20:15:45.345875+01:00');

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111018', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431018', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'hearing_work', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115020', 'tribunal-caseworker', true, false , false, true, false , false, '{"DIVORCE", "IA"}',
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111018', '2021-05-09T20:15:45.345875+01:00');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115022', 'senior-tribunal-caseworker', true, true, true , false , false , false, '{"DIVORCE", "IA"}',
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111018', '2021-05-09T20:15:45.345875+01:00');