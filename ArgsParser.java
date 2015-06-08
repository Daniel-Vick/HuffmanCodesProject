////////////////////////////////////////////////////////////////////////////////
// ArgsParser.java
////////////////////////////////////////////////////////////////////////////////

package org.instructures;

import java.io.*;
import java.util.*;

// A general command-line argument parser following the Unix
// single-character option conventions (similar to getopt,
// http://en.wikipedia.org/wiki/Getopt) and also the GNU long-form
// option conventions (cf. getopt_long, ibid.).
//
// The API uses the fluent-interface style, as discussed in:
// http://www.martinfowler.com/bliki/FluentInterface.html.
public class ArgsParser
{
  // Canned messages and formatting strings.
  private static final String
    DEFAULT_VERSION = "(unknown)",
    HELP_MESSAGE = "display this help and exit",
    VERSION_MESSAGE = "output version information and exit",
    GENERIC_OPTIONS = "OPTIONS",
    OPTION_SUMMARY_FMT = "%4s%s %-20s   %s%n";
  
  // Factory to make a new ArgsParser instance, to generate help
  // messages and to process and validate the arguments for a command
  // with the given `commandName`.
  public static ArgsParser create(String commandName) {
    return new ArgsParser(commandName);
  }

  // A queryable container to hold the parsed results.
  //
  // Options are added using on of the `optional`, `require`, and
  // `requireOneOf` methods. The presence of such Options in the
  // actual arguments processed can be queried via the `hasOption`
  // method.
  //
  // Operands can be associated with an Option or can stand
  // alone. Standalone Operands are added using the `requiredOperand`,
  // `optionalOperand`, `oneOrMoreOperands`, and `zeroOrMoreOperands`
  // methods. Operands associated with an Option are added when that
  // Option is added.
  public class Bindings {
    public boolean hasOption(Option optionToQuery) {
      return options.contains(optionToQuery);
    }

    // If an Operand is optional and has a default value, then this method
    // will return the default value when the Operand wasn't specified.
    public <T> T getOperand(Operand<T> operand) {
      if (operands.containsKey(operand)) {
        List<T> result = getOperands(operand);
        if (result.size() == 1) {
          return result.get(0);
        }
      }
      else if (operand.hasDefaultValue()) {
        return operand.getDefaultValue();
      }
      throw new RuntimeException(
        String.format("Expected one binding for operand %s", operand));
    }
    
    public <T> List<T> getOperands(Operand<T> operand) {
      List<T> result = new ArrayList<>();
      if (operands.containsKey(operand)) {
        List<String> uninterpretedStrings = operands.get(operand);
        for (String stringFormat: uninterpretedStrings) {
          result.add(operand.convertArgument(stringFormat));
        }
      }
      return result;
    }

    private void addOption(Option option) {
      options.add(option);
    }
    
    private void bindOperand(Operand<?> operand, String lexeme) {
      List<String> bindings;
      if (operands.containsKey(operand)) {
        bindings = operands.get(operand);
      }
      else {
        bindings = new ArrayList<>();
        operands.put(operand, bindings);
      }
      try {
        operand.convertArgument(lexeme);
      }
      catch (Exception e) {
        throw new RuntimeException(
          String.format("(invalid format) %s", e.getMessage()));
      }
      bindings.add(lexeme);
    }

    private final Set<Option> options = new HashSet<>();
    private final Map<Operand, List<String>> operands = new HashMap<>();
    
    private Bindings() {
      /* intentionally left blank */
    }
  }

