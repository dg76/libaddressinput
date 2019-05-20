libaddressinput
===============

This is a fork of a fork of Google's [libaddressinput](https://github.com/googlei18n/libaddressinput) without Android dependencies and with the address formatter exposed. The original
project used Scala whereas this is a pure **Java** project that can be compiled using **Maven**.

To get the lines of an address just use this code:

```
List<String> result = new FormatInterpreter(new FormOptions.Builder().build()).getEnvelopeAddress(new AddressData.Builder()
        .setCountry("DE")
        .setPostalCode("3000")
        .setLocality("Hannover")
        .setRecipient("Max Mustermann")
        .setAddress("Hildesheimer Str. 1")
        .build());
```

It will return an array with one string for each line. The "setCountry" function specifies the target country
the address should be formatted for. E.g. the code above returns this address:

```
Max Mustermann
Hildesheimer Str. 1
3000 Hannover
```

And when using `setCountry("US")` it returns

```
Max Mustermann
Hildesheimer Str. 1
Hannover 3000
```

To use this project as dependency in your Maven project just run `mvn install` in the main directory 
of this project and then add

```
<dependency>
    <groupId>com.dgunia</groupId>
    <artifactId>libaddressinput</artifactId>
    <version>1.0</version>
</dependency>
```

to your own project's pom.xml file in the `dependencies` section. To also see the sources when debugging you can also
run `mvn source:jar install`.

The original project can be found here: https://github.com/blackmad/libaddressinput
