@echo off
start "jEdit startup" "{javaw.exe}" {jvmOptions} -jar "{jedit.jar}" -reuseview %*