  // Parses the given command-line argument values according to the
  // specifications set through calls to the `optional`, `require`,
  // `requireOneOf` and `operands` methods.
  //
  // When the given arguments don't match the options specified, an
  // error message is printed and the program exits.
  //
  // Options for displaying the help message and the version message
  // are supported by calls made to `help` and `version`,
  // respectively. A call to 'parse` will cause the program to exit if
  // the help or version options are present in the given `args`. If
  // both are specified, then both will be printed before exit.
  public ArgsParser.Bindings parse(String[] args) {
    //ArrayList<Map.Entry<Operand, String>> operandsSet = operandMap.entrySet();
    //ArrayList<Map.Entry<Option, String>> optionSet = optionMap.entrySet();
    ArrayList<Operand> operandList = new ArrayList<Operand>();
    ArrayList<Option> parsedOptions = new ArrayList<Option>();
    for (Map.Entry<Operand, String> entry : operandMap.entrySet()) {
      operandList.add(entry.getKey());
    }
    //System.out.println(operandList.size());
    int operandIndex = 0;
    Bindings bindings = new Bindings();
    for (int i = 0; i < args.length; i++) {
      //System.out.println("Bouta go");
      if ((args[i].length() > 1) && (args[i].substring(0, 1).equals("-") || args[i].substring(0, 2).equals("--"))) {
	//System.out.println("Went");
	for (Map.Entry<Option, String> entry : optionMap.entrySet()) {
	  List<String> shortFlag = new ArrayList<String>();
	  List<String> longFlag = new ArrayList<String>();
    
	  entry.getKey().getFlags(longFlag, shortFlag);
    //System.out.println(shortFlag.get(0));
	  //System.out.println(longFlag.get(0));
	  
	  // Checks if flags match input
	  boolean shortFlagBool = false;
	  boolean longFlagBool = false;
	  if (shortFlag.size() > 0 && (args[i].length() > 1) && shortFlag.get(0).equals(args[i].substring(1))) {
	    shortFlagBool = true;
	  }
    if (args[i].contains("=")) {
      if (longFlag.get(0).equals(args[i].substring(2, args[i].indexOf("=")))) {
        longFlagBool = true;
      }
    }
	  if (shortFlag.size() > 0) {
	    if (args[i].length() > shortFlag.get(0).length() + 1) {
	      if (shortFlag.get(0).equals(args[i].substring(1,2))) {
		      shortFlagBool = true;
	      }
	    }
	  }
	  if (longFlag.size() > 0 && (args[i].length() > 2) && longFlag.get(0).equals(args[i].substring(2))) {
	    longFlagBool = true;
	  }

    if (longFlag.size() > 0 && (args[i].length() > 2 + longFlag.get(0).length()) && longFlag.get(0).equals(args[i].substring(2,2 + longFlag.get(0).length()))) {
      longFlagBool = true;
    }
    //System.err.println(shortFlagBool, longFlagBool);

	  // Adds options and any corresponding operands
	  if (shortFlagBool || longFlagBool) {
	    if (entry.getKey().hasOperand()) {
	      if (args[i].contains("=")) {

          String stringToAdd = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
          //int stringStart = longFlag.get(0).length() + 3;
          //int stringEnd = args[i].length();
          //stringToAdd = args[i].substring(stringStart, stringEnd);
 		      Operand operandToBind = entry.getKey().getOperand();
          bindings.bindOperand(operandToBind, stringToAdd);
		      //String opString = args[i].substring(longFlag.get(0).length() + 3);
		      //bindings.bindOperand(operandToBind, opString);
		      bindings.addOption(entry.getKey());
		      parsedOptions.add(entry.getKey());
		      i++;
	      }
	      else if (shortFlagBool && (args[i].length() > shortFlag.get(0).length() + 1)) {
		      Operand operandToBind = entry.getKey().getOperand();
		      String opString = args[i].substring(2);
		      bindings.bindOperand(operandToBind, opString);
		      bindings.addOption(entry.getKey());
		      parsedOptions.add(entry.getKey());
		      i++;
	      }
	      else if ((args.length > i + 1) && (!args[i + 1].substring(0).equals("-"))) {
		      //System.out.println("adding option with operand");
		      Operand operandToBind = entry.getKey().getOperand();
		      bindings.bindOperand(operandToBind, args[i + 1]);
		      bindings.addOption(entry.getKey());
		      parsedOptions.add(entry.getKey());
		      i++;
	      }
	      else {
		//throw new RuntimeException() {
		  System.err.println(usageStatement());
		  System.err.println("Error3");
		  System.exit(0);
		//}
	      }
	    } else {
        if (shortFlagBool && args[i].length() > 2) {
          System.err.println(usageStatement());
          System.err.println("Error22");
          System.exit(0);
        }
	      //System.out.println("Adding option with no operand");
	      bindings.addOption(entry.getKey());
	      parsedOptions.add(entry.getKey());
	    } 
	  }
    
    else {
        if (entry.getValue().equals("Optional") || entry.getValue().equals("Exclusive")) {
          break;
        }
	      System.err.println(usageStatement());
	      System.err.println("Error1");
        System.err.println(shortFlagBool + " " + longFlagBool + " " + longFlag.get(0) + " " + entry.getValue());
	      System.exit(0);
	  }
	} 
      }
      else {
	if (operandIndex > operandList.size() - 1) {
	  System.err.println(usageStatement());
	  System.err.println("ErrorHere");
	  System.exit(0);
	}
	else if (operandMap.get(operandList.get(operandIndex)).equals("Required")) {
	  bindings.bindOperand(operandList.get(operandIndex), args[i]);
	  operandIndex++;
	}
	else if (operandMap.get(operandList.get(operandIndex)).equals("Optional")) {
	  bindings.bindOperand(operandList.get(operandIndex), args[i]);
	  operandIndex++;
	}
  else if (operandMap.get(operandList.get(operandIndex)).equals("ZeroOrMore")) {
    for (;i < args.length; i++) {
      bindings.bindOperand(operandList.get(operandIndex), args[i]);
    }
  }
	else {
	  for (int j = operandIndex; j < operandList.size(); j++) {
	    //System.out.println("Binding Operand");
	    bindings.bindOperand(operandList.get(j), args[i]);
	  }	
	}
      }
    }

    /*for (Map.Entry<Option, String> entry : optionMap.entrySet()) {
      if (!bindings.options.contains(entry) && entry.getValue().equals("Required")) {
	//throw new RuntimeException() {
	  System.out.println("Error2");
	//}
      } /*else if (entry.getKey().hasOperand() && !bindings.options.get(entry).getKey().hasOperand()) {
	//throw new RuntimeException() {
	  System.out.println("Error");
	//}
      }
    }*/
    
    for (RequiredMode currMode : requiredModes) {
      if (!currMode.followsRule(parsedOptions)) {
	System.err.println(usageStatement());
	System.err.println("Error4");
      }
    }

    for (Map.Entry<Operand, String> entry : operandMap.entrySet()) {
      if (!bindings.operands.containsKey((entry.getKey())) && entry.getValue().equals("Required")) {
	//throw new RuntimeException() {
	  System.err.println(usageStatement());
	  System.err.println("Errorhere");
	//}
      }
    }
    

        
    // TODO
    
    return bindings;
  }

