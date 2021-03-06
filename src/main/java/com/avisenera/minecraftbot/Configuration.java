package com.avisenera.minecraftbot;

import java.io.*;
import java.util.ArrayList;
import java.util.EnumMap;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * This class manages the configuration values.
 * When it starts, it reads the configuration file and holds on
 * to all its contents.<br>
 * Other classes should always get the necessary values from this class
 * because the values will change when the configuration is reloaded.
 */
public class Configuration {
    private MinecraftBot plugin;
    
    private boolean valid = false;
    
    private EnumMap<Keys.connection, String> connection;
    private EnumMap<Keys.commands, String> commands;
    private EnumMap<Keys.settings, String> settings;
    private EnumMap<Keys.line_to_irc, String> line_to_irc;
    private EnumMap<Keys.line_to_minecraft, String> line_to_minecraft;
    private ArrayList<String> ignore_list;

    /**
     * When instantiating this class, the configuration is not loaded.<br>
     * You must load the configuration by using {@link load()}.
     */
    public Configuration(MinecraftBot instance) {
        plugin = instance;
    }
    
    /**
     * (Re)loads values from the configuration file. If an error occurs,
     * the current configuration values are not changed.
     * @return False if an error occured.
     */
    public boolean load() {
        FileConfiguration config = getConfigFile(plugin);
        if (config == null) return false;
        
        EnumMap<Keys.connection, String> new_c = new EnumMap<Keys.connection, String>(Keys.connection.class);
        EnumMap<Keys.commands, String> new_co = new EnumMap<Keys.commands, String>(Keys.commands.class);
        EnumMap<Keys.settings, String> new_s = new EnumMap<Keys.settings, String>(Keys.settings.class);
        EnumMap<Keys.line_to_irc, String> new_lti = new EnumMap<Keys.line_to_irc, String>(Keys.line_to_irc.class);
        EnumMap<Keys.line_to_minecraft, String> new_ltm = new EnumMap<Keys.line_to_minecraft, String>(Keys.line_to_minecraft.class);
        ArrayList<String> new_ignores = new ArrayList<String>();
        
        for (Keys.connection c : Keys.connection.values())
            new_c.put(c, config.getString("connection."+c, ""));
        for (Keys.commands c : Keys.commands.values())
            new_co.put(c, config.getString("commands."+c, ""));
        for (Keys.settings c : Keys.settings.values())
            new_s.put(c, config.getString("settings."+c, ""));
        for (Keys.line_to_irc c : Keys.line_to_irc.values())
            new_lti.put(c, config.getString("line_formatting.to_irc."+c, ""));
        for (Keys.line_to_minecraft c : Keys.line_to_minecraft.values())
            new_ltm.put(c, config.getString("line_formatting.to_minecraft."+c, ""));
        
        boolean accepted = true;
        
        // Getting ignore list
        BufferedReader ignores = getIgnoreList(plugin);
        String lineinput;
        try {
            while ((lineinput = ignores.readLine()) != null) {
                if (lineinput.startsWith("#")) continue;
                new_ignores.add(lineinput.toLowerCase());
            }
        } catch (IOException e1) {
            plugin.log(2, "An error occured while attempting to read the ignore list.");
            accepted = false;
        }
        
        // Checking for all required values #########################
        String scheck;
        // Server name
        scheck = new_c.get(Keys.connection.server);
        if (scheck == null || scheck.isEmpty()) {
            plugin.log(2, "The server to connect to is not defined.");
            accepted = false;
        }
        // Server port
        scheck = new_c.get(Keys.connection.server_port);
        try {
            int icheck = Integer.parseInt(scheck);
            if (icheck > 65535 || icheck < 1) {
                plugin.log(2, "An invalid port number was defined in the configuration file.");
                accepted = false;
            }
        } catch (NumberFormatException e) {
            plugin.log(2, "An invalid port number was defined in the configuration file.");
            accepted = false;
        }
        // Channel name
        scheck = new_c.get(Keys.connection.channel);
        if (scheck == null || scheck.isEmpty()) {
            plugin.log(2, "A channel was not defined in the configuration file.");
            accepted = false;
        }
        // Fix channel name
        if (!scheck.startsWith("#")) {
            scheck = "#" + scheck;
            new_c.put(Keys.connection.channel, scheck);
        }
        
        // Fixing numeric values to defaults ########################
        // bot message delay - default is 1000
        scheck = new_c.get(Keys.connection.bot_message_delay);
        try {
            int bmd = Integer.parseInt(scheck);
            if (bmd < 0) new_c.put(Keys.connection.bot_message_delay, "1000");
        } catch (NumberFormatException e) {
            new_c.put(Keys.connection.bot_message_delay, "1000");
        }
        // connection tries - default is 5
        scheck = new_c.get(Keys.connection.retries);
        try {
            int retries = Integer.parseInt(scheck);
            if (retries < 0) new_c.put(Keys.connection.retries, "5");
        } catch (NumberFormatException e) {
            new_c.put(Keys.connection.retries, "5");
        }
        
        if (accepted) {
            connection = new_c;
            commands = new_co;
            settings = new_s;
            line_to_irc = new_lti;
            line_to_minecraft = new_ltm;
            ignore_list = new_ignores;
            plugin.log(0, "Configuration has been loaded.");
            
            valid = true;
        }
        
        return accepted;
    }
    
