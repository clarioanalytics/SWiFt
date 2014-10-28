# SWiFt for Amazon Simple Workflow 

SWiFt is a mini-framework for using Amazon SWF based off of the Amazon SDK for Java.

## Goals and Philosophy

[Amazon Simple Workflow](http://aws.amazon.com/swf/) (SWF) is great, but it's really only "simple" in the sense that you don't have to set up and manage your own workflow engine.
Being flexible, the SWF API is actually pretty dense and complicated to use, which is why Amazon also provides its
"AWS Flow Framework for Java" (AWS Flow).

Our team balked however at using AWS Flow as it requires a code-generation pass before compilation: In a word "yuck".

Netflix also found AWS Flow distasteful enough that it created it's own [Glisten framework](https://github.com/Netflix/glisten) in Groovy.
Glisten mirrors AWS FLow in style but, since Groovy is dynamic, there is no need for AWS Flow's code-generation.  

Even so, our team found that Glisten wasn't exactly what we wanted either.  
You still have to define an interface and class for every activity and an interface, class and description class for every workflow, which has a very strong J2EE "framework" smell.

Also, since these frameworks provide so much hand-holding there are several SWF scenarios that didn't seem implemented when we reviewed.
Stuff like, starting and monitoring child workflows, signals, continuous workflows, etc.

So instead, our team decided to create a thin-layer over Amazon's SWF API, which we named "SWiFt", with the goal of swift SWF development. 
Our major goals were: 

- As much as possible use Amazon's SDK classes, don't reinvent the wheel or paint over what works 
- Reduce as much 'magic' as possible, we believe that being able to follow your code in a debugger should be easy
- Make it usable by as large an audience as possible:
    - Make it backward compatible to Java 1.6, which matches the minimum version that can be used with Amazon API for Java
    - Don't add a lot of dependencies and force 'jarplosion' on users 
    - Make writing/running workflows easy enough so that developers will want to do so
    - Document it well and provide lots of examples

This is our company's first open-source release and we hope it finds an audience that will enjoy using it.  

Give SWiFt a fork and let us know what you think!
    
### Note on Timeouts

The SWF Java API allows for registering activities and workflows with no timeout value specified.
Amazon SWF really should reject activity and workflow registrations with missing timeout values since allowing it
leads to annoying errors when trying to start workflows or activities.

As much as possible SWiFt tries to enforce sensible defaults for timeout values.  What this means is that "NONE" is set
by default on most timeout values.  365 days is set on timeout values that do not allow "NONE", specifically 
workflow execution start to close timeouts on workflows and start child workflow actions. 

In practice we've found that most of the time you don't want tight timeouts. Instead we've found it preferable to fine-tune a few places where we want action timeouts.  This makes many of our workflows more like using (SQS), but with all the extra goodness that SWF provides.

## Requirements

- Maven 2 or 3 installation
- Java 1.6+ 
- Active Amazon Web Services account and credentials
- Access to Amazon SWF with a domain set up
- Understanding of Amazon Simple Workflow concepts and the Amazon AWS SDK for Java

Visit [http://aws.amazon.com/swf](http://aws.amazon.com/swf) to learn.  It is our hope that SWiFt will be straight-forward
enough that it helps developers to better understand the available SWF features.

Amazon SWF's API has been relatively stable awhile now so if your project depends on an older version of the Amazon Java SDK SWiFt will most likely work fine.

## Dependencies

In trying to limit jarplosion SWiFt only uses dependencies required by the Amazon Java SDK itself to do SWF work.

See the pom.xml file for the current list.

The only additional project SWiFt uses is [SL4J](http://www.slf4j.org) which, like Apache Commons Logging, is a logging facade.
We picked SL4J since it is an api that has connectors for most popular logging frameworks.

## Building

To do a clean, build, and create javadocs:

    mvn clean install javadoc:javadoc

## Example Workflows

Example recipie workflows are provided in the `com.clario.swift.examples.workflows` package.  A sample configuration with separate
activity and decision pollers can be found in the `com.clario.swift.examples`

While working with the examples (and your own workflows) you'll want to become familiar with [Amazons SWF Console](https://console.aws.amazon.com/swf/).
The console provides a nice UI for setting up your SWF environment and viewing running and completed workflow executions.

Some sample workflows require you to use the SWF Console, for example to send a signal to a running workflow.
See the javadoc for the example workflows for more explanation.

### Configuration
To run the examples in `com.clario.swift.examples` create the file `src/main/resources/config.properties` with the following properties:

    amazon.aws.id=                   # Amazon account id
    amazon.aws.key=                  # Amazon account secret key
    
    swf.domain=swift-examples        # Existing SWF domain under your account that you want to use
    swf.task.list=default            # Default SWF Task list identifier use for listening
    
    activity.pollers.pool.size=2     # ActivityPollerPool pool size
    decision.pollers.pool.size=2     # DecisionPollerPool pool size
    
    activity.pollers.register=true   # Should StartActivityPollers register the example activities on swf.domain and swf.task.list?
    decision.pollers.register=true   # Should StartDecisionPollers register the example workflows on swf.domain and swf.task.list?

_Note: The project's .gitignore file includes src/main/resources/config.properties so it won't be accidentally checked in_

### SWF Domains

Since SWF does not allow you to delete and recreate domains, your team should decide on a naming scheme for domains for each project and environment as well as having a domain for each developer.  An example might be:

      <developer>-<project>-playground   # One for each developer/project: gcoller-project1-playground
      <envionment>-<project>             # dev-myproject1, qa-myproject1, prod-myproject1

### Running Examples

Most developers will probably want to import the project into their IDE and run the example workflows there so they can debug it.

The basic steps are:

- Run StartActivityPollers main method to start polling for activities.  Start it in debug mode to debug an activity method.
- Run StartDecisionPollers main method to start polling for decisions.  Start it in debug mode to debug workflows.
- Choose an example workflow and run it's main method.  I suggest `SimpleWorkflow` to start with.

Maven also provides a way to start up any class with a Main method from the command line.
_Note: make sure the project is built first_

#### Running the Activity poller pool:
   mvn exec:java -Dexec.mainClass="com.clario.swift.examples.ActivityPollerPool"
   
#### Running the Decision poller pool:
   mvn exec:java -Dexec.mainClass="com.clario.swift.examples.DecisionPollerPool"
   
#### Running an example workflow:
   mvn exec:java -Dexec.mainClass="com.clario.swift.examples.workflows.SimpleWorkflow"
   
## License

(The MIT License)

Copyright (c) 2009-2014 Sorin Ionescu and contributors.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.