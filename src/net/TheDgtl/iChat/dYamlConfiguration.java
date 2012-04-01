package net.TheDgtl.iChat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

public class dYamlConfiguration extends YamlConfiguration {
    private final DumperOptions yamlOptions = new DumperOptions();
    private final Representer yamlRepresenter = new YamlRepresenter();
    private final Yaml yaml = new Yaml(new YamlConstructor(), yamlRepresenter, yamlOptions);
    
	@Override
	public void load(InputStream stream) throws IOException, InvalidConfigurationException {
        if (stream == null) {
            throw new IllegalArgumentException("Stream cannot be null");
        }
        
        Map<?, ?> input;
        try {
            input = (Map<?, ?>) yaml.load(stream);
        } catch (YAMLException e) {
            throw new InvalidConfigurationException(e);
        } catch (ClassCastException e) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }
        
        String header = parseHeader(stream);
        if (header.length() > 0) {
            options().header(header);
        }
        
        if (input != null) {
            convertMapsToSections(input, this);
        }
    }
	
	protected String parseHeader(InputStream stream) throws IOException {
		// Load from stream
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader input = new BufferedReader(reader);
		
        StringBuilder result = new StringBuilder();
        boolean readingHeader = true;
        boolean foundHeader = false;
        try {
        	String line;
	        while ((line = input.readLine()) != null && (readingHeader)) {
	            if (line.startsWith(COMMENT_PREFIX)) {
	                if (result.length() > 0) {
	                    result.append("\n");
	                }
	
	                if (line.length() > COMMENT_PREFIX.length()) {
	                    result.append(line.substring(COMMENT_PREFIX.length()));
	                }
	
	                foundHeader = true;
	            } else if ((foundHeader) && (line.length() == 0)) {
	                result.append("\n");
	            } else if (foundHeader) {
	                readingHeader = false;
	            }
	        }
        } finally {
        	input.close();
        }

        return result.toString();
    }
	
	public static dYamlConfiguration loadConfiguration(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        dYamlConfiguration config = new dYamlConfiguration();

        try {
            config.load(file);
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
        } catch (InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file , ex);
        }

        return config;
    }

    /**
     * Creates a new {@link YamlConfiguration}, loading from the given stream.
     * <p>
     * Any errors loading the Configuration will be logged and then ignored.
     * If the specified input is not a valid config, a blank config will be returned.
     *
     * @param stream Input stream
     * @return Resulting configuration
     * @throws IllegalArgumentException Thrown is stream is null
     */
    public static dYamlConfiguration loadConfiguration(InputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("Stream cannot be null");
        }

        dYamlConfiguration config = new dYamlConfiguration();

        try {
            config.load(stream);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load configuration from stream", ex);
        } catch (InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load configuration from stream", ex);
        }

        return config;
    }
}
