# Code Snippet

The class [`CodeSnippet.java`](../../src/main/java/org/sonarsource/plugins/secai/reporting/CodeSnippet.java) can be used to extract a code snippet from Java source code. You can give it a [`Location` object](../../src/main/java/org/sonarsource/plugins/secai/reporting/Location.java) to start from, in which case it only retrieves the source code. Alternatively you can give at least a starting line, the class name and a method name or Jimple statement or pass the `String` of an existing code snippet to extract all relevant information, namely the [bounds](#method-bounds) of the violating method call and its [parameters](#parameters). Note that this class uses basic `String` searching and while it is able to handle both line and block comments, it is not currently able to identify parenthesis or brackets within `String` literals.

## Method Bounds

It is assumed that the first occurrence of the violating method after the given starting line is the correct one. From this method call's opening parenthesis we attempt to locate the correct closing parenthesis.

Once the closing parenthesis is found, the method bounds are set from the start of the method name to the location of the closing parenthesis.

## Parameters

After the search for the [method bounds](#method-bounds) identified the start and end of the parameter list, we can now separate individual parameters. By searching for commas and paying close attention to opening and closing brackets (`(`, `)`, `{`, `}`) we can determine the location of all parameters.