package mw.nwra.ewaterpermit.util;

//import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
//import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Paths;
//import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
//import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import mw.nwra.ewaterpermit.constant.Action;
import mw.nwra.ewaterpermit.exception.DataFormatException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.model.SysUserAccountActivation;
import mw.nwra.ewaterpermit.model.SysUserGroupPermission;
import mw.nwra.ewaterpermit.service.SysConfigService;
import mw.nwra.ewaterpermit.service.SysUserAccountService;
import mw.nwra.ewaterpermit.service.SysUserGroupPermissionService;

@Component
public class AppUtil {

	/* trick to inject service in static methods */
	private static JwtUtil jwtUtil;
	private static SysUserAccountService sysUserAccountService;

	@Autowired
	private JwtUtil jwtUtil0;
	@Autowired
	SysUserAccountService sysUserAccountService0;

	private static SysConfigService configService;
	@Autowired
	private SysConfigService configService0;
	private static String UPLOAD_PATH;
	private static String UPLOAD_URL;

	@Autowired
	SysUserGroupPermissionService userPermissionService0;
	private static SysUserGroupPermissionService userPermissionService;

	@PostConstruct
	private void initStaticDao() {
		jwtUtil = this.jwtUtil0;
		sysUserAccountService = this.sysUserAccountService0;
		userPermissionService = this.userPermissionService0;

		configService = this.configService0;
		if (configService != null) {
			List<mw.nwra.ewaterpermit.model.SysConfig> configs = configService.getAllSysConfigurations();
			if (configs != null && !configs.isEmpty()) {
				UPLOAD_PATH = configs.get(0).getUploadDirectory();
				UPLOAD_URL = configs.get(0).getStorageUrl();
			} else {
				// Set default values or log warning
				UPLOAD_PATH = System.getProperty("user.dir") + "/uploads";
				UPLOAD_URL = "/uploads";
				System.err.println("Warning: No system configurations found. Using default upload paths.");
			}
		}
	}

	/* trick to inject service in static methods */
	public static String[] getIgnoredProperties(Class<?> c, Map<String, Object> obj) {
		obj = removeNulls(obj);
		List<String> fieldsToIgnore = new ArrayList<String>();
		for (Field field : c.getDeclaredFields()) {
			if (!obj.containsKey(field.getName())) {
				fieldsToIgnore.add(field.getName());
			}
		}
		String[] strArray = new String[fieldsToIgnore.size()];
		strArray = fieldsToIgnore.toArray(strArray);
		return strArray;
	}

