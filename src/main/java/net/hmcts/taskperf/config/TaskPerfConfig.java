package net.hmcts.taskperf.config;

public class TaskPerfConfig
{
	/*
	 * Change from using different role signatures for organisational and case roles
	 * to using a standard format for all role assignments.  Expected to be slightly
	 * less well-performing, but more generic (and therefore less subject to
	 * unexpected behaviour when services introduce new types of role).
	 */
	public static boolean useUniformRoleSignatures = true;
	/*
	 * Controls whether to filter out role assignments which do not have jurisdictions
	 * when creating role assignment signatures.  Safest is to leave this false.
	 */
	public static boolean onlyUseRoleAssignmentsWithJurisdictions = false;

	public static void main(String[] args)
	{
		System.out.println("alter database postgres set task_perf_config.use_uniform_role_signatures to '" + (useUniformRoleSignatures ? "Y" : "N") + "';");
	}
}
