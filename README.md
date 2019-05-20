libaddressinput
===============

This is a fork of a fork of google's [libaddressinput](https://github.com/googlei18n/libaddressinput) without Android dependencies and with the address formatter exposed. The original
project used Scala whereas this is a pure Java project that can be compiled using Maven.

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
Hildesheimer Str
3000 Hannover
```

The original project can be found here: https://github.com/blackmad/libaddressinput
