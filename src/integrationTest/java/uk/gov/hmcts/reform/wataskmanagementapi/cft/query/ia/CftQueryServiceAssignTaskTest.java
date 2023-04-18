package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;

import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AllowedJurisdictionConfiguration.class)
@Testcontainers
@Sql("/scripts/ia/assign_task_data.sql")
public class CftQueryServiceAssignTaskTest extends RoleAssignmentHelper {

    private final List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @MockBean
    private CamundaService camundaService;
    @Autowired
    private TaskResourceRepository taskResourceRepository;

    private CftQueryService cftQueryService;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
                                              allowedJurisdictionConfiguration
        );
    }

    @Test
    void should_return_empty_task_resource_when_task_is_null() {
        final String taskId = null;
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("DIVORCE", "373"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired.add(MANAGE);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_task_resource_when_task_is_empty() {
        final String taskId = "";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .roleType(RoleType.CASE)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("DIVORCE", "373"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired.add(MANAGE);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_task_resource_when_permissions_are_missing() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .roleType(RoleType.CASE)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("DIVORCE", "373"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_task_resource_when_permissions_are_wrong() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .roleType(RoleType.CASE)
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("DIVORCE", "373"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired.add(PermissionTypes.EXECUTE);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_task_resource_when_authorisations_are_wrong_for_challenge_grant_type() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .roleType(RoleType.CASE)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("PROBATE", "SCSS"))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired.add(PermissionTypes.OWN);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_task_resource_when_authorisations_is_empty_for_challenge_grant_type() {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111018";
        final String caseId = "1623278362431018";
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), caseId
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(GrantType.CHALLENGED)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);
        permissionsRequired.add(MANAGE);

        final Optional<TaskResource> task = cftQueryService.getTask(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        Assertions.assertThat(task.isEmpty()).isTrue();
    }

}
