package global;

import global.models.File;

import java.nio.charset.Charset;

/**
 * Created by Arsen on 04.09.2016.
 */
public interface Const {

    boolean IS_DEBUG = false;
    boolean SHOULD_LOG_TO_NOTIFICATION = false;

    String EXPORT_FILE_NAME = "Templates.json";
    String ACTION_PREFIX = "pt.action.";

    String MODELS_PACKAGE_PATH = File.class.getCanonicalName().substring(
            0, File.class.getCanonicalName().length() - File.class.getSimpleName().length());

    String NODE_GROUP_DEFAULT = "default";

    String PACKAGE_TEMPLATES_DIR_NAME = "packageTemplates";
    String PACKAGE_TEMPLATES_EXTENSION = "json";
    String FILE_EXTENSION_SEPARATOR = ".";
    String ENCODED_NAME_EXT_DELIMITER = "\u0F0Fext\u0F0F.";

    int MESSAGE_MAX_LENGTH = 50;
    int FAVOURITE_NAME_LIMIT = 70;

    String DIR_USER = "";
    String DIR_INTERNAL = "internal";
    String DIR_J2EE = "j2ee";

    String TUTORIALS_URL = "http://ceh9.github.io/PackageTemplates/";


    interface charsets {
        Charset UTF_8 = Charset.forName("UTF-8");
        Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    }

    interface Key {
        String CTX_FULL_PATH = "CTX_FULL_PATH";
        String CTX_DIR_PATH = "CTX_DIR_PATH";
    }

}
