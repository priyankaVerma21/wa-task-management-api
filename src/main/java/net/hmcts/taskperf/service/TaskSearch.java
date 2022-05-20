package net.hmcts.taskperf.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Value;
import net.hmcts.taskperf.model.Pagination;
import net.hmcts.taskperf.model.SearchRequest;
import net.hmcts.taskperf.model.SortBy;
import net.hmcts.taskperf.service.sql.SqlStatement;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;

/**
 * Main task for performing a task search:
 *   1. Runs the search of task headers.
 *   2. Collates the results in a TaskPaginator, sorting and paginating dynamically.
 *   3. Retrieves the full task data for each task in the requested page of results.
 *   4. Returns all the task data and a count of the total of matching tasks in the database.
 */
public class TaskSearch
{
	@Value
	public static class Results
	{
		public final int totalCount;
		public final List<Task> tasks;
		public final List<String> queryPlans;
		public final List<Event> log;
	}

	@Value
	public static class Task
	{
		private Map<String, Object> attributes;
	}

	@Value
	public static class Event
	{
		private String description;
		private long time;
	}

	/**
	 * System timestamp when the search was initiated.
	 */
	private final long startTime;

	/**
	 * Keep track of the timings.
	 */
	private final List<Event> log = new ArrayList<>();

	/**
	 * Determines whether to explain the queries as well as running them.
	 */
	private final boolean explainQueries;

	/**
	 * Determines whether to analyse the queries as well as running them.
	 */
	private final List<String> queryPlans = new ArrayList<>();

	/**
	 * Total number of tasks matching the search.
	 */
	private int totalCount;

	/**
	 * The tasks returned from the search
	 */
	private final List<Task> tasks = new ArrayList<>();

	/**
	 * The SQL statement to retrieve the requested page of tasks.
	 */
	private final SqlStatement searchStatement = new SqlStatement();

	/**
	 * The SQL statement to retrieve the count of tasks matching the search.
	 */
	private final SqlStatement countStatement = new SqlStatement();

	/**
	 * The signatures of the  role assignments which need to be matched.
	 */
	private Set<String> roleSignatures = new HashSet<>();

	/**
	 * The signatures of the  filter criteria which need to be matched.
	 */
	private Set<String> filterSignatures = new HashSet<>();

	/**
	 * Extra constraint clauses to be added to the SQL.
	 */
	private String extraConstraints = "";

	/**
	 * Parameters to accompany the extra constraint clauses.
	 */
	private final List<Object> extraConstraintParameters = new ArrayList<>();

	/**
	 * The order by criteria for the search.
	 */
	private final List<SortBy> orderBy;

	/**
	 * The offset to request in the SQL.
	 */
	private int offset;

	/**
	 * The limit to request in the SQL.
	 */
	private int limit;

	public static Results searchTasks(SearchRequest searchRequest, Connection connection, boolean explainQueries) throws SQLException
	{
		TaskSearch taskSearch = new TaskSearch(searchRequest, explainQueries);
		taskSearch.run(connection);
		return new Results(taskSearch.totalCount, taskSearch.tasks, taskSearch.queryPlans, taskSearch.log);
	}

	private TaskSearch(SearchRequest searchRequest, boolean explainQueries)
	{
		this.startTime = System.currentTimeMillis();
		log("Started");
		this.explainQueries = explainQueries;
		Set<RoleAssignment> roleAssignments = RoleAssignmentHelper.filterRoleAssignments(searchRequest);
		this.roleSignatures = RoleAssignmentHelper.buildRoleSignatures(roleAssignments, searchRequest.getQuery().getFilter().getPermissions());
		this.filterSignatures = FilterHelper.buildFilterSignatures(searchRequest.getQuery().getFilter());
		buildExtraConstraints(searchRequest, SEARCH_SQL_TASK_ALIAS);
		Pagination pagination = searchRequest.getQuery().getPagination();
		offset = (pagination.getPageNumber() - 1) * pagination.getPageSize();
		limit = pagination.getPageSize();
		orderBy = searchRequest.getQuery().getSort();
		log("Starting to build queries");
		buildSearchSqlStatement();
		log("Search query built");
		buildCountSqlStatement();
		log("Count query built");
	}

	private void log(String description)
	{
		log.add(new Event(description, System.currentTimeMillis() - startTime));
	}

	private void run(Connection connection) throws SQLException
	{
		if (explainQueries)
		{
			log("Explaining search");
			explainSearch(connection);
			log("Finished explaining search");
			log("Explaining count");
			explainCount(connection);
			log("Finished explaining count");
		}
		log("Starting search");
		runSearch(connection);
		log("Finished search");
		log("Starting count");
		runCount(connection);
		log("Finished count");
	}

	/**
	 * The SQL for doing the actual search for tasks.
	 */
	private static final String SEARCH_SQL = "" +	
		    "select t2.*,\n" +
		    "       (select array_agg(array[r.role_name, r.read::text||r.own::text||r.execute::text||r.manage::text||r.cancel::text] || r.authorizations)\n" +
		    "        from cft_task_db.task_roles r where r.task_id = t2.task_id) as permissions\n" +
		    "from   cft_task_db.tasks t2\n" +
		    "where  t2.task_id in (\n" +
		    "        select t.task_id\n" +
		    "        from   cft_task_db.tasks t\n" +
		    "        where  indexed\n" +
		    "        and    state in ('ASSIGNED','UNASSIGNED')\n" +
		    "        and    cft_task_db.filter_signatures(task_id) && ?\n" +
		    "        and    cft_task_db.role_signatures(task_id) && ?[EXTRA_CONSTRAINTS]\n" +
		    "        order by [ORDER_BY]t.major_priority desc, t.priority_date_time desc, t.minor_priority desc\n" +
		    "        offset ? limit ?)";

