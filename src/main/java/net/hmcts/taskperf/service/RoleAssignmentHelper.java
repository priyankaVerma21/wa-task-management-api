package net.hmcts.taskperf.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.hmcts.taskperf.config.TaskPerfConfig;
import net.hmcts.taskperf.model.ClientFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;

public class RoleAssignmentHelper
{
	/**
	 * Removes role assignments which cannot match the query filter.  For example, if the query filter
	 * has jurisdiction: [IA, SSCS], then a role assignment with jurisdiction = CIVIL cannot result
	 * in any tasks being added to the query result set, whereas a role assignment with jurisdiction =
	 * IA, SSCS or null can.
	 */
	public static Set<RoleAssignment> filterRoleAssignments(Collection<RoleAssignment> roleAssignments, ClientFilter clientFilter)
	{
		return
				filterByAttribute(
					filterByAttribute(
						filterByAttribute(
							filterByAttribute(
									roleAssignments.stream(),
									RoleAttributeDefinition.JURISDICTION,
									clientFilter.getJurisdictions()),
							RoleAttributeDefinition.REGION,
							clientFilter.getRegions()),
						RoleAttributeDefinition.BASE_LOCATION,
						clientFilter.getLocations()),
					RoleAttributeDefinition.CASE_ID,
					clientFilter.getCaseIds())
				.filter(RoleAssignmentHelper::isTaskAccessGrantType)
				.filter(RoleAssignmentHelper::hasJurisdictionAttributeIfRequired)
				.collect(Collectors.toSet());
	}

	/**
	 * Return just the role assignments which are of grant type EXCLUDED.
	 */
	public static Set<RoleAssignment> exclusionRoleAssignments(Collection<RoleAssignment> roleAssignments, ClientFilter clientFilter)
	{
		return
				// No need to include exclusions that are for a jurisdiction that
				// isn't being queried.
				filterByAttribute(
						roleAssignments.stream(),
						RoleAttributeDefinition.JURISDICTION,
						clientFilter.getJurisdictions())
				.filter(ra -> ra.getGrantType() == GrantType.EXCLUDED)
				.filter(RoleAssignmentHelper::hasJurisdictionAttributeIfRequired)
				.collect(Collectors.toSet());
	}


	/**
	 * Returns true only for role assignments of the grant types which can give
	 * access to tasks: STANDARD, SPECIFIC and CHALLENGED.
	 */
	private static boolean isTaskAccessGrantType(RoleAssignment roleAssignment)
	{
		GrantType grantType = roleAssignment.getGrantType();
		return
				grantType == GrantType.STANDARD ||
				grantType == GrantType.SPECIFIC ||
				grantType == GrantType.CHALLENGED;
	}

	/**
	 * Returns true if the role assignment has a non-null jurisdiction.
	 */
	public static boolean hasJurisdictionAttributeIfRequired(RoleAssignment roleAssignment)
	{
		if (TaskPerfConfig.onlyUseRoleAssignmentsWithJurisdictions)
		{
			String jurisdiction = roleAssignment.getAttributes().get(RoleAttributeDefinition.JURISDICTION.value());
			return jurisdiction != null && jurisdiction.trim().length() != 0;
		}
		else
		{
			return true;
		}
	}

	/**
	 * Filters out role assignments which cannot match the constraints on the given attribute.  If the set of values
	 * is empty, then the attribute is unconstrained and nothing is removed.  If there are values provided, then
	 * the role assignment value must be one of the set, or null, otherwise the role assignment cannot match any
	 * tasks which also match the value set, and it is removed from consideration.
	 */
	private static Stream<RoleAssignment> filterByAttribute(Stream<RoleAssignment> roleAssignments, RoleAttributeDefinition attribute, Set<String> values)
	{
		return
				values == null || values.isEmpty() ?
				roleAssignments :
				roleAssignments.filter(ra -> roleAssignmentMatches(ra, attribute, values));
	}

