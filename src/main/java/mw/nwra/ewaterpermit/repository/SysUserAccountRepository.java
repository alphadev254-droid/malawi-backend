package mw.nwra.ewaterpermit.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mw.nwra.ewaterpermit.model.SysUserAccount;

@Repository
public interface SysUserAccountRepository extends JpaRepository<SysUserAccount, String> {
	SysUserAccount findByUsername(String username);

	@Query("SELECT u FROM SysUserAccount u " +
		   "LEFT JOIN FETCH u.sysAccountStatus " +
		   "LEFT JOIN FETCH u.sysSalutation " +
		   "LEFT JOIN FETCH u.sysUserGroup " +
		   "LEFT JOIN FETCH u.coreDistrict " +
		   "WHERE u.username = :username")
	SysUserAccount findByUsernameWithAssociations(@Param("username") String username);

	SysUserAccount findByEmailAddress(String emailAddress);

	SysUserAccount findByPhoneNumber(String phoneNumber);

	@Query("SELECT d FROM SysUserAccount d WHERE " + " d.firstName LIKE %:search% OR "
			+ " d.lastName LIKE %:search% OR " + " d.emailAddress LIKE %:search% OR "
			+ " d.sysUserGroup.name LIKE %:search%")
	Page<SysUserAccount> findAll(@Param("search") String search, Pageable pageable);
	
	Long countBySysAccountStatus_Name(String statusName);

	/**
	 * Optimized query to find officers by role with all associations loaded in one query
	 * This prevents N+1 query problems
	 */
	@Query("""
		SELECT u FROM SysUserAccount u 
		LEFT JOIN FETCH u.sysAccountStatus 
		LEFT JOIN FETCH u.sysSalutation 
		LEFT JOIN FETCH u.sysUserGroup 
		LEFT JOIN FETCH u.coreDistrict 
		WHERE u.sysUserGroup.name = :roleOrGroupName 
		AND u.emailAddress IS NOT NULL 
		AND u.emailAddress != ''
		ORDER BY u.firstName, u.lastName
	""")
	List<SysUserAccount> findOfficersByRoleOptimized(@Param("roleOrGroupName") String roleOrGroupName);

	/**
	 * Optimized query to get all users with associations loaded - for when all users are actually needed
	 */
	@Query("""
		SELECT u FROM SysUserAccount u
		LEFT JOIN FETCH u.sysAccountStatus
		LEFT JOIN FETCH u.sysSalutation
		LEFT JOIN FETCH u.sysUserGroup
		LEFT JOIN FETCH u.coreDistrict
		ORDER BY u.firstName, u.lastName
	""")
	List<SysUserAccount> findAllWithAssociations();

	/**
	 * Get all unique user group names for debugging purposes
	 */
	@Query("SELECT DISTINCT u.sysUserGroup.name FROM SysUserAccount u WHERE u.sysUserGroup IS NOT NULL ORDER BY u.sysUserGroup.name")
	List<String> findAllUserGroupNames();
}
