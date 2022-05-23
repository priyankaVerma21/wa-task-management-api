package net.hmcts.taskperf.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.zalando.problem.violations.Violation;

import net.hmcts.taskperf.model.ClientQuery;
import net.hmcts.taskperf.model.Pagination;
import net.hmcts.taskperf.model.SearchRequest;
import net.hmcts.taskperf.model.User;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

@Service
public class TaskSearchAdaptor {

	private final CFTTaskMapper cftTaskMapper;
    private final TaskResourceRepository taskResourceRepository;

    public TaskSearchAdaptor(CamundaService camundaService,
                           CFTTaskMapper cftTaskMapper,
                           TaskResourceRepository taskResourceRepository) {
		this.cftTaskMapper = cftTaskMapper;
		this.taskResourceRepository = taskResourceRepository;
	}

    private Connection getConnection()
    {
    	throw new UnsupportedOperationException("Adaptor.getConnection needs to be implemented.");
    }

	/**
	 * Copied from CftQueryService and adapted.
	 */
    public GetTasksResponse<Task> searchForTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {
        validateRequest(searchTaskRequest);

//        Previous implementation:
//        Sort sort = SortQuery.sortByFields(searchTaskRequest);
//        Pageable page = OffsetPageableRequest.of(firstResult, maxResults, sort);
//
//        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
//            .buildTaskQuery(searchTaskRequest, accessControlResponse, permissionsRequired);
//
//        final Page<TaskResource> pages = taskResourceRepository.findAll(taskResourceSpecification, page);
//
//        final List<TaskResource> taskResources = pages.toList();
        // 1. Do the query to find a list of task IDs.
        // 1.1 Build the SearchRequest from the SearchTaskRequest and AccessControlResponse
        ClientQuery clientQuery = new ClientQuery(
        		getSearchParameterLists(searchTaskRequest),
        		getSearchParameterBooleans(searchTaskRequest),
        		permissionsRequired,
        		new Pagination(firstResult, maxResults),
        		searchTaskRequest.getSortingParameters());
        User user = new User(accessControlResponse.getRoleAssignments());
        SearchRequest searchRequest = new SearchRequest(clientQuery, user);
        try
        {
            // 1.2 Run the search and build an ordered list of the task IDs.
        	TaskSearch.Results results = TaskSearch.searchTasks(searchRequest, getConnection(), false);
	        List<String> orderedTaskIds = results.getTasks().stream()
	        		.map(t -> t.getAttributes().get("task_id").toString())
	        		.collect(Collectors.toList());
	        // 2. Retrieve the full TaskResource data for each task ID, in the right order.
	        List<TaskResource> taskResources = getTaskResources(orderedTaskIds);
	
	        final List<Task> tasks = taskResources.stream()
	            .map(taskResource ->
	                cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
	                    taskResource,
	                    accessControlResponse.getRoleAssignments())
	            )
	            .collect(Collectors.toList());
	
	        return new GetTasksResponse<>(tasks, results.getTotalCount());
        }
        catch (SQLException e)
        {
        	// TODO: handle properly
        	throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the TaskResource entities for the list of task IDs,
     * maintaining the order.  (There is no reason to believe that the
     * TaskResourceRepository guarantees to preserve the order of the
     * list of IDs passed in.)
     */
    private List<TaskResource> getTaskResources(List<String> orderedTaskIds)
    {
    	Map<String, TaskResource> taskResourcesByTaskId = new HashMap<>();
    	for (TaskResource taskResource : taskResourceRepository.findAllById(orderedTaskIds))
    	{
    		taskResourcesByTaskId.put(taskResource.getTaskId(), taskResource);
    	}
    	List<TaskResource> orderedTaskResources = new ArrayList<>();
    	for (String taskId : orderedTaskIds)
    	{
    		orderedTaskResources.add(taskResourcesByTaskId.get(taskId));
    	}
    	return orderedTaskResources;
    }

    private static List<SearchParameterList> getSearchParameterLists(SearchTaskRequest searchTaskRequest)
    {
    	List<SearchParameterList> parameterLists = new ArrayList<>();
    	for (SearchParameter<?> searchParameter : searchTaskRequest.getSearchParameters())
    	{
    		if (searchParameter instanceof SearchParameterList)
    		{
    			parameterLists.add((SearchParameterList)searchParameter);
    		}
    	}
    	return parameterLists;
    }

    private static List<SearchParameterBoolean> getSearchParameterBooleans(SearchTaskRequest searchTaskRequest)
    {
    	List<SearchParameterBoolean> parameterBooleans = new ArrayList<>();
    	for (SearchParameter<?> searchParameter : searchTaskRequest.getSearchParameters())
    	{
    		if (searchParameter instanceof SearchParameterBoolean)
    		{
    			parameterBooleans.add((SearchParameterBoolean)searchParameter);
    		}
    	}
    	return parameterBooleans;
    }

	/**
	 * Copied unchanged from CftQueryService.
	 */
	private void validateRequest(SearchTaskRequest searchTaskRequest) {
	    List<Violation> violations = new ArrayList<>();
	
	    //Validate work-type
	    List<SearchParameterList> workType = new ArrayList<>();
	    for (SearchParameter<?> sp : searchTaskRequest.getSearchParameters()) {
	        if (sp.getKey().equals(SearchParameterKey.WORK_TYPE)) {
	            workType.add((SearchParameterList) sp);
	        }
	    }
	
	    if (!workType.isEmpty()) {
	        //validate work type
	        SearchParameterList workTypeParameter = workType.get(0);
	        List<String> values = workTypeParameter.getValues();
	        //Validate
	        values.forEach(value -> {
	            if (!CftQueryService.ALLOWED_WORK_TYPES.contains(value)) {
	                violations.add(new Violation(
	                    value,
	                    workTypeParameter.getKey() + " must be one of " + Arrays.toString(CftQueryService.ALLOWED_WORK_TYPES.toArray())
	                ));
	            }
	        });
	    }
	
	    if (!violations.isEmpty()) {
	        throw new CustomConstraintViolationException(violations);
	    }
	}

}
