package cloud.zeroprox.gamespleef.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Properties;

import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import cloud.zeroprox.gamespleef.GameSpleef;

/**
 * Most likely, this is the final version of the class.. 
 * Okay, what is this? This is an assistant class that will help you understand your language packs. 
 * Those. Instead of using yaml/hocon files that are dependent on the read encoding of the file, 
 * you can safely write your locales in the .properties file without 
 * worrying about encoding the file at all.
 * http://java-properties-editor.com/ - program for editing ;) 
 *
 * Class rewritten for use in plugins written on Sponge API,
 * here, the methods from the above api are used. But it is not worth 
 * it to rewrite / delete a couple of points for other applications.
 *
 * @author Dereku
 * @update Mr_Krab
 */
public class Locale {

    private final HashMap<String, MessageFormat> messageCache = new HashMap<>();
    private final Properties locale = new Properties();
    private File localeFile;
    private String loc;

    GameSpleef instance;
    public Locale(GameSpleef plugin, String string) {
    	instance = plugin;
    	loc = string;
    }

    /**
     * Initializing the class. Must be called first.
     * Otherwise, you will receive Key "key" does not exists!
     */
    public void init() throws IOException {
        this.locale.clear();
        String loc = this.loc;
        this.localeFile = new File(instance.configFile, loc + ".properties");
        if (this.saveLocale(loc)) {
		    try (FileReader fr = new FileReader(this.localeFile)) {
		        this.locale.load(fr);
		    } catch (Exception ex) {
		    	instance.consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&4Failed to load " + loc + " locale!" + ex));
		    }
		} else {
		    try {
		        this.locale.load(getClass().getResourceAsStream("/assets/gamespleef/en_US.properties"));
		    } catch (IOException ex) {
		    	instance.consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&4Failed to load en_US locale!" + ex));
		    }
		}
    }

    /**
     * Receiving a message from the configuration
     * Example message: "There is so many players."
     * Example call: getString("key");
     *
     * @param key - message key
     * @return message, otherwise null
     */
    public String getString(final String key) {
        return this.getString(key, false, "");
    }

    /**
     * Receiving a message with arguments from the configuration
     * Example message: "There is {0} players: {1}."
     * Example call: getString("key", "2", "You, Me");
     *
     * @param key - message key
     * @param args - arguments of the message
     * @return message, otherwise null
     */
    public String getString(final String key, final String... args) {
        return this.getString(key, false, args);
    }

    /**
     * Получение сообщения из конфигурации с возможностью фильтрации цвета
     * Example message: "\u00a76There is so many players."
     * Example call: getString("key", false);
     *
     * @param key - message key
     * @param removeColors если true, то цвета будут убраны
     * @return message, otherwise null
     */
    public String getString(final String key, final boolean removeColors) {
        return this.getString(key, removeColors, "");
    }

    /**
     * Получение сообщения с аргументами из конфигурации с возможностью фильтрации цвета
     * Example message: "\u00a76There is \u00a7c{0} \u00a76players:\u00a7c {1}."
     * Example call: getString("key", false, "2", "You, Me");
     *
     * @param key - message key
     * @param removeColors если true, то цвета будут убраны
     * @param args - arguments of the message
     * @return message, otherwise null
     */
    public String getString(final String key, final boolean removeColors, final String... args) {
        String out = this.locale.getProperty(key);
        if (out == null) {
            return "§4Key \"" + key + "\" not found!";
        }

        MessageFormat mf = this.messageCache.get(out);
        if (mf == null) {
            mf = new MessageFormat(out);
            this.messageCache.put(out, mf);
        }
        
        out = mf.format(args);
        
        if (removeColors) {
            out = TextColors.NONE.toString();
        }
        
        return out;
    }

    /**
     * Saving the localization file. 
     * The choice of a specific localization is determined by
     * the parameter in the main configuration file.
     * If the selected localization does not exist, 
     * the default file will be saved(en_US.properties).
     * The path to the localization file can be changed.
     * @path - The path to the localization file in the *.jar file.
     * @name - The name of the localization file specified in the main configuration file.
     * @is - Localization file name + .properties
     */
    
	private boolean saveLocale(final String name) {
        if (this.localeFile.exists()) {
            return true;
        }
        String path = "/assets/gamespleef/";
        File enFile = new File(instance.configFile + File.separator + "en_US.properties");
        String is = path + name + ".properties";
        if (getClass().getResource(is) == null) {
        	instance.consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&4Failed to save \"" + name + ".properties\""));
            if (!enFile.exists()){
            	try {
            		URI u = getClass().getResource(path + "en_US.properties").toURI();
            		FileSystem jarFS = FileSystems.newFileSystem(URI.create(u.toString().split("!")[0]), new HashMap<>());
            		Files.copy(jarFS.getPath(u.toString().split("!")[1]), new File(instance.configFile + File.separator + "en_US.properties").toPath());
            		jarFS.close();
            	} catch (IOException ex) {
            		instance.consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&4Failed to save \"" + name + ".properties\"" + ex));
            	} catch (URISyntaxException e) {
            		instance.consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&4Locale \"{0}\" does not exists!\"" + name));
            	}
            	return false;
            	}
            return false;
        } else {    
        	try {
        		URI u = getClass().getResource(is).toURI();
        		FileSystem jarFS = FileSystems.newFileSystem(URI.create(u.toString().split("!")[0]), new HashMap<>());
        		Files.copy(jarFS.getPath(u.toString().split("!")[1]), new File(instance.configFile + File.separator + name + ".properties").toPath());
        		jarFS.close();
        	} catch (IOException ex) {
        		instance.consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&4Failed to save \"" + name + ".properties \"" + ex));
        	} catch (URISyntaxException e) {
        		instance.consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&4Locale \"{0}\" does not exists!\"" + name));
        	}	
        }
        return true;
    }
}