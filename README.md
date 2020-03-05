# Micropolis

Micropolis is the Open Source Release of the original SimCity source code. This is a Java version
originally written by Jason Long.

The original game, SimCity, was developed by Will Wright. Don Hopkins released a GPL open source
version named Micropolis as a part of the One Laptop Per Child (OLPC) program.

The forked version is Copyright (C) 2013 Jason Long (jason@long.name).
Portions Copyright (C) 1989-2007 Electronic Arts Inc.

# Prerequisites

To install and run this game, you need to install the Java Runtime Environment. You can get Java from
https://www.java.com/

For development, please install the Java Development Toolkit. I recommend OpenJDK. You'll find an
installation instruction here: https://openjdk.java.net/install/

# Running

The release archive comes with wrapper scripts, that start the game.

## Linux and Mac

x.x.x is the version number

(1) Extract the tar archive: `tar xf /path/to/micropolis-x.x.x.tar`
    
(2) Run `./micropolis-x.x.x/bin/micropolis`

## Windows

(1) Extract the ZIP archive by double-clicking on the file

(2) Double-click on `micropolis.bat` within the directory `bin`

# Development

## Building

A Gradle wrapper is included, so please just run

    ./gradlew build
    
to build the software and run the tests. 

## Localization

Unless you are an English speaker, you may like to run Micropolis in your own language. Micropolis
is designed to be run in any language, but needs translators to provide the translated text to
display.

If you want to translate the game to another language, please first determine the ISO 639-1 language
code of your target language, e.g. for Spanish use `es` (lower case). A list of language codes can
be found here: https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes

1. Fork the repository on GitHub and clone it locally.

2. Create a copy of each property file in `src/main/resources/strings` and rename the copy according
to your language code. This little bash code will help you:

```bash
    export language_code=es # change this according to your language
    find src/main/resources/strings -regex '.*/\([A-Za-z]*\)\.properties' -exec sh -c 'filename={}; cp $filename ${filename%.properties}_$language_code.properties' \;
```   
 
3. Translate the lines within the new resources bundles.

4. Create a pull request.

## File Format

Save files have the following format:

| Offset | Content                                             |
| ------ | --------------------------------------------------- |
| 0x0000 | History of residential levels (240 16-bit integers) |
| 0x01E0 | History of commericial levels                       |
| 0x03C0 | History of industrial levels                        |
| 0x05A0 | History of crime levels                             |
| 0x0780 | History of pollution levels                         |
| 0x0960 | History of cash flow                                |
| 0x0B40 | Miscellaneous values                                |
| 0x0C30 | Map data (by columns, west to east)                 |

