package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class TaskAutoAssignmentService {

    private final RoleAssignmentService roleAssignmentService;
    private final CftQueryService cftQueryService;

    public TaskAutoAssignmentService(RoleAssignmentService roleAssignmentService,
                                     CftQueryService cftQueryService) {
        this.roleAssignmentService = roleAssignmentService;
        this.cftQueryService = cftQueryService;
    }


    public TaskResource reAutoAssignCFTTask(TaskResource taskResource) {

        //if task is found and assigned
        if (StringUtils.isNotBlank(taskResource.getAssignee())) {

            //get role assignments for the user
            List<RoleAssignment> roleAssignmentsForUser =
                roleAssignmentService.getRolesByUserId(taskResource.getAssignee());

            //build and run the role assignment clause query with list of permissions required
            Optional<TaskResource> taskWithValidPermissions = cftQueryService
                .getTask(taskResource.getTaskId(), roleAssignmentsForUser, List.of(OWN, EXECUTE));

            //if existing user role assignments not have required permissions,then unassign and rerun autoassignment
            if (taskWithValidPermissions.isEmpty()) {
                taskResource.setAssignee(null);
                taskResource.setState(CFTTaskState.UNASSIGNED);
                return autoAssignCFTTask(taskResource);
            }
            return taskResource;
        }
        return autoAssignCFTTask(taskResource);
    }


    public TaskResource autoAssignCFTTask(TaskResource taskResource) {
        List<RoleAssignment> roleAssignments =
            roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource);

        //The query above may return multiple role assignments, and we must select the right one for auto-assignment.
        //
        //    Sort the list of role assignments returned by assignment priority (from the task permissions data).
        //    For each role assignment in this sorted order, if the task role resource permissions data
        //    for the role name of this role assignment has:
        //    - An empty list of authorisations then ignore the role assignment.
        //    - Contain authorizations then check if role-assignment contains at least one of them.
        //
        //The first role assignment which is not ignored is the role assignment to be used for auto-assignment.

        if (roleAssignments.isEmpty() || taskResource.getTaskRoleResources() == null) {
            taskResource.setAssignee(null);
            taskResource.setState(CFTTaskState.UNASSIGNED);
        } else {

            Optional<RoleAssignment> match = runRoleAssignmentAutoAssignVerification(taskResource, roleAssignments);

            if (match.isPresent()) {
                //The actorId of the role assignment selected in stage above is the IdAM ID of the user who is
                //to be assigned the task.
                taskResource.setAssignee(match.get().getActorId());
                taskResource.setState(CFTTaskState.ASSIGNED);
                taskResource.setAutoAssigned(true);
            } else {
                //If stage above produces an empty result of matching role assignment, then the task is
                //to be left unassigned.
                taskResource.setAssignee(null);
                taskResource.setState(CFTTaskState.UNASSIGNED);
            }
        }

        return taskResource;
    }

    public boolean checkAssigneeIsStillValid(TaskResource taskResource, String assignee) {

        List<RoleAssignment> roleAssignments = roleAssignmentService.getRolesByUserId(assignee);

        Optional<RoleAssignment> match = runRoleAssignmentAutoAssignVerification(taskResource, roleAssignments);
        return match.isPresent();

    }

    private Optional<RoleAssignment> runRoleAssignmentAutoAssignVerification(TaskResource taskResource,
                                                                             List<RoleAssignment> roleAssignments) {
        // the lowest assignment priority takes precedence.
        List<TaskRoleResource> rolesList = new ArrayList<>(taskResource.getTaskRoleResources());
        rolesList.sort(comparing(TaskRoleResource::getAssignmentPriority, nullsLast(Comparator.naturalOrder())));

        Map<String, TaskRoleResource> roleResourceMap = rolesList.stream()
            .collect(toMap(
                TaskRoleResource::getRoleName,
                taskRoleResource -> taskRoleResource,
                (a, b) -> b
            ));

        List<RoleAssignment> orderedRoleAssignments = orderRoleAssignments(rolesList, roleAssignments);
        return orderedRoleAssignments.stream()
            .filter(roleAssignment -> isRoleAssignmentValid(taskResource, roleResourceMap, roleAssignment)).findFirst();
    }

    private boolean isRoleAssignmentValid(TaskResource taskResource, Map<String,
        TaskRoleResource> roleResourceMap, RoleAssignment roleAssignment) {
        final TaskRoleResource taskRoleResource = roleResourceMap.get(roleAssignment.getRoleName());

        if (hasTaskBeenAssigned(taskResource, taskRoleResource)) {
            return findMatchingRoleAssignment(taskRoleResource, roleAssignment);
        } else if (isTaskRoleAutoAssignableWithNullOrEmptyAuthorisations(taskRoleResource)) {
            return true;
        } else if (isTaskRoleNotAutoAssignableOrAuthorisationsNotMatching(roleAssignment, taskRoleResource)) {
            return false;
        } else {
            return findMatchingRoleAssignment(taskRoleResource, roleAssignment);
        }
    }

    private boolean isTaskRoleNotAutoAssignableOrAuthorisationsNotMatching(RoleAssignment roleAssignment,
                                                                           TaskRoleResource taskRoleResource) {
        return !taskRoleResource.getAutoAssignable()
               || isAuthorisationsValid(taskRoleResource)
                  && !findMatchingRoleAssignment(taskRoleResource, roleAssignment);
    }

    private boolean isTaskRoleAutoAssignableWithNullOrEmptyAuthorisations(TaskRoleResource taskRoleResource) {
        return taskRoleResource.getAutoAssignable()
               && !isAuthorisationsValid(taskRoleResource);
    }

    private boolean hasTaskBeenAssigned(TaskResource taskResource, TaskRoleResource taskRoleResource) {
        return !taskRoleResource.getAutoAssignable()
               && taskResource.getAssignee() != null
               && isAuthorisationsValid(taskRoleResource);
    }

    private boolean isAuthorisationsValid(TaskRoleResource taskRoleResource) {
        return ArrayUtils.isNotEmpty(taskRoleResource.getAuthorizations());
    }

    private boolean findMatchingRoleAssignment(TaskRoleResource taskRoleResource, RoleAssignment roleAssignment) {
        AtomicBoolean hasMatch = new AtomicBoolean(false);
        Stream.of(taskRoleResource.getAuthorizations())
            .forEach(auth -> {
                //Safe-guard
                if (!hasMatch.get()
                    && roleAssignment.getAuthorisations() != null
                    && roleAssignment.getAuthorisations().contains(auth)) {
                    hasMatch.set(true);
                }
            });
        return hasMatch.get();
    }

    private List<RoleAssignment> orderRoleAssignments(List<TaskRoleResource> rolesList,
                                                      List<RoleAssignment> roleAssignments) {
        List<RoleAssignment> orderedRoleAssignments = new ArrayList<>();
        rolesList.forEach(role -> {
            List<RoleAssignment> filtered = roleAssignments.stream()
                .filter(ra -> role.getRoleName().equals(ra.getRoleName()))
                .collect(Collectors.toList());
            orderedRoleAssignments.addAll(filtered);
        });
        return orderedRoleAssignments;
    }

}