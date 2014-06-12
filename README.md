# _SWiFt_ for Amazon Simple Workflow 

_SWiFt_ is a mini-framework for using Amazon SWF based off of the Amazon SDK for Java.

## Goals and Philosophy

[Amazon Simple Workflow](http://aws.amazon.com/swf/) (SWF) is great, but it's really only "simple" in the sense that you don't have to set up and manage your own workflow engine.
Being flexible, the SWF API is actually pretty dense and complicated to use, which is why Amazon also provides its
"AWS Flow Framework for Java" (AWS Flow).

Our team balked however at using AWS Flow as it requires a code-generation pass before compilation: Basically "Yuck".

Netflix also found AWS Flow distasteful enough that it created it's own [Glisten framework](https://github.com/Netflix/glisten) in Groovy.
Glisten mirrors AWS FLow in style but, since Groovy is dynamic, there is no need for AWS Flow's code-generation.  

Even so, our team found that Glisten wasn't exactly what we wanted in a framework either.  
You still have to define an interface and class for every activity and an interface, class and description class for every workflow, 
workflow which had a very strong 1990's J2EE "framework" smell.

Also, since these frameworks provide so much hand-holding there are several case scenarios that didn't seem implemented when we reviewed.
Stuff like, starting and monitoring child workflows, signals, never-ending workflows, etc.

So instead, our team decided to create a thin-layer over Amazon's SWF API, which we named "_SWiFt_", with the goal of swift workflow development. 

Our major goals are: 

- As much as possible use Amazon's SDK classes, don't reinvent what works
- Reduce as much 'magic' as possible, we believe that following your code in a debugger should be easy
- Make it usable by as large an audience as possible:
    - Make it backward compatible to Java 1.5
    - Don't add a lot of dependencies and force 'jarplosion' on users 
    - Make writing/running workflows easy enough so that developers will want to do so
    - Document it well and provide lots of examples

This is our company's first open-source release and we hope it finds an audience that will enjoy using it.  

Give _SWiFt_ a fork and let us know what you think!

## Requirements

- Maven 2 or 3 installation
- Java 1.5+ 
- Active Amazon Web Services account and credentials
- Access to Amazon SWF with a domain set up
- Decent understanding of Amazon Simple Workflow concepts and the Amazon AWS SDK for Java

Visit [http://aws.amazon.com/swf](http://aws.amazon.com/swf) to learn.  It is our hope that _SWiFt_ will be straight-forward
enough that it actually helps developers understand SWF features better.

## Building

To do a clean, build, and create javadocs:

    mvn clean install javadoc:javadoc

## Example Workflows

Example workflows are provided in the `com.clario.swift.examples.workflows` package.  A sample configuration with separate
activity and decision pollers can be found in the `com.clario.swift.examples`

While working with the examples (and your own workflows) you'll want to become familiar with [Amazons SWF Console](https://console.aws.amazon.com/swf/).
The console provides a nice UI for setting up your SWF environment and viewing running and completed workflow executions. 

Some sample workflows require you to use the SWF Console, for example to send a signal to a running workflow.

### Configuration
To run the examples in `com.clario.swift.examples` create the file `src/main/resources/config.properties` with the following properties:

    amazon.aws.id=                   # Amazon account id
    amazon.aws.key=                  # Amazon account secret key
    
    swf.domain=swift-examples        # Existing SWF domain under your account
    swf.task.list=default            # SWF Task list identifier use
    
    activity.pollers.pool.size=2     # ActivityPollerPool pool size
    decision.pollers.pool.size=2     # DecisionPollerPool pool size
    
    activity.pollers.register=false  # Should StartActivityPollers register the example activities on swf.domain and swf.task.list?
    decision.pollers.register=false  # Should StartDecisionPollers register the example workflows on swf.domain and swf.task.list?

_Note: The project's .gitignore file includes src/main/resources/config.properties so it won't be accidentally checked in_

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
   