  // Uses the given `summaryString` when the help/usage message is printed.
  public ArgsParser summary(String summaryString) {
    this.summaryString = summaryString;
    return this;
  }

  // Enables the command to have an option to display the current
  // version, represented by the given `versionString`. The version
  // option is invoked whenever any of the given `flags` are used,
  // where `flags` is a comma-separated list of valid short- and
  // long-form flags.
  public ArgsParser versionNameAndFlags(String versionString, String flags) {
    this.versionString = versionString;
    this.versionOption = Option.create(flags).summary(VERSION_MESSAGE);
    return optional(versionOption);
  }

  // Enables an automated help message, generated from the options
  // specified.  The help message will be invoked whenever any of the
  // given `flags` are used.
  //
  // The `flags` parameter is a comma-separated list of valid short-
  // and long-form flags, including the leading `-` and `--` marks.
  public ArgsParser helpFlags(String flags) {
    this.helpOption = Option.create(flags).summary(HELP_MESSAGE);
    return optional(helpOption);
  }

  // Adds the given option to the parsing sequence as an optional
  // option. If the option takes an Operand, the value of the
  // associated operand can be accessed using a reference to that
  // specific Operand instance.
  //
  // Throws an IllegalArgumentException if the given option specifies
  // flags that have already been added.
  public ArgsParser optional(Option optionalOption) {
    // TODO
    if (optionMap.containsKey(optionalOption)) {
      throw  new IllegalArgumentException();
    } else {
      optionMap.put(optionalOption, "Optional");
    }
    return this;
  }