	private static final String SEARCH_SQL_TASK_ALIAS = "t";

	private void buildSearchSqlStatement()
	{
		String sql = SEARCH_SQL;
		sql = addOrderBy(sql, orderBy, SEARCH_SQL_TASK_ALIAS);
		sql = addExtraConstraints(sql, extraConstraints);
		searchStatement.append(sql);
		searchStatement.addParameter(filterSignatures);
		searchStatement.addParameter(roleSignatures);
		for (Object parameter : extraConstraintParameters)
		{
			searchStatement.addParameter(parameter);
		}
		searchStatement.addParameter(offset);
		searchStatement.addParameter(limit);
	}

	private void buildExtraConstraints(SearchRequest searchRequest, String alias)
	{
		Set<String> caseIds = searchRequest.getQuery().getFilter().getCaseIds();
		if (caseIds != null && !caseIds.isEmpty())
		{
			if (caseIds.size() == 1)
			{
				extraConstraints += "\nand " + alias + ".case_id = ?";
				extraConstraintParameters.add(caseIds.stream().findFirst().get());
			}
			else
			{
				extraConstraints += "\nand " + alias + ".case_id in ?";
				extraConstraintParameters.add(caseIds);
			}
		}
		Set<String> assignees = searchRequest.getQuery().getFilter().getAssignees();
		if (assignees != null && !assignees.isEmpty())
		{
			if (assignees.size() == 1)
			{
				extraConstraints += "\nand " + alias + ".assignee = ?";
				extraConstraintParameters.add(assignees.stream().findFirst().get());
			}
			else
			{
				extraConstraints += "\nand " + alias + ".assignee in ?";
				extraConstraintParameters.add(assignees);
			}
		}
		Set<String> excludedCaseIds = buildExcludedCaseIds(RoleAssignmentHelper.exclusionRoleAssignments(searchRequest));
		if (!excludedCaseIds.isEmpty())
		{
			if (excludedCaseIds.size() == 1)
			{
				extraConstraints += "\nand not " + alias + ".case_id = ?";
				extraConstraintParameters.add(excludedCaseIds.stream().findFirst().get());
			}
			else
			{
				extraConstraints += "\nand not " + alias + ".case_id = any(?)";
				extraConstraintParameters.add(excludedCaseIds);
			}
		}
	}

	private Set<String> buildExcludedCaseIds(Set<RoleAssignment> roleAssignments)
	{
		return
				roleAssignments.stream()
				.filter(ra -> ra.getGrantType() == GrantType.EXCLUDED)
				.map(ra -> ra.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()))
				.filter(c -> c != null)
				.collect(Collectors.toSet());
	}

	private static String addOrderBy(String sql, List<SortBy> sort, String alias)
	{
		String orderColumns = "";
		if (sort != null)
		{
			for (SortBy sortBy : sort)
			{
				orderColumns += alias + "." + sortBy.getColumn().value() + " " + sortBy.getDirection().value() + ", ";
			}
		}
		return sql.replace("[ORDER_BY]", orderColumns);
	}

	private static String addExtraConstraints(String sql, String extraConstraints)
	{
		return sql.replace("[EXTRA_CONSTRAINTS]", extraConstraints);
	}

	private void runSearch(Connection connection) throws SQLException
	{
		searchStatement.execute(connection, r -> tasks.add(makeTask(r)));
	}

	private void explainSearch(Connection connection) throws SQLException
	{
		queryPlans.add("SEARCH QUERY");
		queryPlans.add("============");
		queryPlans.addAll(searchStatement.explain(connection));
	}

	private Task makeTask(ResultSet results)
	{
		try
		{
			Map<String, Object> attributes = new HashMap<>();
			ResultSetMetaData metadata = results.getMetaData();
			for (int i = 1; i <= metadata.getColumnCount(); ++i)
			{
				attributes.put(results.getMetaData().getColumnName(i), results.getObject(i));
			}
			return new Task(attributes);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * The SQL for counting the total of matching tasks.
	 */
	private static String COUNT_SQL = "" +
			"select count(*) as count\n" +
			"from   cft_task_db.tasks t\n" + 
			"where  indexed\n" +
			"and    state in ('ASSIGNED','UNASSIGNED')\n" +
			"and    cft_task_db.filter_signatures(task_id) && ?\n" +
			"and    cft_task_db.role_signatures(task_id) && ?[EXTRA_CONSTRAINTS]";

	private void buildCountSqlStatement()
	{
		String sql = addExtraConstraints(COUNT_SQL, extraConstraints);
		countStatement.append(sql);
		countStatement.addParameter(filterSignatures);
		countStatement.addParameter(roleSignatures);
		for (Object parameter : extraConstraintParameters)
		{
			countStatement.addParameter(parameter);
		}
	}

	private void runCount(Connection connection) throws SQLException
	{
		countStatement.execute(connection, r -> totalCount = getCount(r));
	}

	private void explainCount(Connection connection) throws SQLException
	{
		queryPlans.add("COUNT QUERY");
		queryPlans.add("============");
		queryPlans.addAll(countStatement.explain(connection));
	}

	private int getCount(ResultSet results)
	{
		try
		{
			return results.getInt("count");
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}