    /**
     * Returns the Map containing all the configuration options under connection.
     */
    public EnumMap<Keys.connection, String> connection() {
        return connection.clone();
    }
    
    /**
     * Returns the given connection value in the configuration file.
     * @param value Equivalent to config.getString("connection.(value)")
     */
    public String connection(Keys.connection value) {
        if (!valid || value == null) return "";
        
        String rv = connection.get(value);
        if (rv == null) return "";
        else return rv;
    }
    
    /**
     * Returns the given settings value in the configuration file.
     * @param value Equivalent to config.getString("settings.(value)", "")
     */
    public String settingsS(Keys.settings value) {
        if (!valid || value == null) return "";
        
        String rv = settings.get(value);
        if (rv == null) return "";
        else return rv;
    }
    
    /**
     * Returns the given commands value as a boolean in the configuration file.
     * @param value Equivalent to config.getBoolean("commands.(value)", false)
     */
    public boolean commandsB(Keys.commands value) {
        String rv = commandsS(value);
        return (rv.equalsIgnoreCase("true"));
    }
    
    /**
     * Returns the given commands value in the configuration file.
     * @param value Equivalent to config.getString("commands.(value)", "")
     */
    public String commandsS(Keys.commands value) {
        if (!valid || value == null) return "";
        
        String rv = commands.get(value);
        if (rv == null) return "";
        else return rv;
    }
    
    /**
     * Returns the given settings value as a boolean in the configuration file.
     * @param value Equivalent to config.getBoolean("settings.(value)", false)
     */
    public boolean settingsB(Keys.settings value) {
        String rv = settingsS(value);
        return (rv.equalsIgnoreCase("true"));
    }
    
    /**
     * Returns the formatting string value that was entered in the configuration file.
     * @param value Equivalent to config.getString("line_formatting.to_irc.(value)")
     */
    public String line_to_irc(Keys.line_to_irc value) {
        if (!valid || value == null) return "";
        
        String rv = line_to_irc.get(value);
        if (rv == null) return "";
        else return rv;
    }
    
    /**
     * Returns the formatting string value that was entered in the configuration file.
     * @param value Equivalent to config.getString("line_formatting.to_irc.(value)")
     */
    public String line_to_minecraft(Keys.line_to_minecraft value) {
        if (!valid || value == null) return "";
        
        String rv = line_to_minecraft.get(value);
        if (rv == null) return "";
        else return rv;
    }
    
    /**
     * Returns an array list containing all the current ignore values.
     */
    public ArrayList<String> ignoreList() {
        return ignore_list;
    }
    
    /**
     * Gets the configuration file. If the file does not exist, it tries to
     * create it. This method sends log information in case an error occurs.
     * @return null if the file was just created or an error occured.
     */
    private FileConfiguration getConfigFile(MinecraftBot plugin) {
        // Checks if the config file exists. If not, creates it.
        // Returns false if an error occured.
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            plugin.getConfig().load(new File(plugin.getDataFolder(), "config.yml"));
            return plugin.getConfig();
        } catch (FileNotFoundException e) {
            plugin.log(0, "No config file found. Creating a default configuration file.");
            plugin.log(0, "You must edit this file before being able to use this plugin.");
            saveFile(plugin);
        } catch (IOException e) {
            plugin.log(2, "Error while loading config! Check if config.yml or the plugins folder is writable.");
        } catch (InvalidConfigurationException e) {
            plugin.log(2, "Configuration is invalid. Check your syntax. (Remove any tab characters.)");
        }
        return null;
    }
    
    private BufferedReader getIgnoreList(MinecraftBot plugin) {
        // Checks if the ignore list file exists, and if it doesn't, creates it.
        try {
            File il = new File(plugin.getDataFolder(), "ignorelist.txt");
            if (!il.exists()) {
                // Create the new ignore list
                il.createNewFile();
                BufferedWriter out = new BufferedWriter(new FileWriter(il));
                out.write("# Ignore list - Any nicks placed into this file will be ignored in IRC."); out.newLine();
                out.write("# This file is the equivalent of sending many /irc ignore commands at once."); out.newLine();
                out.write("# It is not automatically updated. When reloading the file, all ignored nicks " +
                		"will be replaced with the nicks in this file."); out.newLine();
                out.newLine();
                out.write("#One nick per line."); out.newLine();
                out.flush();
                out.close();
            }
            return new BufferedReader(new FileReader(il));
        } catch (IOException ex) {
            plugin.log(2, "Failed to create new ignore list file. Check if the plugins folder is writable.");
        }
        return null;
    }
    
    private void saveFile(MinecraftBot plugin) {
        try
        {
            File conf = new File(plugin.getDataFolder(), "config.yml");
            
            InputStream is = this.getClass().getResourceAsStream("/config.yml");
            
            if (!conf.exists())
                conf.createNewFile();
            OutputStream os = new FileOutputStream(conf);
            
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0)
                os.write(buf, 0, len);

            is.close();
            os.close();
        }
        catch (IOException e)
        {
            plugin.log(2, "Failed to save config.yml - Check the plugin's data directory!");
        }
        catch (NullPointerException e)
        {
            plugin.log(2, "Could not find the default config.yml! Is it in the .jar?");
        }
    }
}