  // Adds the given option to the parsing sequence as a required
  // option. If the option is not present during argument parsing, an
  // error message is generated using the given `errString`. If the
  // option takes an Operand, the value of the associated operand can
  // be accessed using a reference to that specific Operand instance.
  //
  // Throws an IllegalArgumentException if the given option specifies
  // flags that have already been added.
  public ArgsParser require(String errString, Option requiredOption) {
    // TODO
    if (optionMap.containsKey(requiredOption)) {
      throw new IllegalArgumentException(errString);
    } else {
      for (RequiredMode mode : requiredModes) {
	if (mode.modes.contains(requiredOption)) {
	  throw new IllegalArgumentException(errString);
	}
      }
    }
    Option[] temp = {requiredOption};
    requiredModes.add(new RequiredMode(temp));
    optionMap.put(requiredOption, "Required");
    return this;
  }

  // Adds the given set of mutually-exclusive options to the parsing
  // sequence. An error message is generated using the given
  // `errString` when multiple options that are mutually exclusive
  // appear, and when none appear. An example of such a group of
  // mutually- exclusive options is when the option specifies a
  // particular mode for the command where none of the modes are
  // considered as a default.
  //
  // Throws an IllegalArgumentException if any of the given options
  // specify flags that have already been added.
  public ArgsParser requireOneOf(String errString, Option... exclusiveOptions) {
    // TODO
    for (int i = 0; i < exclusiveOptions.length; i++) {
      if (optionMap.containsKey(exclusiveOptions[i])) {
	throw new IllegalArgumentException(errString);
      } else {
	for (RequiredMode mode : requiredModes) {
	  if (mode.modes.contains(exclusiveOptions[i])) {
	    throw new IllegalArgumentException(errString);
	  }
	}
      }
    }
    for (int i = 0; i < exclusiveOptions.length; i++) {
      optionMap.put(exclusiveOptions[i], "Exclusive");
    }
    requiredModes.add(new RequiredMode(exclusiveOptions));
    return this;
  }

  // Adds the given operand to the parsing sequence as a required
  // operand to appear exactly once. The matched argument's value is
  // retrievable from the `ArgsParser.Bindings` store by passing the
  // same `requiredOperand` instance to the `getOperand` method.
  public ArgsParser requiredOperand(Operand requiredOperand) {
    // TODO
    operandMap.put(requiredOperand, "Required");
    return this;
  }

  // Adds the given operand to the parsing sequence as an optional
  // operand. The matched argument's value is retrievable from the
  // `ArgsParser.Bindings` store by passing the same `optionalOperand`
  // instance to the `getOperands` method, which will return either a
  // the empty list or a list with a single element.
  public ArgsParser optionalOperand(Operand optionalOperand) {
    // TODO
    operandMap.put(optionalOperand, "Optional");
    return this;
  }

  // Adds the given operand to the parsing sequence as a required
  // operand that must be specifed at least once and can be used
  // multiple times (the canonical example would be a list of one or
  // more input files).
  //
  // The values of the arguments matched is retrievable from the
  // `ArgsParser.Bindings` store by passing the same `operand`
  // instance to the `getOperands` method, which will return a list
  // with at least one element (should the arguments pass the
  // validation process).
  public ArgsParser oneOrMoreOperands(Operand operand) {
    // TODO
    operandMap.put(operand, "OneOrMore");
    return this;
  }

  // Adds the given operand to the parsing sequence as an optional
  // operand that can be used zero or more times (the canonical
  // example would be a list of input files, where if none are given
  // then stardard input is assumed).
  //
  // The values of the arguments matched is retrievable from the
  // `ArgsParser.Bindings` store by passing the same `operand`
  // instance to the `getOperands` method, which will return a list of
  // all matches, potentially the empty list.
  public ArgsParser zeroOrMoreOperands(Operand operand) {
    // TODO
    operandMap.put(operand, "ZeroOrMore");
    return this;
  }

