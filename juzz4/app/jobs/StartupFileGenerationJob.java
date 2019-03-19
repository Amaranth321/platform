package jobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lib.util.JsonReader;
import models.MongoDeviceModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import platform.analytics.VcaType;
import platform.devices.NodeEnv;
import platform.events.EventType;
import play.Logger;
import play.Play;
import play.i18n.Messages;
import play.jobs.Job;

import java.io.*;
import java.util.*;

public class StartupFileGenerationJob extends Job
{
    @Override
    public void doJob()
    {
        try
        {
            Logger.info("Verifying keys in language files.");
            setMissingKeysInLanguageFiles();

            Logger.info("Generating js type files");
            generateJsTypeFiles();

            if (Play.mode.isDev())
            {
                Logger.info("Verifying config.json files");
                checkConfigFiles();
            }
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
    }

    /**
     * Reads a Java properties file, but returns the key-value pairs in the
     * order they appear in the properties file.
     * <p/>
     * This is quite a bit of a hack:
     * (1) Knowing that Properties is a subclass of Hashtable (really poor
     * design) and that JDK's implementation of Properties.load calls the
     * Hashtable.put method, we create a subclass of Properties with the put
     * method to capture when key-value pairs are added.
     * (2) We don't want to deal with the parsing of the properties file, so we
     * simply call Properties.load and let it do all the dirty work. Try
     * looking at the JDK source codes to figure out how complicated the
     * parsing logic is.
     * (3) I am too lazy to define my own class for storing a key-value pair, so
     * I just took any available implementation. In this case, from Apache
     * Commons.
     *
     * @param fileName Properties file.
     *
     * @author Tan Yee Fan
     */
    private List<Map.Entry<String, String>> readPropertiesFile(String fileName)
    {
        Reader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
            final List<Map.Entry<String, String>> list = new ArrayList<>();
            Properties properties = new Properties()
            {
                @Override
                public Object put(Object key, Object value)
                {
                    if (key instanceof String && value instanceof String)
                    {
                        Map.Entry<String, String> entry = new ImmutablePair<>((String) key, (String) value);
                        list.add(entry);
                    }
                    return super.put(key, value);
                }
            };
            properties.load(reader);
            return list;
        }
        catch (IOException e)
        {
            Logger.error(e, "Error reading properties file " + fileName + ".");
            return null;
        }
        finally
        {
            if (reader != null)
            {
                IOUtils.closeQuietly(reader);
            }
        }
    }