	/**
	 * Returns true if the specified attribute is null (unconstrained) or is in the given set.
	 */
	private static boolean roleAssignmentMatches(RoleAssignment roleAssignment, RoleAttributeDefinition attribute, Set<String> values)
	{
		String attributeValue = roleAssignment.getAttributes().get(attribute.value());
		return attributeValue == null || values.contains(attributeValue);
	}

	/**
	 * Create a list of all the signatures representing the given role assignments with
	 * the given set of permissions. Multiple signatures are generated per role assignment.
	 */
	public static Set<String> buildRoleSignatures(Set<RoleAssignment> roleAssignments, Set<String> permissions)
	{
		Set<String> roleSignatures = new HashSet<>();
		for (RoleAssignment roleAssignment : roleAssignments)
		{
			for (String permission : permissions)
			{
				addRoleSignatures(roleAssignment, permission, roleSignatures);
			}
		}
		return roleSignatures;
	}

	/**
	 * Extend the collection with a "*" wildcard value.
	 */
	private static Collection<String> withWildcard(Collection<String> strings)
	{
		strings = (strings == null) ? new HashSet<>() : new HashSet<>(strings);
		strings.add("*");
		return strings;
	}

	/**
	 * If the value is null, replace it with a wildcard (*).
	 */
	private static String wildcardIfNull(String value)
	{
		return value == null ? "*" : value;
	}

	private static String[] UNKNOWN = new String[] {};
	private static String[] PUBLIC = new String[] {"U"};
	private static String[] PRIVATE = new String[] {"U", "P"};
	private static String[] RESTRICTED = new String[] {"U", "P", "R"};

	/**
	 * Return abbreviations for all the classification equal to or lower
	 * than the argument.  Abbreviations match the single-letter abbreviations
	 * used in the database to index tasks.
	 */
	private static String[] lowerClassifications(Classification classification)
	{
		switch (classification)
		{
		case PUBLIC:
			return PUBLIC;
		case PRIVATE:
			return PRIVATE;
		case RESTRICTED:
			return RESTRICTED;
		default:
			return UNKNOWN;
		}
	}

	/**
	 * Return the abbreviation for the given classification.
	 */
	private static String abbreviateClassification(Classification classification)
	{
		switch (classification)
		{
		case PUBLIC:
			return "U";
		case PRIVATE:
			return "P";
		case RESTRICTED:
			return "R";
		default:
			return null;
		}
	}

	/**
	 * Add all the role signatures to the list which can be created by the Cartesian product of:
	 *     - a pair of each singular attribute value and a wildcard (*)
	 *     - all the classifications <= the role assignment classification
	 *     - all the authorisations on the role assignment, plus a wildcard (*)
	 */
	private static void addRoleSignatures(RoleAssignment roleAssignment, String permission, Set<String> roleSignatures)
	{
		if (TaskPerfConfig.expandRoleAssignmentClassifications)
		{
			for (String classification : lowerClassifications(roleAssignment.getClassification()))
			{
				for (String authorisation : authorisationsForRoleAssignmentAndPermission(roleAssignment, permission))
				{
					String roleSignature = makeRoleSignature(roleAssignment, classification, authorisation, permission);
					if (roleSignature != null) roleSignatures.add(roleSignature);
				}
			}
		}
		else
		{
			for (String authorisation : authorisationsForRoleAssignmentAndPermission(roleAssignment, permission))
			{
				String classificationAbbreviation = abbreviateClassification(roleAssignment.getClassification());
				String roleSignature = makeRoleSignature(roleAssignment, classificationAbbreviation, authorisation, permission);
				if (roleSignature != null) roleSignatures.add(roleSignature);
			}
		}
	}

	/**
	 * Returns the list of authorisations to be included in signatures for the given role assignment.
	 * If the query is looking for available tasks, then signatures for organisational roles include
	 * all the user's authorisations, plus a wildcard ("*").
	 * 
	 * For other types of query, and for case roles in all queries, authorisations are ignored, and
	 * 
	 */
	private static Collection<String> authorisationsForRoleAssignmentAndPermission(RoleAssignment roleAssignment, String permission)
	{
		boolean isOrganisationalRole = treatAsOrganisationalRole(roleAssignment);
		boolean isAvailableTasks = "a".equals(permission);
		Collection<String> authorisations;
		if (isAvailableTasks && isOrganisationalRole)
		{
			authorisations = withWildcard(roleAssignment.getAuthorisations());
		}
		else
		{
			authorisations = List.of("*");
		}
		return authorisations;
		
	}

