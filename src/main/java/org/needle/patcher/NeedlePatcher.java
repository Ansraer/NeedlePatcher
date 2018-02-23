package org.needle.patcher;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NeedlePatcher {

    public static String PATCHER_VERSION;
    public static String PATCHER_ARTIFACTID;

    public static String gameVersion;

    public static String needleVersion;
    public static String[] needleSupportedPatcherVersions;
    public static String[] needleSupportedGameVersions;


    static Logger logger = LogManager.getLogger(NeedlePatcher.class);

    public static void main(String[] args){
        System.out.println(logger.isInfoEnabled());
        logger.entry();
        logger.info("Starting needle patcher...");


        //Loading Maven infromation
        final Properties properties = new Properties();
        try {
            properties.load(NeedlePatcher.class.getClassLoader().getResourceAsStream("version.prop"));
            PATCHER_VERSION = properties.getProperty("version");
            PATCHER_ARTIFACTID = properties.getProperty("artifactId");
            logger.info("Loaded Maven information: "+ PATCHER_ARTIFACTID + " version: "+ PATCHER_VERSION +"\n");
        } catch (IOException e) {
            logger.error("failed to load Maven information", e);
            logger.error("Attempting to skip... \n");
        }


        String currentPath="";
        try {
            currentPath = NeedlePatcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        logger.info("Current path is: "+currentPath);


        String gameJarPath = pickJar("Game Jar:");
        String needleJarPath = pickJar("Needle Path:");


        logger.info("Reading Game Information");


        //loading information about the game
        Class<?> clazz = loadClassFromExternalJar(gameJarPath, "CardCrawlGame");
        try {
            Field f = clazz.getField("TRUE_VERSION_NUM");
            f.setAccessible(true);
            gameVersion= (String) f.get(null);
        } catch (Exception e) {
            logger.warn("Failed to get infromation from game class: ", e);
            e.printStackTrace();
            logger.warn("For this game version you will probably need a newer Needle Pather. You are currently using: "+PATCHER_VERSION);
            logger.exit();
            System.exit(0);
        }
        logger.info("Game version is: "+gameVersion);

        //loading information about needle
        clazz = loadClassFromExternalJar(needleJarPath, "NeedleCore");
        try {
            Field f = clazz.getField("TRUE_VERSION_NUM");
            f.setAccessible(true);
            gameVersion= (String) f.get(null);
        } catch (Exception e) {
            logger.warn("Failed to get infromation from game class: ", e);
            e.printStackTrace();
            logger.warn("For this game version you will probably need a newer Needle Pather. You are currently using: "+PATCHER_VERSION);
            logger.exit();
            System.exit(0);
        }
        logger.info("Game version is: "+gameVersion);





        /**


        // Load the class representation
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath( "/Path/from/root/myjarfile.jar" );
        CtClass cc = pool.get("org.mine.Myclass"); ////////// Not reading Myclass from myjarfile.jar


        // Find the method we want to patch and rename it
        // (we will be creating a new method with the original name).
        CtMethod m_old = cc.getDeclaredMethod("methodToRename");
        // m_old.setName( "methodToRename" );

        cc.removeMethod( m_old );

**/
    }

    /** Used to get game jar and mod jar if they are not specified by the arguments.
     *  @param title The title of the file choosed dialog.
     */
    public static String pickJar(String title){
        FileFilter filter = new FileNameExtensionFilter("JAR Files","jar", "zip");


        // JFileChooser-Objekt erstellen
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        chooser.setVisible(true);
        chooser.setName(title);
        chooser.setFileFilter(filter);

        // Dialog zum Oeffnen von Dateien anzeigen
        int result = chooser.showOpenDialog(null);


        // Only if file was accepted
        if(result == JFileChooser.APPROVE_OPTION)
        {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    public static Class loadClassFromExternalJar(String jarPath, String className){

        try {
            JarFile jarFile;
            jarFile = new JarFile(jarPath);
            Enumeration<JarEntry> e = jarFile.entries();

            URL[] urls = { new URL("jar:file:" + jarPath+"!/") };
            URLClassLoader cl = URLClassLoader.newInstance(urls);

            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                if(je.isDirectory() || !je.getName().endsWith(".class") || !je.getName().toLowerCase().contains((className+".class").toLowerCase())){
                    continue;
                }
                // -6 because of .class
                String classFullName = je.getName().substring(0,je.getName().length()-6);
                classFullName = classFullName.replace('/', '.');
                logger.info("Found external class "+classFullName);
                Class c = cl.loadClass(classFullName);


                return c;

            }
        } catch (IOException | ClassNotFoundException e) {
            logger.warn("Failed to load class from external jar file: ", e);
            e.printStackTrace();
            logger.exit();
            System.exit(0);
        }
        return null;
    }
}