    /**
     * Add/remove missing/unused keys in other language files with reference to messages.en
     * messages.zh is a duplicate of messages.zh-cn.
     * Maintain only messages.zh-cn.
     */
    private void setMissingKeysInLanguageFiles()
    {
        String confDir = Play.applicationPath.getAbsolutePath() + "/conf";
        String enFile = confDir + "/messages.en";
        String zhFile = confDir + "/messages.zh";
        String zhCnFile = confDir + "/messages.zh-cn";
        String zhTwFile = confDir + "/messages.zh-tw";

        String enNodeFile = confDir + "/messages.en-node";
        String zhCnNodeFile = confDir + "/messages.zh-cn-node";
        String zhTwNodeFile = confDir + "/messages.zh-tw-node";

        List<Map.Entry<String, String>> enEntryList = readPropertiesFile(enFile);
        if (enEntryList == null)
        {
            Logger.error("Skipping setting of missing keys in language files.");
            return;
        }

        Properties zhCnStrings = Messages.all("zh-cn");
        Properties zhTwStrings = Messages.all("zh-tw");
        Properties enNodeStrings = Messages.all("en-node");
        Properties zhCnNodeStrings = Messages.all("zh-cn-node");
        Properties zhTwNodeStrings = Messages.all("zh-tw-node");

        PrintWriter zhWriter = null;
        PrintWriter zhCnWriter = null;
        PrintWriter zhTwWriter = null;
        PrintWriter enNodeWriter = null;
        PrintWriter zhCnNodeWriter = null;
        PrintWriter zhTwNodeWriter = null;

        try
        {
            zhWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(zhFile), "UTF-8")));
            zhCnWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(zhCnFile), "UTF-8")));
            zhTwWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(zhTwFile), "UTF-8")));
            enNodeWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(enNodeFile), "UTF-8")));
            zhCnNodeWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(zhCnNodeFile), "UTF-8")));
            zhTwNodeWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(zhTwNodeFile), "UTF-8")));

            for (Map.Entry<String, String> enEntry : enEntryList)
            {
                String key = enEntry.getKey();
                String enValue = enEntry.getValue();

                //zh-cn
                if (zhCnStrings.containsKey(key))
                {
                    zhWriter.println(key + "=" + zhCnStrings.getProperty(key));
                    zhCnWriter.println(key + "=" + zhCnStrings.getProperty(key));
                }
                else
                {
                    zhWriter.println(key + "=" + enValue);
                    zhCnWriter.println(key + "=" + enValue);
                    Logger.info("New key added to zh-cn: " + key);
                }

                //zh-tw
                if (zhTwStrings.containsKey(key))
                {
                    zhTwWriter.println(key + "=" + zhTwStrings.getProperty(key));
                }
                else
                {
                    zhTwWriter.println(key + "=" + enValue);
                    Logger.info("New key added to zh-tw: " + key);
                }

                //en-node
                if (enNodeStrings.containsKey(key))
                {
                    enNodeWriter.println(key + "=" + enNodeStrings.getProperty(key));
                }
                else
                {
                    enNodeWriter.println(key + "=" + enValue);
                    Logger.info("New key added to en-node: " + key);
                }

                //zh-cn-node
                if (zhCnNodeStrings.containsKey(key))
                {
                    zhCnNodeWriter.println(key + "=" + zhCnNodeStrings.getProperty(key));
                }
                else
                {
                    zhCnNodeWriter.println(key + "=" + enValue);
                    Logger.info("New key added to zh-cn-node: " + key);
                }

                //zh-tw-node
                if (zhTwNodeStrings.containsKey(key))
                {
                    zhTwNodeWriter.println(key + "=" + zhTwNodeStrings.getProperty(key));
                }
                else
                {
                    zhTwNodeWriter.println(key + "=" + enValue);
                    Logger.info("New key added to zh-tw-node: " + key);
                }
            }
        }
        catch (IOException e)
        {
            Logger.error(e, "Error writing language files.");
        }
        finally
        {
            if (zhWriter != null)
            {
                zhWriter.close();
            }
            if (zhCnWriter != null)
            {
                zhCnWriter.close();
            }
            if (zhTwWriter != null)
            {
                zhTwWriter.close();
            }
            if (enNodeWriter != null)
            {
                enNodeWriter.close();
            }
            if (zhCnNodeWriter != null)
            {
                zhCnNodeWriter.close();
            }
            if (zhTwNodeWriter != null)
            {
                zhTwNodeWriter.close();
            }
        }
    }

    private void checkConfigFiles()
    {
        File configFolder = new File(Play.applicationPath + "/app");
        final File primaryFile = new File(configFolder + "/default.config.json");

        File[] configFiles = configFolder.listFiles(new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                return !pathname.equals(primaryFile) && pathname.getAbsolutePath().contains("config.json");
            }
        });

        try
        {
            JsonReader primary = new JsonReader();
            JsonReader customized = new JsonReader();
            primary.load(primaryFile);

            Set<String> configKeySet = primary.getFullKeySet();
            for (File configFile : configFiles)
            {
                customized.load(configFile);
                for (String key : configKeySet)
                {
                    if (!customized.containsKey(key))
                    {
                        Logger.warn("[%s] missing %s", configFile.getName(), key);
                    }
                }
            }

        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void generateJsTypeFiles()
    {
        /**
         *
         * This will generate backend internal types into a js file
         * so that the frontend can refer/lookup the type information at runtime.
         *
         * Generated file is guaranteed to have the latest types.
         *
         * Notes:
         * - for each class, there should be two maps : xxxType and xxxTypeInfo
         * - for ENUMs, use toString() or name() explicitly for consistency and readability
         * - if you use your own conventions, document them properly
         */

        String namespace = "backend";
        String entryFormat = namespace + ".%s = %s;";
        String headerInfo = "/**\n" +
                            " *\n" +
                            " * This file is generated at server startup. DO NOT edit or copy it out to another location.\n" +
                            " * Instead, create another js file with helper classes that read this file\n" +
                            " *\n" +
                            " */";
        List<String> outputList = new ArrayList<>();
        outputList.add(headerInfo);
        outputList.add(String.format("var %s = {};", namespace));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        /**
         * VCA types
         */
        Map<String, String> vcaTypeMap = new LinkedHashMap<>();
        Map<String, Map<String, Object>> vcaTypeInfoMap = new LinkedHashMap<>();
        for (VcaType vcaType : VcaType.values())
        {
            String backendName = vcaType.name();
            vcaTypeMap.put(backendName, backendName);

            Map<String, Object> typeInfoMap = new LinkedHashMap<>();
            typeInfoMap.put("displayName", vcaType.getConfigFeature().toString());
            typeInfoMap.put("typeName", vcaType.getVcaTypeName());
            typeInfoMap.put("configFeature", vcaType.getConfigFeature().toString());
            typeInfoMap.put("reportFeature", vcaType.getReportFeature().toString());
            typeInfoMap.put("eventType", vcaType.getEventType().toString());
            vcaTypeInfoMap.put(backendName, typeInfoMap);
        }
        outputList.add(String.format(entryFormat, "VcaType", gson.toJson(vcaTypeMap)));
        outputList.add(String.format(entryFormat, "VcaTypeInfo", gson.toJson(vcaTypeInfoMap)));

        /**
         * Event Types
         */
        Map<String, String> eventTypeMap = new LinkedHashMap<>();
        Map<String, Map<String, Object>> eventTypeInfoMap = new LinkedHashMap<>();
        for (EventType eventType : EventType.values())
        {
            String backendName = eventType.name();
            eventTypeMap.put(backendName, backendName);

            Map<String, Object> typeInfoMap = new LinkedHashMap<>();
            typeInfoMap.put("displayName", eventType.toString());
            typeInfoMap.put("typeName", eventType.toString());
            typeInfoMap.put("isBIVcaEvent", eventType.isBIVcaEvent());
            typeInfoMap.put("isSecurityVcaEvent", eventType.isSecurityVcaEvent());
            typeInfoMap.put("origin", eventType.getOrigin());
            eventTypeInfoMap.put(backendName, typeInfoMap);
        }
        outputList.add(String.format(entryFormat, "EventType", gson.toJson(eventTypeMap)));
        outputList.add(String.format(entryFormat, "EventTypeInfo", gson.toJson(eventTypeInfoMap)));

        /**
         * Node Environment
         */
        Map<String, String> nodeEnvMap = new LinkedHashMap<>();
        Map<String, Map<String, Object>> nodeEnvInfoMap = new LinkedHashMap<>();
        for (NodeEnv nodeEnv : NodeEnv.values())
        {
            String backendName = nodeEnv.name();
            nodeEnvMap.put(backendName, backendName);

            Map<String, Object> typeInfoMap = new LinkedHashMap<>();
            List<MongoDeviceModel> nodeModels = MongoDeviceModel.getNodeModels(nodeEnv);
            List<String> modelIdList = new ArrayList<>();
            for (MongoDeviceModel nodeModel : nodeModels)
            {
                modelIdList.add(nodeModel.getModelId());
            }
            typeInfoMap.put("modelIdList", modelIdList);
            nodeEnvInfoMap.put(backendName, typeInfoMap);
        }
        outputList.add(String.format(entryFormat, "NodeEnv", gson.toJson(nodeEnvMap)));
        outputList.add(String.format(entryFormat, "NodeEnvInfo", gson.toJson(nodeEnvInfoMap)));


        /**
         * End of types!
         *
         * File Creation.
         *
         */
        try
        {
            //write to public so that js file is visible
            File rootDir = new File(Play.applicationPath + "/public/javascripts/_generated/");
            if (!rootDir.exists())
            {
                rootDir.mkdirs();
            }

            File kupTypesJs = new File(rootDir + "/backend.types.js");
            FileUtils.writeStringToFile(kupTypesJs, StringUtils.join(outputList, "\n\n"));
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }
}