	/**
	 * Create the signature of the given role assignment, combined with the classification,
	 * authorisation and permission.  This matches the signatures used in the database to
	 * index tasks based on task role / permission configuration.
	 */
	private static String makeRoleSignature(RoleAssignment roleAssignment, String classification, String authorisation, String permission)
	{
		if (TaskPerfConfig.useUniformRoleSignatures)
		{
			return makeUniformRoleSignature(roleAssignment, classification, authorisation, permission);
		}
		else
		{
			if (treatAsOrganisationalRole(roleAssignment))
			{
				return makeOrganisationalRoleSignature(roleAssignment, classification, authorisation, permission);
			}
			else if (treatAsCaseRole(roleAssignment))
			{
				return makeCaseRoleSignature(roleAssignment, classification, authorisation, permission);
			}
			else
			{
				System.err.println("IGNORING UNSUPPORTED ROLE ASSIGNMENT " + roleAssignment.getRoleName());
				return null;
			}
		}
	}

	/**
	 * Determine whether this role assignment should be treated as an organisational role.
	 */
	private static boolean treatAsOrganisationalRole(RoleAssignment roleAssignment)
	{
		return roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()) == null;
	}

	/**
	 * Determine whether this role assignment should be treated as a case role.
	 */
	private static boolean treatAsCaseRole(RoleAssignment roleAssignment)
	{
		return roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()) != null;
	}

	/**
	 * Create the signature of the given case role assignment, combined with the
	 * classification, authorisation and permission.  This matches the signatures used in
	 * the database to index tasks based on task role / permission configuration.
	 */
	private static String makeCaseRoleSignature(RoleAssignment roleAssignment, String classification, String authorisation, String permission)
	{
		return
				"c:" +
				roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()) + ":" +
				roleAssignment.getRoleName() + ":" +
				permission + ":" +
				classification + ":" +
				authorisation;
	}

	/**
	 * Create the signature of the given organisational role assignment, combined with the
	 * classification, authorisation and permission.  This matches the signatures used in
	 * the database to index tasks based on task role / permission configuration.
	 */
	private static String makeOrganisationalRoleSignature(RoleAssignment roleAssignment, String classification, String authorisation, String permission)
	{
		return
				"o:" +
				roleAssignment.getAttributes().get(RoleAttributeDefinition.JURISDICTION.value()) + ":" +
				wildcardIfNull(roleAssignment.getAttributes().get(RoleAttributeDefinition.REGION.value())) + ":" +
				wildcardIfNull(roleAssignment.getAttributes().get(RoleAttributeDefinition.BASE_LOCATION.value())) + ":" +
				roleAssignment.getRoleName() + ":" +
				permission + ":" +
				classification + ":" +
				authorisation;
	}

	/**
	 * Create the signature of the given role assignment, combined with the
	 * classification, authorisation and permission.  This matches the signatures used in
	 * the database to index tasks based on task role / permission configuration.
	 * This is a uniform procedure that can be used for both case roles and organisational
	 * roles.
	 */
	private static String makeUniformRoleSignature(RoleAssignment roleAssignment, String classification, String authorisation, String permission)
	{
		return
				wildcardIfNull(roleAssignment.getAttributes().get(RoleAttributeDefinition.JURISDICTION.value())) + ":" +
				wildcardIfNull(roleAssignment.getAttributes().get(RoleAttributeDefinition.REGION.value())) + ":" +
				wildcardIfNull(roleAssignment.getAttributes().get(RoleAttributeDefinition.BASE_LOCATION.value())) + ":" +
				roleAssignment.getRoleName() + ":" +
				wildcardIfNull(roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value())) + ":" +
				permission + ":" +
				classification + ":" +
				authorisation;
	}
}