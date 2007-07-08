@echo off
start "jEdit startup" "{javaw.exe}" -Xms64M -Xmx192M -jar "{jedit.jar}" -reuseview %*