  private final String commandName;

  private String summaryString = null;
  private String versionString = DEFAULT_VERSION;
  private Option helpOption = null;
  private Option versionOption = null;
  
  private ArgsParser(String commandName) {
    this.commandName = commandName;
  }

  // TODO: Add more code here if you think it'll be helpful!
  private Map<Option, String> optionMap = new HashMap<Option, String>();
  private Map<Operand, String> operandMap = new LinkedHashMap<Operand, String>();
  private ArrayList<RequiredMode> requiredModes = new ArrayList<RequiredMode>();
  //private Set<Option> requiredOptions = new LinkedHashSet<Option>();
  //private Set<Option> optionalOptions = new LinkedHashSet<Option>();
  //private Set<Option> exclusiveOptions = new LinkedHashSet<Option>();
  //private Set<Operand> requiredOperands = new LinkedHashSet<Operand>();
  //private Set<Operand> optionalOperands = new LinkedHashSet<Operand>();
  
  class RequiredMode {
   private Set<Option> modes = new HashSet<Option>();

   RequiredMode(Option[] options) {
     for (Option option: options) {
       modes.add(option);
     }
   }

   // To be called near the end of `parse`.
   boolean followsRule(List<Option> actualParsedOptions) {
     // The rule is that there must be one and only one member of
     // the `modes` set in the given set of `actualParsedOptions`
     boolean result = true; // Assume rule is followed until prove otherwise
     Option mode = null;
     for (Option actual: actualParsedOptions) {
       // Is `actual` in our set in the first place?
       if (modes.contains(actual)) {
         if (mode == null) {
           mode = actual;
         } else if (mode != actual) {
           // Error message here: "Must specify only one of these..."
           // So, both `mode` and `actual` are required modes in our set,
           // and because they differ, we know what the conflict is.
           result = false;
         }
       }
     }
     if (mode == null) {
       // Error message here: "Must specify one of ...our set"
       result = false; 
     }
     return result;
    }
  }

  public String usageStatement() {
    int reqOpt = 0;
    int optOpt = 0;
    int reqOpr = 0;
    int optOpr = 0;
    for (Map.Entry<Option, String> entry : optionMap.entrySet()) {
      if (entry.getValue().equals("Required")) {
        reqOpt++;
      }
      else if (entry.getValue().equals("Optional")) {
        optOpt++;
      }
      else if (entry.getValue().equals("Exclusive")) {
        reqOpt++;
      }
    }
    for (Map.Entry<Operand, String> entry : operandMap.entrySet()) {
      if (entry.getValue().equals("Required")) {
        reqOpr++;
      }
      else if (entry.getValue().equals("Optional")) {
        optOpr++;
      }
      else if (entry.getValue().equals("OneOrMore")) {
        reqOpr += 2;
      }
      else if (entry.getValue().equals("ZeroOrMore")) {
        optOpr += 2;
      }
    }
    String stringToReturn = "Usage: " + commandName + " ";
    if (reqOpt > 1) {
      stringToReturn += "Options...";
    }
    else if (reqOpt > 0 && optOpt > 0) {
      stringToReturn += "Option...";
    }
    else if (reqOpt > 0) {
      stringToReturn += "Option";
    }
    else if (optOpt > 1) {
      stringToReturn += "[Options...]";
    }
    else if (optOpt > 0) {
      stringToReturn += "[Option]";
    }

    if (reqOpr > 1) {
      stringToReturn += "Operands...";
    }
    else if (reqOpr > 0 && optOpt > 0) {
      stringToReturn += "Operands...";
    }
    else if (reqOpr > 0) {
      stringToReturn += "Operand";
    }
    else if (optOpr > 1) {
      stringToReturn += "[Operands...]";
    }
    else if (optOpr > 0) {
      stringToReturn += "[Operand]";
    }
    return stringToReturn;
  }
}