	private static Map<String, Object> removeNulls(Map<String, Object> obj) {
		obj = obj.entrySet().stream().filter(it -> it.getValue() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return obj;
	}

	private static ObjectMapper initMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());
//	mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		return mapper;
	}

	public static Object objectToClass(Class<?> c, Map<String, Object> obj) throws DataFormatException {
		obj = removeNulls(obj);
		Object newObj = null;
		ObjectMapper mapper = initMapper();
		try {
			newObj = mapper.convertValue(obj, c);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DataFormatException("Invalid parameters for the operation. Unrecognized field passed.");
		}
		return newObj;
	}

	public static Object objectToClass(Class<?> c, Object obj) throws DataFormatException {
		Object newObj = null;
		ObjectMapper mapper = initMapper();
		try {
			newObj = mapper.convertValue(obj, c);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DataFormatException("Invalid parameters for the operation. Unrecognized field passed.");
		}
		return newObj;
	}

	public static Object sanitize(Class<?> c, Object obj, String[] propertiesToIgnore) throws DataFormatException {
		Object objCopy;
		try {
			objCopy = Class.forName(c.getName()).getConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DataFormatException("Invalid parameters for the operation. Unable to construct response.");
		}
		BeanUtils.copyProperties(obj, objCopy, propertiesToIgnore);
		return objCopy;
	}

	public static SysUserAccount sanitizeSysUserAccount(SysUserAccount userAccount) {
		try {
			SysUserAccount sanitized = (SysUserAccount) sanitize(SysUserAccount.class, userAccount,
					new String[] { "password", "lastPasswordAttempt", "passwordAttemptCount" });
			// Ensure date_created is preserved
			if (sanitized.getDateCreated() == null && userAccount.getDateCreated() != null) {
				sanitized.setDateCreated(userAccount.getDateCreated());
			}
			return sanitized;
		} catch (DataFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return userAccount;
	}

	public static SysUserAccount getLoggedInUser(String token) {
		if (token == null || token.trim().isEmpty()) {
			return null;
		}
		
		// Handle both "Bearer token" and "token" formats
		String actualToken;
		if (token.startsWith("Bearer ")) {
			actualToken = token.substring(7); // Remove "Bearer " prefix
		} else {
			actualToken = token; // Token without Bearer prefix
		}
		
		String username = jwtUtil.extractUsername(actualToken);
		return sysUserAccountService.getSysUserAccountByUsernameWithAssociations(username);
	}

	public static SysUserAccountActivation createSysUserAccountActivation(SysUserAccount user) {
		return createSysUserAccountActivation(user, null);
	}

	public static SysUserAccountActivation createSysUserAccountActivation(SysUserAccount user, String registrationHost) {
		SysUserAccountActivation userActivation = new SysUserAccountActivation();
		userActivation.setSysUserAccount(user);
		// Generate secure random token (32 characters alphanumeric)
		String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
		token = token.substring(0, 32); // 32 character token for security
		userActivation.setToken(token);
		userActivation.setRegistrationHost(registrationHost);
		userActivation.setDateCreated(new Timestamp(new Date().getTime()));

		Date dt = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(dt);
		c.add(Calendar.DATE, 2); // account has to be activated within 48hrs
		dt = c.getTime();

		userActivation.setDateExpired(new Timestamp(dt.getTime()));
		userActivation.setDateActivated(null);
		return userActivation;
	}

	public static Pageable getPageRequest(int page, int limit, String sortCol, String sortOrder) {
		// Validate parameters to prevent IndexOutOfBoundsException
		if (page < 0) page = 0;
		if (limit <= 0) limit = 10; // Default page size
		if (sortCol == null || sortCol.trim().isEmpty()) sortCol = "id";
		if (sortOrder == null) sortOrder = "asc";

		Sort sort = (sortOrder.equalsIgnoreCase("desc")) ? Sort.by(sortCol).descending() : Sort.by(sortCol).ascending();
		return PageRequest.of(page, limit, sort);
	}

	public static String uploadFile(String prefix, String fileString) {
		prefix = prefix.replaceAll("\\s+", "_").toLowerCase();
		// token the data
		String fileExtension = null;
		if (fileString.contains(",")) {
			fileExtension = (fileString.split(";")[0]).split("/")[1];
			fileString = fileString.split(",")[1];
			// for word document file extension in not docx
			fileExtension = fileExtension.contains("wordprocessing") ? "docx"
					: fileExtension.contains("msword") ? "doc" : fileExtension;
		}
		if (!isBase64File(fileString)) {
			throw new ForbiddenException("Could not upload the file");
		}
		Decoder decoder = Base64.getDecoder();
		byte[] fileByte = decoder.decode(fileString);

		fileExtension = fileExtension == null ? guessFileTypeTika(fileByte) : fileExtension;
		if (fileExtension == null) {
			throw new ForbiddenException("Unrecognized file type");
		}
		String fileName = prefix + UUID.randomUUID().toString() + new Date().getTime() + "." + fileExtension;

		// remove underscore for directory name
		StringBuilder sb = new StringBuilder(prefix);
		String dirPrefix = sb.deleteCharAt(prefix.length() - 1).toString();
		File outputDirectory = new File(UPLOAD_PATH + "/" + dirPrefix);
		if (!outputDirectory.exists()) {
			outputDirectory.mkdir();
		}
		File outputfile = new File(outputDirectory.getAbsolutePath() + "/" + fileName);
		try {
			FileUtils.writeByteArrayToFile(outputfile, fileByte);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ForbiddenException("Could not upload the file");
		}
		return UPLOAD_URL + "/" + dirPrefix + "/" + fileName;
	}

	public static void deleteUploadedFile(String fileUrl) {
		try {
			URI url = Paths.get(fileUrl).toUri();
//			.replaceFirst("/", "")
			fileUrl = url.getPath().replace("/", "\\");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// getting from the second slash after path, which is the actual folder
		fileUrl = fileUrl.substring(fileUrl.substring(0, fileUrl.lastIndexOf("\\")).lastIndexOf("\\"));
		File outputfile = new File(UPLOAD_PATH + fileUrl);
//		outputfile.delete();
		File outputDirectory = new File(UPLOAD_PATH + "\\deleted");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdir();
		}
		outputfile.renameTo(new File(UPLOAD_PATH + "\\deleted" + fileUrl));
	}

	public static boolean isBase64File(String str) {
		if (str.contains(",")) {
			str = str.split(",")[1];
		}
		return org.apache.commons.codec.binary.Base64.isBase64(str.getBytes());
	}

	private static String guessFileTypeTika(byte[] base64Bytes) {
		Tika tika = new Tika();
		String fileType = tika.detect(base64Bytes);
		return MimeTypes.getFileExtension(fileType);
	}

	public static List<SysUserGroupPermission> getUserPermissions(SysUserAccount theUser) {
		List<SysUserGroupPermission> groupPerms = userPermissionService
				.getSysUserGroupPermissionBySysUserGroup(theUser.getSysUserGroup());
//		List<SysUserGroupPermission> userPerms = userPermissionService.getSysUserGroupPermissionBySysUserAccount(theUser);

		// Remove duplicates permissions
		Set<SysUserGroupPermission> set = new LinkedHashSet<>(groupPerms);
//		set.addAll(userPerms);

		// Convert Set to ArrayList
		ArrayList<SysUserGroupPermission> combinedPerms = new ArrayList<SysUserGroupPermission>(set);
		return combinedPerms;
	}

	public static void can(List<SysUserGroupPermission> perms, String object_name, String action) {
		perms = perms.stream().filter((pm) -> {
			return (pm.getSysObject().getName().equals(object_name) || pm.getSysObject().getName().equals("all"))
					&& (pm.getSysPermission().getName().equals(action)
							|| pm.getSysPermission().getName().equals(Action.ALL.toString()));
		}).collect(Collectors.toList());
		if (perms.isEmpty()) {
			throw new ForbiddenException("Action denied");
		}
	}
	
	public static boolean hasPermission(SysUserAccount user, String permissionName) {
		List<SysUserGroupPermission> userPermissions = getUserPermissions(user);
		return userPermissions.stream().anyMatch(perm -> 
			perm.getSysPermission().getName().equals(permissionName) ||
			perm.getSysPermission().getName().equals(Action.ALL.toString())
		);
	}
}