package org.apache.maven.test;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.surefire.SurefireBooter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Run tests using Surefire.
 *
 * @author Jason van Zyl
 * @version $Id$
 * @requiresDependencyResolution test
 * @goal test
 * @phase test
 * @todo make version of junit and surefire configurable
 * @todo make report to be produced configurable
 */
public class SurefirePlugin
    extends AbstractMojo
{
    /**
     * Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, but quite convenient on occasion.
     *
     * @parameter expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on occasion.
     *
     * @parameter expression="${maven.test.failure.ignore}"
     */
    private boolean testFailureIgnore;

    /**
     * The base directory of the project being tested. This can be obtained in your unit test by System.getProperty("basedir").
     *
     * @parameter expression="${basedir}"
     * @required
     */
    private File basedir;

    /**
     * The directory containing generated classes of the project being tested.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File classesDirectory;

    /**
     * The directory containing generated test classes of the project being tested.
     *
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     */
    private File testClassesDirectory;

    /**
     * The classpath elements of the project being tested.
     *
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List classpathElements;

    /**
     * Base directory where all reports are written to.
     *
     * @parameter expression="${project.build.directory}/surefire-reports"
     */
    private String reportsDirectory;

    /**
     * The test source directory containing test class sources.
     *
     * @parameter expression="${project.build.testSourceDirectory}"
     * @required
     */
    private File testSourceDirectory;

    /**
     * Specify this parameter if you want to use the test pattern matching notation, Ant pattern matching, to select tests to run.
     * The Ant pattern will be used to create an include pattern formatted like <code>**&#47;${test}.java</code>
     * When used, the <code>includes</code> and <code>excludes</code> patterns parameters are ignored
     *
     * @parameter expression="${test}"
     */
    private String test;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be included in testing.
     * When not specified and whent the <code>test</code> parameter is not specified, the default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>
     *
     * @parameter
     */
    private List includes;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be excluded in testing.
     * When not specified and whent the <code>test</code> parameter is not specified, the default excludes will be
     * <code>**&#47;Abstract*Test.java  **&#47;Abstract*TestCase.java **&#47;*$*</code>
     *
     * @parameter
     */
    private List excludes;

    /**
     * ArtifactRepository of the localRepository. To obtain the directory of localRepository in unit tests use System.setProperty( "localRepository").
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @parameter
     */
    private Map systemProperties;

    /**
     * List of of Plugin Artifacts.
     *
     * @parameter expression="${plugin.artifacts}"
     */
    private List pluginArtifacts;

    /**
     * Option to print summary of test suites or just print the test cases that has errors.
     *
     * @parameter expression="${surefire.printSummary}"
     * default-value="true"
     */
    private boolean printSummary;

    /**
     * Selects the formatting for the test report to be generated.  Can be set as brief, plain, or xml.
     *
     * @parameter expression="${surefire.reportFormat}"
     * default-value="brief"
     */
    private String reportFormat;

    /**
     * Option to generate a file test report or just output the test report to the console.
     *
     * @parameter expression="${surefire.useFile}"
     * default-value="true"
     */
    private boolean useFile;

    /**
     * Option to specify the forking mode. Can be "none", "once" or "pertest"
     *
     * @parameter expression="${forkMode}"
     * default-value="none"
     */
    private String forkMode;

    /**
     * Option to specify the jvm (or path to the java executable) to use with
     * the forking options. For the default we will assume that java is in the path.
     *
     * @parameter expression="${jvm}"
     * default-value="java"
     */
    private String jvm;

    /**
     * Arbitrary options to set on the command line.
     *
     * @parameter expression="${argLine}"
     */
    private String argLine;

    /**
     * Additional environments to set on the command line.
     *
     * @parameter
     */
    private Map environmentVariables = new HashMap();

    /**
     * Command line working directory.
     *
     * @parameter
     */
    private File workingDirectory;

    /**
     * Option to specify the jvm (or path to the java executable) to use with
     * the forking options. For the default we will assume that java is in the path.
     *
     * @parameter expression="${childDelegation}"
     * default-value="true"
     */
    private boolean childDelegation;

    /**
     * Groups for this test. Only classes/methods/etc decorated with one of the
     * groups specified here will be included in test run, if specified.
     *
     * @parameter expression="${groups}"
     */
    private String groups;

    /**
     * Excluded groups. Any methods/classes/etc with one of the groups specified in this
     * list will specifically not be run.
     *
     * @parameter expression="${excludedGroups}"
     */
    private String excludedGroups;

    /**
     * List of TestNG suite xml file locations, seperated by commas. It should be noted that
     * if suiteXmlFiles is specified, <b>no</b> other tests will be run, effectively making
     * any other parameters, like include/exclude useless.
     *
     * @parameter
     */
    private List suiteXmlFiles;

    /**
     * The attribute thread-count allows you to specify how many threads should be allocated
     * for this execution. Makes most sense to use in conjunction with parallel.
     *
     * @parameter expression="${threadCount}"
     * default-value="0"
     */
    private int threadCount;

    /**
     * When you use the parallel attribute, TestNG will try to run all your test methods in
     * separate threads, except for methods that depend on each other, which will be run in
     * the same thread in order to respect their order of execution.
     *
     * @parameter expression="${parallel}"
     * default-value="false"
     */
    private boolean parallel;

    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Tests are skipped." );

            return;
        }

        if ( !testClassesDirectory.exists() )
        {
            getLog().info( "No tests to run." );

            return;
        }

        // ----------------------------------------------------------------------
        // Setup the surefire booter
        // ----------------------------------------------------------------------

        SurefireBooter surefireBooter = new SurefireBooter();

        surefireBooter.setGroups( groups );

        surefireBooter.setExcludedGroups( excludedGroups );

        surefireBooter.setThreadCount( threadCount );

        surefireBooter.setParallel( parallel );

        surefireBooter.setTestSourceDirectory( testSourceDirectory.getPath() );

        // ----------------------------------------------------------------------
        // Reporting
        // ----------------------------------------------------------------------

        getLog().info( "Setting reports dir: " + reportsDirectory );

        surefireBooter.setReportsDirectory( reportsDirectory );

        if ( suiteXmlFiles != null && suiteXmlFiles.size() > 0 )
        {
            for ( int i = 0; i < suiteXmlFiles.size(); i++ )
            {
                String filePath = (String) suiteXmlFiles.get( i );
                File file = new File( filePath );
                if ( file.exists() )
                {
                    surefireBooter.addBattery( "org.apache.maven.surefire.battery.TestNGXMLBattery",
                                               new Object[]{file} );
                }
            }
        }

        // ----------------------------------------------------------------------
        // Check to see if we are running a single test. The raw parameter will
        // come through if it has not been set.
        // ----------------------------------------------------------------------

        if ( test != null )
        {
            // FooTest -> **/FooTest.java

            List includes = new ArrayList();

            List excludes = new ArrayList();

            String[] testRegexes = split( test, ",", -1 );

            for ( int i = 0; i < testRegexes.length; i++ )
            {
                includes.add( "**/" + testRegexes[i] + ".java" );
            }

            surefireBooter.addBattery( "org.apache.maven.surefire.battery.DirectoryBattery",
                                       new Object[]{testClassesDirectory, includes, excludes} );
        }
        //Only if testng suites aren't being run
        else if ( suiteXmlFiles == null || suiteXmlFiles.size() < 1 )
        {
            // defaults here, qdox doesn't like the end javadoc value
            // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
            if ( includes == null || includes.size() == 0 )
            {
                includes = new ArrayList(
                    Arrays.asList( new String[]{"**/Test*.java", "**/*Test.java", "**/*TestCase.java"} ) );
            }
            if ( excludes == null || excludes.size() == 0 )
            {
                excludes = new ArrayList(
                    Arrays.asList( new String[]{"**/Abstract*Test.java", "**/Abstract*TestCase.java", "**/*$*"} ) );
            }

            surefireBooter.addBattery( "org.apache.maven.surefire.battery.DirectoryBattery",
                                       new Object[]{testClassesDirectory, includes, excludes} );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        getLog().debug( "Test Classpath :" );

        getLog().debug( testClassesDirectory.getPath() );

        surefireBooter.addClassPathUrl( testClassesDirectory.getPath() );

        getLog().debug( classesDirectory.getPath() );

        surefireBooter.addClassPathUrl( classesDirectory.getPath() );

        for ( Iterator i = classpathElements.iterator(); i.hasNext(); )
        {
            String classpathElement = (String) i.next();

            getLog().debug( classpathElement );

            surefireBooter.addClassPathUrl( classpathElement );
        }

        for ( Iterator i = pluginArtifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            // TODO: this is crude for now. We really want to get "surefire-booter" and all its dependencies, but the
            // artifacts don't keep track of their children. We could just throw all of them in, but that would add an
            // unnecessary maven-artifact dependency which is precisely the reason we are isolating the classloader
            if ( "junit".equals( artifact.getArtifactId() ) || "org.testng".equals( artifact.getGroupId() ) ||
                "org.apache.maven.surefire".equals( artifact.getGroupId() ) ||
                "plexus-utils".equals( artifact.getArtifactId() ) )
            {
                getLog().debug( "Adding to surefire test classpath: " + artifact.getFile().getAbsolutePath() );

                surefireBooter.addClassPathUrl( artifact.getFile().getAbsolutePath() );
            }
        }

        addReporters( surefireBooter );

        processSystemProperties();

        // ----------------------------------------------------------------------
        // Forking
        // ----------------------------------------------------------------------

        boolean success;
        try
        {
            surefireBooter.setForkMode( forkMode );

            if ( !forkMode.equals( "none" ) )
            {
                surefireBooter.setSystemProperties( System.getProperties() );

                surefireBooter.setJvm( jvm );

                surefireBooter.setBasedir( basedir.getAbsolutePath() );

                surefireBooter.setArgLine( argLine );

                surefireBooter.setEnvironmentVariables( environmentVariables );

                surefireBooter.setWorkingDirectory( workingDirectory );

                surefireBooter.setChildDelegation( childDelegation );

                if ( getLog().isDebugEnabled() )
                {
                    surefireBooter.setDebug( true );
                }
            }

            success = surefireBooter.run();
        }
        catch ( Exception e )
        {
            // TODO: better handling
            throw new MojoExecutionException( "Error executing surefire", e );
        }

        if ( !success )
        {
            String msg = "There are test failures.";

            if ( testFailureIgnore )
            {
                getLog().error( msg );
            }
            else
            {
                throw new MojoExecutionException( msg );
            }
        }
    }

    protected void processSystemProperties()
    {
        System.setProperty( "basedir", basedir.getAbsolutePath() );

        System.setProperty( "localRepository", localRepository.getBasedir() );

        // Add all system properties configured by the user
        if ( systemProperties != null )
        {
            Iterator iter = systemProperties.keySet().iterator();

            while ( iter.hasNext() )
            {
                String key = (String) iter.next();

                String value = (String) systemProperties.get( key );

                getLog().debug( "Setting system property [" + key + "]=[" + value + "]" );

                System.setProperty( key, value );
            }
        }
    }

    protected String[] split( String str, String separator, int max )
    {
        StringTokenizer tok;

        if ( separator == null )
        {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer( str );
        }
        else
        {
            tok = new StringTokenizer( str, separator );
        }

        int listSize = tok.countTokens();

        if ( max > 0 && listSize > max )
        {
            listSize = max;
        }

        String[] list = new String[listSize];

        int i = 0;

        int lastTokenBegin;

        int lastTokenEnd = 0;

        while ( tok.hasMoreTokens() )
        {
            if ( max > 0 && i == listSize - 1 )
            {
                // In the situation where we hit the max yet have
                // tokens left over in our input, the last list
                // element gets all remaining text.
                String endToken = tok.nextToken();

                lastTokenBegin = str.indexOf( endToken, lastTokenEnd );

                list[i] = str.substring( lastTokenBegin );

                break;
            }
            else
            {
                list[i] = tok.nextToken();

                lastTokenBegin = str.indexOf( list[i], lastTokenEnd );

                lastTokenEnd = lastTokenBegin + list[i].length();
            }
            i++;
        }

        return list;
    }

    /**
     * <p> Adds Reporters that will generate reports with different formatting.
     * <p> The Reporter that will be added will be based on the value of the parameter
     * useFile, reportFormat, and printSummary.
     *
     * @param surefireBooter The surefire booter that will run tests.
     */
    private void addReporters( SurefireBooter surefireBooter )
    {
        if ( useFile )
        {
            if ( printSummary )
            {
                if ( forking() )
                {
                    surefireBooter.addReport( "org.apache.maven.surefire.report.ForkingConsoleReporter" );
                }
                else
                {
                    surefireBooter.addReport( "org.apache.maven.surefire.report.ConsoleReporter" );
                }
            }

            if ( reportFormat.equals( "brief" ) )
            {
                surefireBooter.addReport( "org.apache.maven.surefire.report.BriefFileReporter" );
            }
            else if ( reportFormat.equals( "plain" ) )
            {
                surefireBooter.addReport( "org.apache.maven.surefire.report.FileReporter" );
            }
        }
        else
        {
            if ( reportFormat.equals( "brief" ) )
            {
                surefireBooter.addReport( "org.apache.maven.surefire.report.BriefConsoleReporter" );
            }
            else if ( reportFormat.equals( "plain" ) )
            {
                surefireBooter.addReport( "org.apache.maven.surefire.report.DetailedConsoleReporter" );
            }
        }

        surefireBooter.addReport( "org.apache.maven.surefire.report.XMLReporter" );
    }

    private boolean forking()
    {
        return !forkMode.equals( "none" );
    }
}
