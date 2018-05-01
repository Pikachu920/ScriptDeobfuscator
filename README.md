# Script Deobfuscator

A simple deobfuscator for Skript obfuscators based around options.

Usage:

Put
```
on script load:
  deobfuscate the script
```
at the top of the script you wish to deobfuscate. 

You will find the deobfuscated script in the `plugins/Skript/scripts` folder
named `deobfuscated_originalname.sk` or, if the script is not associated with a file,
just `deobfuscated.sk`.